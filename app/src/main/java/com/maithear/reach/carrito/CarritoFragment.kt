package com.maithear.reach.carrito

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.maithear.reach.R
import com.maithear.reach.checkout.CheckoutActivity
import com.maithear.reach.core.ActualizarCantidad
import com.maithear.reach.core.Carrito
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.api
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.exito
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.mxn
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import com.maithear.reach.tienda.ProductoDetalleActivity
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.launch

class CarritoFragment : Fragment(), Refrescable {

    private lateinit var adaptador: CarritoAdapter
    private lateinit var refrescar: SwipeRefreshLayout
    private var ocupado = false
    private var carritoActual: Carrito? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_carrito, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerCarrito).insetsSistema(arriba = true)
        refrescar = view.findViewById(R.id.refrescarCarrito)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }

        adaptador = CarritoAdapter(
            alCambiarCantidad = { item, nueva ->
                operar { actualizarCantidad(item.id, ActualizarCantidad(nueva)) }
            },
            alEliminar = { item ->
                operar { eliminarItem(item.id) }
            },
            alEliminarEnsamble = { id, nombre ->
                requireContext().confirmar("Quitar $nombre", "Se quitarán todas las piezas de este equipo.", "Quitar") {
                    operar { eliminarEnsamble(id) }
                }
            },
            alTocarProducto = { ProductoDetalleActivity.abrir(requireContext(), it) }
        )
        view.findViewById<RecyclerView>(R.id.rvCarrito).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adaptador
        }

        view.findViewById<View>(R.id.btnVaciarCarrito).setOnClickListener {
            requireContext().confirmar("Vaciar carrito", "¿Quieres quitar todos los productos?", "Vaciar") { vaciar() }
        }
        view.findViewById<MaterialButton>(R.id.btnContinuarCompra).setOnClickListener {
            startActivity(Intent(requireContext(), CheckoutActivity::class.java))
        }
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() {
        if (view == null) return
        if (!Sesion.activa) {
            pintarInvitado()
        } else {
            cargar()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden && Sesion.activa && carritoActual != null) cargar()
    }

    private fun pintarInvitado() {
        val raiz = view ?: return
        adaptador.establecer(emptyList())
        raiz.findViewById<View>(R.id.barraCarrito).visible(false)
        raiz.findViewById<View>(R.id.btnVaciarCarrito).visible(false)
        raiz.findViewById<TextView>(R.id.tvCarritoConteo).text = "Inicia sesión para comprar"
        raiz.mostrarVacio(R.drawable.ic_person, "Tu carrito te espera", "Inicia sesión para guardar productos y completar tu compra.", "Iniciar sesión") {
            activity?.requiereSesion()
        }
    }

    private fun cargar() {
        val raiz = view ?: return
        refrescar.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val r = api { carrito() }
            refrescar.isRefreshing = false
            when (r) {
                is Resultado.Ok -> pintar(r.dato)
                is Resultado.Error -> if (carritoActual == null) {
                    raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar tu carrito", r.mensaje, "Reintentar") { cargar() }
                } else {
                    requireContext().toast(r.mensaje)
                }
            }
        }
    }

    private fun operar(llamada: suspend com.maithear.reach.core.ApiService.() -> Carrito) {
        if (ocupado) return
        ocupado = true
        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = api(llamada)) {
                is Resultado.Ok -> pintar(r.dato)
                is Resultado.Error -> requireContext().toast(r.mensaje)
            }
            ocupado = false
        }
    }

    private fun vaciar() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = api { vaciarCarrito().exito() }) {
                is Resultado.Ok -> cargar()
                is Resultado.Error -> requireContext().toast(r.mensaje)
            }
        }
    }

    private fun pintar(c: Carrito) {
        val raiz = view ?: return
        carritoActual = c
        (activity as? TiendaActivity)?.actualizarBadge(c.totalArticulos)

        val filas = mutableListOf<FilaCarrito>()
        c.avisos.orEmpty().forEach { filas.add(FilaCarrito.Aviso(it)) }
        val costoPorEnsamble = if (c.totalEnsambles > 0) c.costoEnsamble / c.totalEnsambles else 0.0
        c.items.filter { it.ensambleId != null }.groupBy { it.ensambleId!! }.forEach { (id, piezas) ->
            filas.add(FilaCarrito.Ensamble(id, piezas.first().ensambleNombre ?: "PC personalizada", piezas, costoPorEnsamble))
        }
        c.items.filter { it.ensambleId == null }.forEach { filas.add(FilaCarrito.Item(it)) }
        adaptador.establecer(filas)

        val vacio = c.items.isEmpty()
        raiz.findViewById<TextView>(R.id.tvCarritoConteo).text = when (c.totalArticulos) {
            0 -> "Sin productos"
            1 -> "1 artículo"
            else -> "${c.totalArticulos} artículos"
        }
        raiz.findViewById<View>(R.id.btnVaciarCarrito).visible(!vacio)
        raiz.findViewById<View>(R.id.barraCarrito).visible(!vacio)
        if (vacio) {
            raiz.mostrarVacio(R.drawable.ic_cart, "Tu carrito está vacío", "Explora el catálogo o arma tu PC a la medida.", "Explorar catálogo") {
                (activity as? TiendaActivity)?.irA(R.id.nav_catalogo)
            }
            return
        }
        raiz.ocultarVacio()

        val tvEnvio = raiz.findViewById<TextView>(R.id.tvEnvioGratis)
        val progreso = raiz.findViewById<LinearProgressIndicator>(R.id.progEnvioGratis)
        if (c.faltaParaEnvioGratis > 0) {
            tvEnvio.text = "Te faltan ${c.faltaParaEnvioGratis.mxn()} para obtener envío gratis"
            progreso.progress = ((c.subtotalProductos / c.envioGratisDesde) * 100).toInt().coerceIn(0, 100)
        } else {
            tvEnvio.text = "¡Tu pedido tiene envío estándar gratis!"
            progreso.progress = 100
        }

        val totales = raiz.findViewById<LinearLayout>(R.id.llTotalesCarrito)
        totales.removeAllViews()
        totales.agregarFilaTotal("Productos", c.subtotalProductos.mxn())
        if (c.costoEnsamble > 0) totales.agregarFilaTotal("Ensamble y pruebas", c.costoEnsamble.mxn())
        totales.agregarFilaTotal("Total", c.total.mxn(), destacado = true)
    }
}
