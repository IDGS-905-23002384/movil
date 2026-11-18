package com.maithear.reach.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.maithear.reach.R
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.agregarChip
import com.maithear.reach.core.api
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.valor
import com.maithear.reach.pedidos.PedidoAdapter
import com.maithear.reach.pedidos.PedidoDetalleActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdminPedidosFragment : Fragment(), Refrescable {

    private val filtros = linkedMapOf<String?, String>(
        null to "Todos",
        "pendientes" to "Por enviar",
        "ensamble" to "En ensamble",
        "enviados" to "Enviados",
        "entregados" to "Entregados",
        "cancelados" to "Cancelados"
    )

    private var estado: String? = null
    private var busqueda = ""
    private var pagina = 1
    private var totalPaginas = 1
    private var cargando = false
    private var trabajo: Job? = null
    private var sincronizando = false

    private lateinit var refrescar: SwipeRefreshLayout
    private lateinit var chips: ChipGroup
    private var recargarAlVolver = false
    private val adaptador = PedidoAdapter(mostrarCliente = true) {
        recargarAlVolver = true
        PedidoDetalleActivity.abrir(requireContext(), it.id, admin = true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_admin_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerAdminLista).insetsSistema(arriba = true)
        view.findViewById<TextView>(R.id.tvAdminListaEyebrow).text = "Operación"
        view.findViewById<TextView>(R.id.tvAdminListaTitulo).text = "Pedidos"
        val etBuscar = view.findViewById<EditText>(R.id.etBuscarAdmin)
        etBuscar.hint = "Buscar por folio, cliente o correo"
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
        sincronizando = true
        filtros.forEach { (clave, texto) -> chips.agregarChip(texto, clave, clave == estado) }
        sincronizando = false
        chips.setOnCheckedStateChangeListener { grupo, ids ->
            if (sincronizando || ids.isEmpty()) return@setOnCheckedStateChangeListener
            estado = grupo.findViewById<Chip>(ids.first())?.tag as? String
            recargar()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminLista)
        val manager = LinearLayoutManager(context)
        rv.layoutManager = manager
        rv.adapter = adaptador
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && !cargando && pagina < totalPaginas && manager.findLastVisibleItemPosition() >= adaptador.itemCount - 3) {
                    cargarPagina(pagina + 1)
                }
            }
        })
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() = recargar()

    override fun onResume() {
        super.onResume()
        if (recargarAlVolver && !isHidden && view != null) {
            recargarAlVolver = false
            recargar()
        }
    }

    fun aplicarFiltro(filtro: String?) {
        estado = filtro
        if (view != null) {
            sincronizando = true
            for (i in 0 until chips.childCount) {
                val chip = chips.getChildAt(i) as? Chip ?: continue
                if (chip.tag as? String == filtro) chip.isChecked = true
            }
            sincronizando = false
        }
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
            val r = api { pedidosAdmin(estado, busqueda.ifBlank { null }, numero, 30) }
            refrescar.isRefreshing = false
            cargando = false
            when (r) {
                is Resultado.Ok -> {
                    pagina = r.dato.pagina
                    totalPaginas = r.dato.totalPaginas
                    if (numero == 1) adaptador.establecer(r.dato.items) else adaptador.agregar(r.dato.items)
                    if (r.dato.total == 0) {
                        raiz.mostrarVacio(R.drawable.ic_receipt, "Sin pedidos", "No hay pedidos con este filtro.")
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> if (numero == 1) {
                    adaptador.establecer(emptyList())
                    raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar los pedidos", r.mensaje, "Reintentar") { recargar() }
                }
            }
        }
    }
}
