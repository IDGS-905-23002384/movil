package com.maithear.reach.mapas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

class UbicacionActivity : BaseActivity() {

    companion object {
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
        const val EXTRA_CALLE = "calle"
        const val EXTRA_NUMERO = "numero"
        const val EXTRA_COLONIA = "colonia"
        const val EXTRA_CIUDAD = "ciudad"
        const val EXTRA_ESTADO = "estado"
        const val EXTRA_CP = "cp"
    }

    private lateinit var mapa: MapView
    private lateinit var tvDireccion: TextView
    private lateinit var tvCoordenadas: TextView
    private var detectada: DireccionDetectada? = null
    private var trabajo: Job? = null

    private val permisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) centrarEnMiUbicacion() else toast("Mueve el mapa manualmente para ubicar tu domicilio")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ubicacion)
        configurarHeader("Ubica tu domicilio", "Mapa")
        findViewById<View>(R.id.barraUbicacion).insetsSistema(abajo = true)
        tvDireccion = findViewById(R.id.tvUbicacionDireccion)
        tvCoordenadas = findViewById(R.id.tvUbicacionCoordenadas)
        mapa = findViewById(R.id.mapaUbicacion)

        val latitud = intent.getDoubleExtra(EXTRA_LATITUD, Double.NaN)
        val longitud = intent.getDoubleExtra(EXTRA_LONGITUD, Double.NaN)
        val inicial = if (latitud.isNaN() || longitud.isNaN()) Mapas.LEON else GeoPoint(latitud, longitud)
        Mapas.preparar(mapa, inicial, if (inicial === Mapas.LEON) 13.0 else 17.5)

        mapa.addMapListener(DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                actualizar()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                actualizar()
                return true
            }
        }, 500))

        findViewById<View>(R.id.btnMiUbicacion).setOnClickListener { pedirUbicacion() }
        findViewById<MaterialButton>(R.id.btnConfirmarUbicacion).setOnClickListener { confirmar() }

        if (inicial === Mapas.LEON) {
            if (Ubicacion.tienePermiso(this)) centrarEnMiUbicacion() else permisos.launch(Ubicacion.PERMISOS)
        }
        actualizar()
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
    }

    override fun onPause() {
        mapa.onPause()
        super.onPause()
    }

    private fun pedirUbicacion() {
        if (Ubicacion.tienePermiso(this)) centrarEnMiUbicacion() else permisos.launch(Ubicacion.PERMISOS)
    }

    private fun centrarEnMiUbicacion() {
        tvDireccion.text = "Buscando tu ubicación…"
        Ubicacion.obtener(this) { ubicacion ->
            if (ubicacion == null) {
                toast("No pudimos obtener tu ubicación. Activa el GPS o mueve el mapa.")
                actualizar()
                return@obtener
            }
            mapa.controller.setZoom(17.5)
            mapa.controller.animateTo(GeoPoint(ubicacion.latitude, ubicacion.longitude))
        }
    }

    private fun actualizar() {
        val centro = mapa.mapCenter
        val latitud = centro.latitude
        val longitud = centro.longitude
        tvCoordenadas.text = String.format(Locale.US, "%.5f, %.5f", latitud, longitud)
        trabajo?.cancel()
        trabajo = lifecycleScope.launch {
            val d = Ubicacion.direccionDe(this@UbicacionActivity, latitud, longitud)
            detectada = d
            tvDireccion.text = d?.texto() ?: "Ubicación seleccionada"
        }
    }

    private fun confirmar() {
        val centro = mapa.mapCenter
        val d = detectada
        val datos = Intent()
            .putExtra(EXTRA_LATITUD, centro.latitude)
            .putExtra(EXTRA_LONGITUD, centro.longitude)
            .putExtra(EXTRA_CALLE, d?.calle)
            .putExtra(EXTRA_NUMERO, d?.numero)
            .putExtra(EXTRA_COLONIA, d?.colonia)
            .putExtra(EXTRA_CIUDAD, d?.ciudad)
            .putExtra(EXTRA_ESTADO, d?.estado)
            .putExtra(EXTRA_CP, d?.codigoPostal)
        setResult(Activity.RESULT_OK, datos)
        finish()
    }
}
