package com.blackbotek.app1

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BotService : AccessibilityService() {

    private var botActivo = false
    private var diasRestantes = 0L
    private var idUnico: String? = null
    private var ultimaValidacion = 0L
    private var procesando = false

    companion object {
        const val CANAL_NOTIFICACION = "BOT_SERVICE_CHANNEL"
        const val NOTIF_ID = 1001
        const val INTERVALO_VALIDACION_MS = 60_000L // validar Firebase cada 60s
    }

    // ─── FOREGROUND SERVICE: notificación persistente para no ser matado ───
    override fun onServiceConnected() {
        super.onServiceConnected()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, construirNotificacion("🤖 BlackBotEk activo"))
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_NOTIFICACION,
                "Bot InDrive",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Servicio activo del bot" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(texto: String): Notification {
        return NotificationCompat.Builder(this, CANAL_NOTIFICACION)
            .setContentTitle("BlackBotEk")
            .setContentText(texto)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ─── EVENTO PRINCIPAL ───
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("BOT_CONFIG", Context.MODE_PRIVATE)
        val id = prefs.getString("user_id", null) ?: return

        // Solo procesar eventos relevantes de InDrive
        val paquete = event.packageName?.toString() ?: ""
        if (!paquete.contains("indriver", ignoreCase = true) &&
            !paquete.contains("inDriver", ignoreCase = false)) return

        val ahora = System.currentTimeMillis()

        // Validar Firebase solo cada 60 segundos
        if (ahora - ultimaValidacion > INTERVALO_VALIDACION_MS) {
            ultimaValidacion = ahora
            validarEstadoEnFirebase(id, prefs)
        }

        // Si el bot no está activo, no hacer nada
        if (!botActivo || diasRestantes <= 0) return

        // Evitar procesar si ya hay un toque en curso
        if (procesando) return

        val rootNode = rootInActiveWindow ?: return

        val precioMinimoKM  = prefs.getFloat("precio_km", 0f)
        val radioMaxRecogida = prefs.getFloat("radio_recogida", 999f)
        val calificacionMin  = prefs.getFloat("calif_min", 0f)
        val delay            = prefs.getLong("velocidad_reaccion", 350L)
        val gananciaExtra    = prefs.getFloat("ganancia_extra", 0f)
        val horaPicoActivo   = prefs.getBoolean("hora_pico_activo", false)
        val multiplicador    = prefs.getFloat("multiplicador_hora_pico", 20f)

        val precioFinal = if (horaPicoActivo)
            precioMinimoKM * (1f + multiplicador / 100f)
        else precioMinimoKM

        escanearNodos(rootNode, precioFinal + gananciaExtra, radioMaxRecogida, calificacionMin, delay)
    }

    // ─── VALIDAR EN FIREBASE ───
    private fun validarEstadoEnFirebase(id: String, prefs: android.content.SharedPreferences) {
        FirebaseDatabase.getInstance()
            .getReference("Usuarios").child(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.child("estado").getValue(String::class.java) ?: "pendiente"
                    diasRestantes = snapshot.child("dias_restantes").getValue(Long::class.java) ?: 0L
                    botActivo = estado == "activo" && diasRestantes > 0

                    val notifTexto = if (botActivo)
                        "🟢 Bot activo · $diasRestantes días restantes"
                    else
                        "🔴 Bot inactivo · Solicita activación"

                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIF_ID, construirNotificacion(notifTexto))
                }
                override fun onCancelled(error: DatabaseError) {
                    botActivo = false
                }
            })
    }

    // ─── ESCANEAR ÁRBOL DE NODOS ───
    private fun escanearNodos(
        node: AccessibilityNodeInfo,
        pKm: Float,
        rMax: Float,
        cMin: Float,
        delay: Long
    ) {
        // Buscar botón "Aceptar" primero por resource-id
        val candidatos = mutableListOf<AccessibilityNodeInfo>()
        buscarBotones(node, candidatos)

        for (boton in candidatos) {
            val textoBoton = boton.text?.toString()?.lowercase() ?: ""
            if (textoBoton.contains("aceptar") || textoBoton.contains("accept")) {
                // Extraer info del viaje desde el contexto del árbol
                val infoViaje = extraerInfoViaje(node)
                if (esViajeRentable(infoViaje, pKm, rMax, cMin)) {
                    procesando = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        ejecutarToqueEnNodo(boton)
                        procesando = false
                    }, delay + (50..200).random())
                    return
                }
            }
        }
    }

    private fun buscarBotones(node: AccessibilityNodeInfo, resultado: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) resultado.add(node)
        for (i in 0 until node.childCount) {
            val hijo = node.getChild(i) ?: continue
            buscarBotones(hijo, resultado)
        }
    }

    // ─── EXTRAER INFO DEL VIAJE ───
    data class InfoViaje(
        val precio: Float = 0f,
        val distanciaRecogida: Float = 999f,
        val calificacion: Float = 5f,
        val encontrado: Boolean = false
    )

    private fun extraerInfoViaje(root: AccessibilityNodeInfo): InfoViaje {
        val textos = mutableListOf<String>()
        recolectarTextos(root, textos)
        val todoElTexto = textos.joinToString(" ")

        // Precio: busca patrones como "$12.50", "12,50", "$ 8"
        val precio = Regex("""[$＄]?\s*(\d{1,4}[.,]\d{0,2})""")
            .findAll(todoElTexto)
            .mapNotNull { it.groupValues[1].replace(",", ".").toFloatOrNull() }
            .filter { it in 1f..9999f }
            .firstOrNull() ?: 0f

        // Distancia de recogida: "1.2 km", "800 m", "0.5km"
        val distKm = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)
            .find(todoElTexto)?.groupValues?.get(1)?.replace(",", ".")?.toFloatOrNull()

        val distM = Regex("""(\d+)\s*m\b""")
            .find(todoElTexto)?.groupValues?.get(1)?.toFloatOrNull()?.div(1000f)

        val distancia = distKm ?: distM ?: 999f

        // Calificación: "4.8", "4,9 ★", "⭐4.7"
        val calif = Regex("""[⭐★]?\s*([4-5][.,]\d)\s*[⭐★]?""")
            .find(todoElTexto)?.groupValues?.get(1)?.replace(",", ".")?.toFloatOrNull() ?: 5f

        return InfoViaje(
            precio = precio,
            distanciaRecogida = distancia,
            calificacion = calif,
            encontrado = precio > 0f
        )
    }

    private fun recolectarTextos(node: AccessibilityNodeInfo, lista: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { lista.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { lista.add(it) }
        for (i in 0 until node.childCount) {
            val hijo = node.getChild(i) ?: continue
            recolectarTextos(hijo, lista)
        }
    }

    // ─── LÓGICA DE FILTRADO REAL ───
    private fun esViajeRentable(info: InfoViaje, pKm: Float, rMax: Float, cMin: Float): Boolean {
        if (!info.encontrado) return false
        if (info.distanciaRecogida > rMax) return false
        if (info.calificacion < cMin) return false
        // Si pKm > 0, comprobar precio mínimo (aproximación: precio / dist media asumida)
        if (pKm > 0 && info.precio < pKm) return false
        return true
    }

    // ─── EJECUTAR TOQUE EN POSICIÓN REAL DEL NODO ───
    private fun ejecutarToqueEnNodo(node: AccessibilityNodeInfo) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.isEmpty) return

        // Toque con pequeña variación aleatoria para parecer humano
        val x = bounds.centerX().toFloat() + (-8..8).random()
        val y = bounds.centerY().toFloat() + (-5..5).random()

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 100L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        botActivo = false
    }
}
