package com.maithear.reach.admin

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.configurador.ArmaPcFragment
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Campo
import com.maithear.reach.core.Categoria
import com.maithear.reach.core.CategoriaGuardar
import com.maithear.reach.core.Proveedor
import com.maithear.reach.core.ProveedorGuardar
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.api
import com.maithear.reach.core.dialogoFormulario
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.launch

class CatalogosActivity : BaseActivity() {

    companion object {
        const val EXTRA_TIPO = "tipo"
        const val TIPO_PROVEEDORES = "proveedores"
        const val TIPO_CATEGORIAS = "categorias"
    }

    private data class Fila(val titulo: String, val subtitulo: String, val badge: String, val activo: Boolean, val accion: () -> Unit)

    private val esProveedores: Boolean
        get() = intent.getStringExtra(EXTRA_TIPO) != TIPO_CATEGORIAS

    private lateinit var refrescar: SwipeRefreshLayout
    private val adaptador = FilaAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista)
        configurarHeader(if (esProveedores) "Proveedores" else "Categorías", "Administración")
        findViewById<View>(R.id.barraLista).apply {
            visible(true)
            insetsSistema(abajo = true)
        }
        findViewById<MaterialButton>(R.id.btnListaAccion).apply {
            text = if (esProveedores) "Nuevo proveedor" else "Nueva categoría"
            setOnClickListener { if (esProveedores) editarProveedor(null) else editarCategoria(null) }
        }
        refrescar = findViewById(R.id.refrescarLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }
        findViewById<RecyclerView>(R.id.rvLista).apply {
            layoutManager = LinearLayoutManager(this@CatalogosActivity)
            adapter = adaptador
        }
        cargar()
    }

    private fun cargar() {
        refrescar.isRefreshing = true
        lifecycleScope.launch {
            val raiz = findViewById<View>(android.R.id.content)
            val filas: List<Fila>? = if (esProveedores) {
                when (val r = api { proveedores() }) {
                    is Resultado.Ok -> r.dato.map { p ->
                        Fila(
                            p.nombreEmpresa,
                            listOfNotNull(p.contactoPersona?.ifBlank { null }, p.telefono?.ifBlank { null }, p.email?.ifBlank { null }).joinToString(" · "),
                            if (p.activo) "${p.totalProductos} productos" else "Inactivo",
                            p.activo
                        ) { editarProveedor(p) }
                    }
                    is Resultado.Error -> {
                        raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar", r.mensaje, "Reintentar") { cargar() }
                        null
                    }
                }
            } else {
                when (val r = api { categorias() }) {
                    is Resultado.Ok -> r.dato.map { c ->
                        Fila(
                            c.nombre,
                            c.tipoComponente?.let { "Paso del configurador: ${ArmaPcFragment.nombreTipo(it)}" } ?: "Categoría de tienda",
                            "${c.totalProductos} productos",
                            true
                        ) { editarCategoria(c) }
                    }
                    is Resultado.Error -> {
                        raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar", r.mensaje, "Reintentar") { cargar() }
                        null
                    }
                }
            }
            refrescar.isRefreshing = false
            if (filas != null) {
                adaptador.establecer(filas)
                if (filas.isEmpty()) raiz.mostrarVacio(R.drawable.ic_inventory, "Sin registros", "Agrega el primero con el botón de abajo.") else raiz.ocultarVacio()
            }
        }
    }

    private fun editarProveedor(p: Proveedor?) {
        dialogoFormulario(
            titulo = if (p == null) "Nuevo proveedor" else "Editar proveedor",
            campos = listOf(
                Campo("empresa", "Empresa", p?.nombreEmpresa.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS),
                Campo("contacto", "Persona de contacto", p?.contactoPersona.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS),
                Campo("telefono", "Teléfono", p?.telefono.orEmpty(), InputType.TYPE_CLASS_PHONE, "10 dígitos"),
                Campo("email", "Correo", p?.email.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            )
        ) { v, dialogo ->
            val empresa = v["empresa"].orEmpty()
            val telefono = v["telefono"].orEmpty()
            val email = v["email"].orEmpty()
            when {
                empresa.length < 2 -> toast("Escribe el nombre de la empresa")
                telefono.isNotEmpty() && !Regex("^\\d{10}$").matches(telefono) -> toast("El teléfono debe tener 10 dígitos")
                email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Correo no válido")
                else -> lifecycleScope.launch {
                    val datos = ProveedorGuardar(empresa, v["contacto"].orEmpty(), telefono.ifEmpty { null }, email.ifEmpty { null }, p?.activo ?: true)
                    val r = api { if (p == null) crearProveedor(datos) else actualizarProveedor(p.id, datos) }
                    when (r) {
                        is Resultado.Ok -> {
                            dialogo.dismiss()
                            toast("Proveedor guardado")
                            cargar()
                        }
                        is Resultado.Error -> toast(r.mensaje)
                    }
                }
            }
        }
    }

    private fun editarCategoria(c: Categoria?) {
        dialogoFormulario(
            titulo = if (c == null) "Nueva categoría" else "Editar categoría",
            mensaje = if (c?.tipoComponente != null) "Esta categoría alimenta el paso \"${ArmaPcFragment.nombreTipo(c.tipoComponente)}\" del configurador." else null,
            campos = listOf(
                Campo("nombre", "Nombre", c?.nombre.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES),
                Campo("orden", "Orden de aparición", (c?.orden ?: 20).toString(), InputType.TYPE_CLASS_NUMBER)
            )
        ) { v, dialogo ->
            val nombre = v["nombre"].orEmpty()
            val orden = v["orden"]?.toIntOrNull()
            when {
                nombre.length < 2 -> toast("Escribe el nombre")
                orden == null || orden !in 0..999 -> toast("Orden no válido")
                else -> lifecycleScope.launch {
                    val datos = CategoriaGuardar(nombre, c?.tipoComponente, orden)
                    val r = api { if (c == null) crearCategoria(datos) else actualizarCategoria(c.id, datos) }
                    when (r) {
                        is Resultado.Ok -> {
                            dialogo.dismiss()
                            toast("Categoría guardada")
                            cargar()
                        }
                        is Resultado.Error -> toast(r.mensaje)
                    }
                }
            }
        }
    }

    private inner class FilaAdapter : RecyclerView.Adapter<FilaAdapter.VH>() {
        private var filas: List<Fila> = emptyList()

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        fun establecer(lista: List<Fila>) {
            filas = lista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_simple))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val f = filas[position]
            val v = holder.itemView
            v.findViewById<TextView>(R.id.tvSimpleTitulo).text = f.titulo
            v.findViewById<TextView>(R.id.tvSimpleSub).apply {
                text = f.subtitulo
                visible(f.subtitulo.isNotBlank())
            }
            v.findViewById<TextView>(R.id.tvSimpleBadge).apply {
                text = f.badge
                setBackgroundResource(if (f.activo) R.drawable.bg_badge_azul_claro else R.drawable.bg_badge_gris)
                setTextColor(ContextCompat.getColor(context, if (f.activo) R.color.azul else R.color.texto_sec))
            }
            v.setOnClickListener { f.accion() }
        }

        override fun getItemCount(): Int = filas.size
    }
}
