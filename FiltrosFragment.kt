package com.blackbotek.app1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.switchmaterial.SwitchMaterial

class FiltrosFragment : Fragment() {

    private var mapBloquearZonas: MapView? = null
    private var mapRetornoACasa: MapView? = null
    private var googleMapBloquear: GoogleMap? = null
    private var googleMapRetorno: GoogleMap? = null

    private var radioLlegada = 2f
    private var multiplicadorHoraPico = 20f
    private var horaPicoActivo = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_filtros, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("BOT_CONFIG", Context.MODE_PRIVATE)

        mapBloquearZonas = view.findViewById(R.id.mapBloquearZonas)
        mapRetornoACasa  = view.findViewById(R.id.mapRetornoACasa)

        val btnDibujarZona    = view.findViewById<Button>(R.id.btnDibujarZona)
        val btnLimpiarZonas   = view.findViewById<Button>(R.id.btnLimpiarZonas)
        val btnConfirmarPunto = view.findViewById<Button>(R.id.btnConfirmarPunto)
        val sliderRadio       = view.findViewById<SeekBar>(R.id.sliderRadioLlegada)
        val txtRadio          = view.findViewById<TextView>(R.id.txtRadioLlegada)
        val switchHoraPico    = view.findViewById<SwitchMaterial>(R.id.switchHoraPico)
        val sliderMulti       = view.findViewById<SeekBar>(R.id.sliderMultiplicador)
        val txtMulti          = view.findViewById<TextView>(R.id.txtMultiplicador)
        val btnGuardar        = view.findViewById<Button>(R.id.btnGuardarFiltros)

        // Cargar valores guardados
        radioLlegada          = prefs.getFloat("radio_llegada", 2f)
        multiplicadorHoraPico = prefs.getFloat("multiplicador_hora_pico", 20f)
        horaPicoActivo        = prefs.getBoolean("hora_pico_activo", false)

        sliderRadio.progress = radioLlegada.toInt()
        txtRadio.text = String.format("%.1f KM", radioLlegada)
        switchHoraPico.isChecked = horaPicoActivo
        sliderMulti.progress = multiplicadorHoraPico.toInt()
        txtMulti.text = String.format("%d%%", multiplicadorHoraPico.toInt())
        sliderMulti.isEnabled = horaPicoActivo

        // Inicializar MapViews con lambdas explícitas (fix del bug de orden)
        mapBloquearZonas?.onCreate(savedInstanceState)
        mapBloquearZonas?.getMapAsync { map ->
            googleMapBloquear = map
            map.uiSettings.isZoomControlsEnabled = true
            val latLng = getSavedLocation(prefs)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        }

        mapRetornoACasa?.onCreate(savedInstanceState)
        mapRetornoACasa?.getMapAsync { map ->
            googleMapRetorno = map
            map.uiSettings.isZoomControlsEnabled = true
            val latLng = getSavedLocation(prefs)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))

            // Mostrar marcador de casa guardado
            val lat = prefs.getFloat("casa_lat", 0f).toDouble()
            val lng = prefs.getFloat("casa_lng", 0f).toDouble()
            if (lat != 0.0 && lng != 0.0) {
                val casaLatLng = LatLng(lat, lng)
                map.addMarker(MarkerOptions().position(casaLatLng).title("🏠 Mi Casa"))
            }

            // Guardar punto al hacer long-click en el mapa
            map.setOnMapLongClickListener { punto ->
                map.clear()
                map.addMarker(MarkerOptions().position(punto).title("🏠 Mi Casa"))
                prefs.edit()
                    .putFloat("casa_lat", punto.latitude.toFloat())
                    .putFloat("casa_lng", punto.longitude.toFloat())
                    .apply()
                Toast.makeText(requireContext(), "📍 Punto de casa guardado", Toast.LENGTH_SHORT).show()
            }
        }

        btnDibujarZona.setOnClickListener {
            Toast.makeText(requireContext(), "Mantén presionado el mapa para marcar zona", Toast.LENGTH_SHORT).show()
        }

        btnLimpiarZonas.setOnClickListener {
            googleMapBloquear?.clear()
            Toast.makeText(requireContext(), "Zonas eliminadas", Toast.LENGTH_SHORT).show()
        }

        btnConfirmarPunto.setOnClickListener {
            val lat = prefs.getFloat("casa_lat", 0f)
            val lng = prefs.getFloat("casa_lng", 0f)
            if (lat == 0f && lng == 0f) {
                Toast.makeText(requireContext(), "Mantén presionado el mapa para marcar tu casa", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "✅ Punto de retorno: $lat, $lng", Toast.LENGTH_SHORT).show()
            }
        }

        sliderRadio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                radioLlegada = progress.toFloat()
                txtRadio.text = String.format("%.1f KM", radioLlegada)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        switchHoraPico.setOnCheckedChangeListener { _, isChecked ->
            horaPicoActivo = isChecked
            sliderMulti.isEnabled = isChecked
        }

        sliderMulti.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                multiplicadorHoraPico = progress.toFloat()
                txtMulti.text = String.format("%d%%", multiplicadorHoraPico.toInt())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnGuardar.setOnClickListener {
            prefs.edit()
                .putFloat("radio_llegada", radioLlegada)
                .putFloat("multiplicador_hora_pico", multiplicadorHoraPico)
                .putBoolean("hora_pico_activo", horaPicoActivo)
                .apply()
            Toast.makeText(requireContext(), "✅ Filtros guardados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSavedLocation(prefs: android.content.SharedPreferences): LatLng {
        val lat = prefs.getFloat("casa_lat", 0f).toDouble()
        val lng = prefs.getFloat("casa_lng", 0f).toDouble()
        return if (lat != 0.0 && lng != 0.0) LatLng(lat, lng)
        else LatLng(18.4861, -69.9312) // Santo Domingo por defecto
    }

    override fun onResume()    { super.onResume();    mapBloquearZonas?.onResume();  mapRetornoACasa?.onResume()  }
    override fun onPause()     { mapBloquearZonas?.onPause();   mapRetornoACasa?.onPause();   super.onPause()    }
    override fun onDestroy()   { mapBloquearZonas?.onDestroy(); mapRetornoACasa?.onDestroy(); super.onDestroy()  }
    override fun onLowMemory() { super.onLowMemory(); mapBloquearZonas?.onLowMemory(); mapRetornoACasa?.onLowMemory() }
}
