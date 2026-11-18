package com.maithear.reach.cuenta

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Direccion
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.api
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.exito
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.launch

class DireccionesActivity : BaseActivity() {

    companion object {
        const val EXTRA_SELECCION = "seleccion"
        const val EXTRA_DIRECCION_ID = "direccion_id"
    }

    private val modoSeleccion: Boolean
        get() = intent.getBooleanExtra(EXTRA_SELECCION, false)

    private lateinit var refrescar: SwipeRefreshLayout
    private val adaptador = DireccionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista)
        configurarHeader(if (modoSeleccion) "Elige una dirección" else "Mis direcciones", "Entregas")
        findViewById<View>(R.id.barraLista).apply {
            visible(true)
            insetsSistema(abajo = true)
        }
        findViewById<MaterialButton>(R.id.btnListaAccion).apply {
            text = "Agregar dirección"
            setOnClickListener { abrirFormulario(null) }
        }
        refrescar = findViewById(R.id.refrescarLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }
        findViewById<RecyclerView>(R.id.rvLista).apply {
            layoutManager = LinearLayoutManager(this@DireccionesActivity)
            adapter = adaptador
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        refrescar.isRefreshing = true
        lifecycleScope.launch {
            val r = api { direcciones() }
            refrescar.isRefreshing = false
            val raiz = findViewById<View>(android.R.id.content)
            when (r) {
                is Resultado.Ok -> {
                    adaptador.establecer(r.dato)
                    if (r.dato.isEmpty()) {
                        raiz.mostrarVacio(R.drawable.ic_location, "Sin direcciones", "Agrega el domicilio donde quieres recibir tus pedidos.")
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar tus direcciones", r.mensaje, "Reintentar") { cargar() }
            }
        }
    }

    private fun abrirFormulario(direccion: Direccion?) {
        val intent = Intent(this, DireccionFormActivity::class.java)
        if (direccion != null) intent.putExtra(DireccionFormActivity.EXTRA_DIRECCION, Gson().toJson(direccion))
        startActivity(intent)
    }

    private fun elegir(direccion: Direccion) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_DIRECCION_ID, direccion.id))
        finish()
    }

    private fun accion(bloque: suspend com.maithear.reach.core.ApiService.() -> Unit, mensaje: String) {
        lifecycleScope.launch {
            api(bloque).siOk {
                toast(mensaje)
                cargar()
            }.siError { toast(it.mensaje) }
        }
    }

    inner class DireccionAdapter : RecyclerView.Adapter<DireccionAdapter.VH>() {
        private var items: List<Direccion> = emptyList()

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        fun establecer(lista: List<Direccion>) {
            items = lista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_direccion))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val d = items[position]
            val v = holder.itemView
            v.findViewById<TextView>(R.id.tvDirAlias).text = d.alias
            v.findViewById<View>(R.id.tvDirPredeterminada).visible(d.esPredeterminada)
            v.findViewById<TextView>(R.id.tvDirNombre).text = d.nombreRecibe
            val interior = if (d.numeroInterior.isNullOrBlank()) "" else " Int. ${d.numeroInterior}"
            v.findViewById<TextView>(R.id.tvDirTexto).text =
                "${d.calle} ${d.numeroExterior}$interior, ${d.colonia}\n${d.ciudad}, ${d.estado}, C.P. ${d.codigoPostal}\nTel. ${d.telefono}"
            v.findViewById<View>(R.id.btnDirPredeterminar).apply {
                visible(!d.esPredeterminada)
                setOnClickListener { accion({ predeterminarDireccion(d.id).exito() }, "Dirección predeterminada actualizada") }
            }
            v.findViewById<View>(R.id.btnDirEditar).setOnClickListener { abrirFormulario(d) }
            v.findViewById<View>(R.id.btnDirEliminar).setOnClickListener {
                confirmar("Eliminar dirección", "¿Quieres eliminar \"${d.alias}\"?", "Eliminar") {
                    accion({ eliminarDireccion(d.id).exito() }, "Dirección eliminada")
                }
            }
            v.setOnClickListener { if (modoSeleccion) elegir(d) else abrirFormulario(d) }
        }

        override fun getItemCount(): Int = items.size
    }
}
