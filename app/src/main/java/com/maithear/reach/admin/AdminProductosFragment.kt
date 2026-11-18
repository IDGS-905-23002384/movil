package com.maithear.reach.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.maithear.reach.R
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.agregarChip
import com.maithear.reach.core.api
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.siOk
import com.maithear.reach.core.valor
import com.maithear.reach.core.visible
import com.maithear.reach.tienda.ProductoAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdminProductosFragment : Fragment(), Refrescable {

    private var categoriaId: Int? = null
    private var busqueda = ""
    private var pagina = 1
    private var totalPaginas = 1
    private var cargando = false
    private var trabajo: Job? = null
    private var recargarAlVolver = false
    private var cargado = false

    private lateinit var refrescar: SwipeRefreshLayout
    private lateinit var chips: ChipGroup
    private lateinit var adaptador: ProductoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_admin_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerAdminLista).insetsSistema(arriba = true)
        view.findViewById<TextView>(R.id.tvAdminListaEyebrow).text = "Catálogo"
        view.findViewById<TextView>(R.id.tvAdminListaTitulo).text = "Productos"
        val etBuscar = view.findViewById<EditText>(R.id.etBuscarAdmin)
        etBuscar.hint = "Buscar por nombre, marca o SKU"
        etBuscar.setOnEditorActionListener { v, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                busqueda = etBuscar.valor()
                v.ocultarTeclado()
                recargar()
                true
            } else {
                false
            }
        }

        refrescar = view.findViewById(R.id.refrescarAdminLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { recargar() }

        chips = view.findViewById(R.id.chipsAdmin)
        chips.setOnCheckedStateChangeListener { grupo, ids ->
            if (ids.isEmpty()) return@setOnCheckedStateChangeListener
            val nueva = grupo.findViewById<Chip>(ids.first())?.tag as? Int
            if (nueva != categoriaId) {
                categoriaId = nueva
                recargar()
            }
        }

        adaptador = ProductoAdapter(alTocar = { abrirFormulario(it.id) })
        val rv = view.findViewById<RecyclerView>(R.id.rvAdminLista)
        val grid = GridLayoutManager(context, 2)
        rv.layoutManager = grid
        rv.adapter = adaptador
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && !cargando && pagina < totalPaginas && grid.findLastVisibleItemPosition() >= adaptador.itemCount - 4) {
                    cargarPagina(pagina + 1)
                }
            }
        })

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabAdmin).apply {
            text = "Nuevo producto"
            visible(true)
            setOnClickListener { abrirFormulario(null) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            api { categorias() }.siOk { lista ->
                chips.agregarChip("Todos", null, categoriaId == null)
                lista.forEach { chips.agregarChip(it.nombre, it.id, it.id == categoriaId) }
            }
        }
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() {
        if (!cargado) recargar()
    }

    override fun onResume() {
        super.onResume()
        if (recargarAlVolver && view != null) {
            recargarAlVolver = false
            recargar()
        }
    }

    private fun abrirFormulario(id: Int?) {
        recargarAlVolver = true
        val intent = Intent(requireContext(), FormProductoActivity::class.java)
        if (id != null) intent.putExtra(FormProductoActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    private fun recargar() {
        if (view == null) return
        trabajo?.cancel()
        cargando = false
        cargarPagina(1)
    }

    private fun cargarPagina(numero: Int) {
        val raiz = view ?: return
        cargando = true
        if (numero == 1) refrescar.isRefreshing = true
        trabajo = viewLifecycleOwner.lifecycleScope.launch {
            val r = api {
                productos(
                    categoriaId = categoriaId,
                    q = busqueda.ifBlank { null },
                    orden = "nombre",
                    incluirInactivos = true,
                    pagina = numero,
                    tamano = 30
                )
            }
            refrescar.isRefreshing = false
            cargando = false
            when (r) {
                is Resultado.Ok -> {
                    cargado = true
                    pagina = r.dato.pagina
                    totalPaginas = r.dato.totalPaginas
                    if (numero == 1) adaptador.establecer(r.dato.items) else adaptador.agregar(r.dato.items)
                    if (r.dato.total == 0) raiz.mostrarVacio(R.drawable.ic_grid, "Sin productos", "No hay productos con este filtro.") else raiz.ocultarVacio()
                }
                is Resultado.Error -> if (numero == 1) {
                    raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar los productos", r.mensaje, "Reintentar") { recargar() }
                }
            }
        }
    }
}
