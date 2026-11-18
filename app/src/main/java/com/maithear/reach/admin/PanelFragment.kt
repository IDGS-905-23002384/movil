package com.maithear.reach.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.maithear.reach.R
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.ResumenPanel
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.iniciales
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mxn
import com.maithear.reach.core.saludo
import com.maithear.reach.core.toast
import com.maithear.reach.pedidos.PedidoAdapter
import com.maithear.reach.pedidos.PedidoDetalleActivity
import kotlinx.coroutines.launch

class PanelFragment : Fragment(), Refrescable {

    private lateinit var refrescar: SwipeRefreshLayout

    private val admin: AdminActivity?
        get() = activity as? AdminActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_panel, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerPanel).insetsSistema(arriba = true)
        refrescar = view.findViewById(R.id.refrescarPanel)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }

        val usuario = Sesion.usuario
        view.findViewById<TextView>(R.id.tvPanelAvatar).text = iniciales(usuario?.nombre)
        view.findViewById<TextView>(R.id.tvPanelSaludo).text = saludo()
        view.findViewById<TextView>(R.id.tvPanelNombre).text = usuario?.nombre.orEmpty()
        view.findViewById<TextView>(R.id.tvPanelRol).text = usuario?.rol.orEmpty()
        view.findViewById<View>(R.id.tvVerPedidos).setOnClickListener { admin?.abrirPedidos(null) }

        kpi(view, R.id.kpiPorEnviar, R.drawable.ic_truck, R.color.azul, R.drawable.bg_circulo_azul, "Por enviar") { admin?.abrirPedidos("pendientes") }
        kpi(view, R.id.kpiEnsamble, R.drawable.ic_build, R.color.morado, R.drawable.bg_badge_morado, "En ensamble") { admin?.abrirPedidos("ensamble") }
        kpi(view, R.id.kpiStockBajo, R.drawable.ic_warning, R.color.ambar_oscuro, R.drawable.bg_badge_ambar, "Stock bajo") { admin?.abrirInventario(true) }
        kpi(view, R.id.kpiPedidosHoy, R.drawable.ic_receipt, R.color.verde, R.drawable.bg_badge_verde, "Pedidos hoy") { admin?.abrirPedidos(null) }
        kpi(view, R.id.kpiProductos, R.drawable.ic_grid, R.color.azul, R.drawable.bg_circulo_azul, "Productos activos") { admin?.irA(R.id.nav_productos) }
        kpi(view, R.id.kpiClientes, R.drawable.ic_people, R.color.texto_sec, R.drawable.bg_badge_gris, "Clientes registrados") { }
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() = cargar()

    private fun kpi(raiz: View, id: Int, icono: Int, color: Int, fondo: Int, etiqueta: String, accion: () -> Unit) {
        val tarjeta = raiz.findViewById<View>(id)
        tarjeta.findViewById<View>(R.id.flKpiIcono).setBackgroundResource(fondo)
        tarjeta.findViewById<ImageView>(R.id.ivKpi).apply {
            setImageResource(icono)
            setColorFilter(ContextCompat.getColor(context, color))
        }
        tarjeta.findViewById<TextView>(R.id.tvKpiEtiqueta).text = etiqueta
        tarjeta.findViewById<TextView>(R.id.tvKpiValor).text = "—"
        tarjeta.setOnClickListener { accion() }
    }

    private fun valor(raiz: View, id: Int, texto: String) {
        raiz.findViewById<View>(id).findViewById<TextView>(R.id.tvKpiValor).text = texto
    }

    private fun cargar() {
        if (view == null) return
        refrescar.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val r = api { panel() }
            refrescar.isRefreshing = false
            when (r) {
                is Resultado.Ok -> pintar(r.dato)
                is Resultado.Error -> requireContext().toast(r.mensaje)
            }
        }
    }

    private fun pintar(p: ResumenPanel) {
        val raiz = view ?: return
        raiz.findViewById<TextView>(R.id.tvPanelVentasHoy).text = p.ventasHoy.mxn()
        raiz.findViewById<TextView>(R.id.tvPanelVentasMes).text = "Acumulado del mes: ${p.ventasMes.mxn()}"
        valor(raiz, R.id.kpiPorEnviar, p.pendientesEnvio.toString())
        valor(raiz, R.id.kpiEnsamble, p.enEnsamble.toString())
        valor(raiz, R.id.kpiStockBajo, p.productosStockBajo.toString())
        valor(raiz, R.id.kpiPedidosHoy, p.pedidosHoy.toString())
        valor(raiz, R.id.kpiProductos, p.productosActivos.toString())
        valor(raiz, R.id.kpiClientes, p.clientes.toString())

        val lista = raiz.findViewById<LinearLayout>(R.id.llUltimosPedidos)
        lista.removeAllViews()
        if (p.ultimosPedidos.isEmpty()) {
            lista.addView(TextView(requireContext()).apply {
                text = "Aún no hay pedidos registrados."
                setTextColor(ContextCompat.getColor(context, R.color.texto_ter))
                textSize = 13f
            })
        }
        for (pedido in p.ultimosPedidos) {
            val fila = lista.inflar(R.layout.item_pedido)
            PedidoAdapter.enlazar(fila, pedido, true) { PedidoDetalleActivity.abrir(requireContext(), it.id, admin = true) }
            lista.addView(fila)
        }
    }
}
