package com.maithear.reach.mapas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.Sucursal
import com.maithear.reach.core.api
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class SucursalesActivity : BaseActivity() {

    companion object {
        const val EXTRA_SELECCION = "seleccion_sucursal"
        const val EXTRA_SUCURSAL_ID = "sucursal_id"
    }

    private lateinit var mapa: MapView
    private lateinit var lista: RecyclerView
    private var sucursales: List<Sucursal> = emptyList()
    private var miUbicacion: GeoPoint? = null
    private var marcadorYo: Marker? = null
    private var seleccionada: Int = 0
    private val adaptador = SucursalAdapter()

    private val modoSeleccion: Boolean
        get() = intent.getBooleanExtra(EXTRA_SELECCION, false)

    private val permisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) ubicarme() else toast("Sin permiso de ubicación no podemos calcular distancias")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sucursales)
        configurarHeader(if (modoSeleccion) "Elige dónde recoger" else "Sucursales Reach", "Mapa")
        mapa = findViewById(R.id.mapaSucursales)
        Mapas.preparar(mapa)

        lista = findViewById(R.id.rvSucursales)
        lista.insetsSistema(abajo = true)
        lista.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        lista.adapter = adaptador

        findViewById<View>(R.id.btnMiUbicacionSuc).setOnClickListener {
            if (Ubicacion.tienePermiso(this)) ubicarme() else permisos.launch(Ubicacion.PERMISOS)
        }
        seleccionada = intent.getIntExtra(EXTRA_SUCURSAL_ID, 0)
        cargar()
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
    }

    override fun onPause() {
        mapa.onPause()
        super.onPause()
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val r = api { sucursales() }) {
                is Resultado.Ok -> {
                    sucursales = r.dato
                    pintarMarcadores()
                    adaptador.notifyDataSetChanged()
                    Mapas.encuadrar(mapa, sucursales.map { GeoPoint(it.latitud, it.longitud) })
                    if (Ubicacion.tienePermiso(this@SucursalesActivity)) ubicarme()
                }
                is Resultado.Error -> toast(r.mensaje)
            }
        }
    }

    private fun pintarMarcadores() {
        Mapas.limpiar(mapa)
        sucursales.forEachIndexed { i, s ->
            val m = Mapas.marcador(mapa, GeoPoint(s.latitud, s.longitud), s.nombre, R.drawable.pin_tienda)
            m.snippet = s.direccion
            m.setOnMarkerClickListener { marcador, _ ->
                marcador.showInfoWindow()
                enfocar(i)
                true
            }
        }
        miUbicacion?.let { marcadorYo = Mapas.marcador(mapa, it, "Estás aquí", R.drawable.pin_destino) }
        mapa.invalidate()
    }

    private fun ubicarme() {
        Ubicacion.obtener(this) { ubicacion ->
            if (ubicacion == null) {
                toast("No pudimos obtener tu ubicación. Activa el GPS e intenta de nuevo.")
                return@obtener
            }
            val yo = GeoPoint(ubicacion.latitude, ubicacion.longitude)
            miUbicacion = yo
            sucursales = sucursales.sortedBy { Mapas.distanciaKm(yo, it.latitud, it.longitud) }
            pintarMarcadores()
            adaptador.notifyDataSetChanged()
            val cercana = sucursales.firstOrNull() ?: return@obtener
            Mapas.encuadrar(mapa, listOf(yo, GeoPoint(cercana.latitud, cercana.longitud)))
            lista.smoothScrollToPosition(0)
        }
    }

    private fun enfocar(indice: Int) {
        val s = sucursales.getOrNull(indice) ?: return
        mapa.controller.animateTo(GeoPoint(s.latitud, s.longitud))
        lista.smoothScrollToPosition(indice)
    }

    private fun elegir(s: Sucursal) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SUCURSAL_ID, s.id))
        finish()
    }

    inner class SucursalAdapter : RecyclerView.Adapter<SucursalAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_sucursal))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = sucursales[position]
            val v = holder.itemView
            v.findViewById<TextView>(R.id.tvSucNombre).text = s.nombre
            v.findViewById<TextView>(R.id.tvSucDireccion).text = "${s.direccion}, ${s.ciudad}"
            v.findViewById<TextView>(R.id.tvSucHorario).text = s.horario
            val distancia = miUbicacion?.let { Mapas.distanciaKm(it, s.latitud, s.longitud) }
            v.findViewById<TextView>(R.id.tvSucDistancia).apply {
                text = distancia?.let { if (it < 1) "${(it * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", it) } ?: ""
                visible(distancia != null)
            }
            v.findViewById<View>(R.id.cardSucursal).setBackgroundResource(
                if (s.id == seleccionada) R.drawable.bg_card_seleccionada else R.drawable.bg_card_borde
            )
            v.findViewById<MaterialButton>(R.id.btnSucComoLlegar).setOnClickListener {
                Mapas.abrirNavegacion(this@SucursalesActivity, s.latitud, s.longitud, s.nombre)
            }
            v.findViewById<MaterialButton>(R.id.btnSucLlamar).setOnClickListener { Mapas.llamar(this@SucursalesActivity, s.telefono) }
            v.findViewById<MaterialButton>(R.id.btnSucElegir).apply {
                visible(modoSeleccion)
                setOnClickListener { elegir(s) }
            }
            v.setOnClickListener { enfocar(holder.bindingAdapterPosition) }
        }

        override fun getItemCount(): Int = sucursales.size
    }
}
