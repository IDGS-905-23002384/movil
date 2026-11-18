package com.maithear.reach.tienda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.maithear.reach.R
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.iniciales
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.saludo
import com.maithear.reach.core.visible
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class InicioFragment : Fragment(), Refrescable {

    private lateinit var refrescar: SwipeRefreshLayout
    private lateinit var adCategorias: CategoriaAdapter
    private lateinit var adOfertas: ProductoAdapter
    private lateinit var adPrearmados: PrearmadoAdapter
    private lateinit var adDestacados: ProductoAdapter
    private var cargado = false

    private val tienda: TiendaActivity?
        get() = activity as? TiendaActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_inicio, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerInicio).insetsSistema(arriba = true)
        refrescar = view.findViewById(R.id.refrescarInicio)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }

        view.findViewById<View>(R.id.buscadorInicio).setOnClickListener { tienda?.abrirCatalogo(enfocarBusqueda = true) }
        view.findViewById<View>(R.id.cardBannerArma).setOnClickListener { tienda?.irA(R.id.nav_arma) }
        view.findViewById<View>(R.id.tvVerOfertas).setOnClickListener { tienda?.abrirCatalogo(soloOfertas = true) }

        val agregar: (com.maithear.reach.core.ProductoLista) -> Unit = { p ->
            viewLifecycleOwner.lifecycleScope.launch { AccionesCarrito.agregar(requireActivity(), p.id, 1, p.nombre) }
        }
        val abrir: (com.maithear.reach.core.ProductoLista) -> Unit = { p -> ProductoDetalleActivity.abrir(requireContext(), p.id) }

        adCategorias = CategoriaAdapter { tienda?.abrirCatalogo(categoriaId = it.id) }
        adOfertas = ProductoAdapter(carrusel = true, alAgregar = agregar, alTocar = abrir)
        adPrearmados = PrearmadoAdapter { PrearmadoSheet.mostrar(this, it) }
        adDestacados = ProductoAdapter(alAgregar = agregar, alTocar = abrir)

        view.findViewById<RecyclerView>(R.id.rvCategorias).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = adCategorias
        }
        view.findViewById<RecyclerView>(R.id.rvOfertas).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = adOfertas
        }
        view.findViewById<RecyclerView>(R.id.rvPrearmados).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = adPrearmados
        }
        view.findViewById<RecyclerView>(R.id.rvDestacados).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = adDestacados
        }
        pintarUsuario(view)
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() {
        view?.let { pintarUsuario(it) }
        if (!cargado) cargar()
    }

    private fun pintarUsuario(view: View) {
        val usuario = Sesion.usuario
        view.findViewById<TextView>(R.id.tvAvatar).text = iniciales(usuario?.nombre ?: "Reach")
        view.findViewById<TextView>(R.id.tvSaludo).text = saludo()
        view.findViewById<TextView>(R.id.tvNombreUsuario).text =
            if (Sesion.activa && usuario != null) usuario.nombre.substringBefore(' ') else "Bienvenido a Reach"
    }

    private fun cargar() {
        val raiz = view ?: return
        val progreso = raiz.findViewById<View>(R.id.progresoInicio)
        if (!cargado) progreso.visible(true)
        raiz.ocultarVacio()
        viewLifecycleOwner.lifecycleScope.launch {
            val rCategorias = async { api { categorias() } }
            val rOfertas = async { api { productos(soloOfertas = true, orden = "descuento", tamano = 10) } }
            val rPrearmados = async { api { prearmados() } }
            val rDestacados = async { api { productos(soloDestacados = true, tamano = 10) } }

            val cat = rCategorias.await()
            val ofe = rOfertas.await()
            val pre = rPrearmados.await()
            val des = rDestacados.await()

            if (cat is Resultado.Ok) adCategorias.establecer(cat.dato)
            if (ofe is Resultado.Ok) adOfertas.establecer(ofe.dato.items)
            if (pre is Resultado.Ok) adPrearmados.establecer(pre.dato)
            if (des is Resultado.Ok) adDestacados.establecer(des.dato.items)

            raiz.findViewById<View>(R.id.seccionOfertas).visible(ofe is Resultado.Ok && ofe.dato.items.isNotEmpty())
            progreso.visible(false)
            refrescar.isRefreshing = false

            if (cat is Resultado.Error && des is Resultado.Error) {
                raiz.mostrarVacio(R.drawable.ic_warning, "Sin conexión", cat.mensaje, "Reintentar") { cargar() }
            } else {
                cargado = true
            }
        }
    }
}
