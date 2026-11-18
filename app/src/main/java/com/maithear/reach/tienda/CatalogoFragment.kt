package com.maithear.reach.tienda

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.maithear.reach.R
import com.maithear.reach.core.ProductoLista
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CatalogoFragment : Fragment(), Refrescable {

    private val ordenes = linkedMapOf(
        "relevancia" to "Relevancia",
        "precio_asc" to "Menor precio",
        "precio_desc" to "Mayor precio",
        "descuento" to "Mayor descuento",
        "nombre" to "Nombre A-Z",
        "recientes" to "Más recientes"
    )

    private var categoriaId: Int? = null
    private var busqueda: String = ""
    private var soloOfertas = false
    private var orden = "relevancia"
    private var enfocar = false
    private var pagina = 1
    private var totalPaginas = 1
    private var cargando = false
    private var cargado = false
    private var trabajo: Job? = null

    private lateinit var adaptador: ProductoAdapter
    private lateinit var refrescar: SwipeRefreshLayout
    private lateinit var etBuscar: EditText
    private lateinit var chips: ChipGroup
    private lateinit var chipOfertas: Chip
    private lateinit var btnOrden: MaterialButton
    private lateinit var tvResultados: TextView
    private var sincronizandoChips = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_catalogo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerCatalogo).insetsSistema(arriba = true)
        refrescar = view.findViewById(R.id.refrescarCatalogo)
        etBuscar = view.findViewById(R.id.etBuscar)
        chips = view.findViewById(R.id.chipsCategorias)
        chipOfertas = view.findViewById(R.id.chipOfertas)
        btnOrden = view.findViewById(R.id.btnOrden)
        tvResultados = view.findViewById(R.id.tvResultados)
        val btnLimpiar = view.findViewById<ImageButton>(R.id.btnLimpiarBusqueda)

        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { recargar() }

        adaptador = ProductoAdapter(
            alAgregar = { p -> agregar(p) },
            alTocar = { p -> ProductoDetalleActivity.abrir(requireContext(), p.id) }
        )
        val rv = view.findViewById<RecyclerView>(R.id.rvCatalogo)
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

        etBuscar.setText(busqueda)
        btnLimpiar.visible(busqueda.isNotEmpty())
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                btnLimpiar.visible(!s.isNullOrEmpty())
            }
        })
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
        btnLimpiar.setOnClickListener {
            etBuscar.setText("")
            busqueda = ""
            recargar()
        }

        chipOfertas.isChecked = soloOfertas
        chipOfertas.setOnCheckedChangeListener { _, marcado ->
            if (soloOfertas != marcado) {
                soloOfertas = marcado
                recargar()
            }
        }

        btnOrden.text = ordenes[orden]
        btnOrden.setOnClickListener { mostrarOrden() }

        chips.setOnCheckedStateChangeListener { grupo, ids ->
            if (sincronizandoChips || ids.isEmpty()) return@setOnCheckedStateChangeListener
            val nuevo = grupo.findViewById<Chip>(ids.first())?.tag as? Int
            if (nuevo != categoriaId) {
                categoriaId = nuevo
                recargar()
            }
        }
        cargarCategorias()
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() {
        if (!cargado) recargar()
        if (enfocar && view != null) {
            enfocar = false
            etBuscar.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(etBuscar, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun aplicarFiltro(categoria: Int?, texto: String?, ofertas: Boolean, enfocarBusqueda: Boolean) {
        categoriaId = categoria
        busqueda = texto ?: ""
        soloOfertas = ofertas
        enfocar = enfocarBusqueda
        cargado = false
        if (view != null) {
            etBuscar.setText(busqueda)
            chipOfertas.isChecked = soloOfertas
            sincronizarChips()
        }
    }

    private fun cargarCategorias() {
        viewLifecycleOwner.lifecycleScope.launch {
            api { categorias() }.siOk { lista ->
                sincronizandoChips = true
                chips.removeAllViews()
                chips.agregarChip("Todos", null, categoriaId == null)
                for (c in lista) chips.agregarChip(c.nombre, c.id, c.id == categoriaId)
                sincronizandoChips = false
            }
        }
    }

    private fun sincronizarChips() {
        sincronizandoChips = true
        for (i in 0 until chips.childCount) {
            val chip = chips.getChildAt(i) as? Chip ?: continue
            if (chip.tag as? Int == categoriaId) chip.isChecked = true
        }
        sincronizandoChips = false
    }

    private fun mostrarOrden() {
        val menu = PopupMenu(requireContext(), btnOrden)
        ordenes.values.forEachIndexed { i, texto -> menu.menu.add(0, i, i, texto) }
        menu.setOnMenuItemClickListener { item ->
            val clave = ordenes.keys.elementAt(item.itemId)
            if (clave != orden) {
                orden = clave
                btnOrden.text = ordenes[orden]
                recargar()
            }
            true
        }
        menu.show()
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
                    orden = if (orden == "relevancia") null else orden,
                    soloOfertas = if (soloOfertas) true else null,
                    pagina = numero,
                    tamano = 20
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
                    tvResultados.text = if (r.dato.total == 1) "1 producto" else "${r.dato.total} productos"
                    if (r.dato.total == 0) {
                        raiz.mostrarVacio(R.drawable.ic_search, "Sin resultados", "Prueba con otra búsqueda o categoría.", "Limpiar filtros") {
                            aplicarFiltro(null, "", false, false)
                            recargar()
                        }
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> {
                    if (numero == 1) {
                        adaptador.establecer(emptyList())
                        raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar el catálogo", r.mensaje, "Reintentar") { recargar() }
                    }
                }
            }
        }
    }

    private fun agregar(p: ProductoLista) {
        viewLifecycleOwner.lifecycleScope.launch { AccionesCarrito.agregar(requireActivity(), p.id, 1, p.nombre) }
    }
}
