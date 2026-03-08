package com.blackbotek.app1

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private var firebaseListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("BOT_CONFIG", Context.MODE_PRIVATE)

        // ID único centralizado (nunca genera duplicados)
        val idUnico = UserSession.getOrCreateId(prefs)

        // Configurar tabs
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "VELOCIDAD"
                1 -> "FILTROS"
                2 -> "RESUMEN"
                else -> ""
            }
        }.attach()

        validarEstado(idUnico)
    }

    private fun validarEstado(idUnico: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(idUnico)

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.child("estado").getValue(String::class.java) ?: "pendiente"
                val dias   = snapshot.child("dias_restantes").getValue(Long::class.java) ?: 0L
                val activo = estado == "activo" && dias > 0

                prefs.edit().putBoolean("bot_activo", activo).apply()

                // Si el usuario no existe aún en Firebase, crearlo
                if (!snapshot.exists()) {
                    ref.setValue(mapOf(
                        "id"             to idUnico,
                        "estado"         to "pendiente",
                        "dias_restantes" to 0,
                        "telefono"       to (prefs.getString("telefono", "") ?: ""),
                        "fecha_registro" to System.currentTimeMillis()
                    ))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error al conectar con Firebase", Toast.LENGTH_SHORT).show()
            }
        }

        ref.addValueEventListener(firebaseListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listener para evitar fugas de memoria
        val idUnico = UserSession.getId(prefs) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(idUnico)
        firebaseListener?.let { ref.removeEventListener(it) }
    }
}
