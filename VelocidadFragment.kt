package com.blackbotek.app1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class VelocidadFragment : Fragment() {

    private var velocidadSeleccionada = 350L
    private var gananciaExtra = 0f
    private var precioOroActivo = false
    private var precioOroValor = 0f
    private var calificacionMinima = 4.8f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_velocidad, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("BOT_CONFIG", Context.MODE_PRIVATE)

        val btnFlash          = view.findViewById<Button>(R.id.btnModoFlash)
        val btnCompetitivo    = view.findViewById<Button>(R.id.btnModoCompetitivo)
        val btnNormal         = view.findViewById<Button>(R.id.btnModoNormal)
        val btnHumano         = view.findViewById<Button>(R.id.btnModoHumano)
        val btnMenos          = view.findViewById<Button>(R.id.btnMenosGanancia)
        val btnMas            = view.findViewById<Button>(R.id.btnMasGanancia)
        val txtGanancia       = view.findViewById<TextView>(R.id.txtGanancia)
        val switchPrecioOro   = view.findViewById<SwitchMaterial>(R.id.switchPrecioOro)
        val etPrecioOro       = view.findViewById<EditText>(R.id.etPrecioOro)
        val sliderCalif       = view.findViewById<SeekBar>(R.id.sliderCalificacionPasajero)
        val txtCalif          = view.findViewById<TextView>(R.id.txtCalificacionPasajero)
        val btnGuardar        = view.findViewById<Button>(R.id.btnGuardarVelocidad)

        // Cargar valores guardados
        velocidadSeleccionada = prefs.getLong("velocidad_reaccion", 350L)
        gananciaExtra         = prefs.getFloat("ganancia_extra", 0f)
        precioOroActivo       = prefs.getBoolean("precio_oro_activo", false)
        precioOroValor        = prefs.getFloat("precio_oro_valor", 0f)
        calificacionMinima    = prefs.getFloat("calif_min", 4.8f)

        txtGanancia.text     = String.format("$%.2f", gananciaExtra)
        switchPrecioOro.isChecked = precioOroActivo
        etPrecioOro.setText(if (precioOroValor > 0) precioOroValor.toString() else "")
        etPrecioOro.isEnabled = precioOroActivo
        sliderCalif.progress = (calificacionMinima * 10).toInt()
        txtCalif.text        = String.format("%.1f", calificacionMinima)

        actualizarBotonesVelocidad(btnFlash, btnCompetitivo, btnNormal, btnHumano)

        btnFlash.setOnClickListener {
            velocidadSeleccionada = 50L
            actualizarBotonesVelocidad(btnFlash, btnCompetitivo, btnNormal, btnHumano)
        }
        btnCompetitivo.setOnClickListener {
            velocidadSeleccionada = 150L
            actualizarBotonesVelocidad(btnFlash, btnCompetitivo, btnNormal, btnHumano)
        }
        btnNormal.setOnClickListener {
            velocidadSeleccionada = 350L
            actualizarBotonesVelocidad(btnFlash, btnCompetitivo, btnNormal, btnHumano)
        }
        btnHumano.setOnClickListener {
            velocidadSeleccionada = 600L
            actualizarBotonesVelocidad(btnFlash, btnCompetitivo, btnNormal, btnHumano)
        }

        btnMenos.setOnClickListener {
            gananciaExtra = (gananciaExtra - 5f).coerceAtLeast(0f)
            txtGanancia.text = String.format("$%.2f", gananciaExtra)
        }
        btnMas.setOnClickListener {
            gananciaExtra += 5f
            txtGanancia.text = String.format("$%.2f", gananciaExtra)
        }

        switchPrecioOro.setOnCheckedChangeListener { _, isChecked ->
            precioOroActivo = isChecked
            etPrecioOro.isEnabled = isChecked
        }

        sliderCalif.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                calificacionMinima = progress / 10f
                txtCalif.text = String.format("%.1f", calificacionMinima)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnGuardar.setOnClickListener {
            precioOroValor = etPrecioOro.text.toString().toFloatOrNull() ?: 0f
            prefs.edit()
                .putLong("velocidad_reaccion", velocidadSeleccionada)
                .putFloat("ganancia_extra", gananciaExtra)
                .putBoolean("precio_oro_activo", precioOroActivo)
                .putFloat("precio_oro_valor", precioOroValor)
                .putFloat("calif_min", calificacionMinima)
                .apply()
            Toast.makeText(requireContext(), "✅ Configuración guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarBotonesVelocidad(flash: Button, comp: Button, normal: Button, humano: Button) {
        val alfa = 0.4f
        flash.alpha  = if (velocidadSeleccionada == 50L)  1f else alfa
        comp.alpha   = if (velocidadSeleccionada == 150L) 1f else alfa
        normal.alpha = if (velocidadSeleccionada == 350L) 1f else alfa
        humano.alpha = if (velocidadSeleccionada == 600L) 1f else alfa
    }
}
