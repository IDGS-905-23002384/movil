package com.maithear.reach.admin

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.maithear.reach.R
import com.maithear.reach.core.AlertaStock
import com.maithear.reach.core.Campo
import com.maithear.reach.core.Fechas
import com.maithear.reach.core.Movimiento
import com.maithear.reach.core.MovimientoCrear
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.agregarChip
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.dialogoFormulario
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class InventarioFragment : Fragment(), Refrescable {

    private var verAlertas = true
    private var pagina = 1
    private var totalPaginas = 1
    private var cargando = false
    private var trabajo: Job? = null
    private var sincronizando = false

    private lateinit var refrescar: SwipeRefreshLayout
    private lateinit var chips: ChipGroup
    private lateinit var rv: RecyclerView
    private val adAlertas = AlertaAdapter()
    private val adKardex = KardexAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_admin_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerAdminLista).insetsSistema(arriba = true)
        view.findViewById<TextView>(R.id.tvAdminListaEyebrow).text = "Existencias"
        view.findViewById<TextView>(R.id.tvAdminListaTitulo).text = "Inventario"
        view.findViewById<View>(R.id.buscadorAdmin).visible(false)

        refrescar = view.findViewById(R.id.refrescarAdminLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { recargar() }

        chips = view.findViewById(R.id.chipsAdmin)
        sincronizando = true
        chips.agregarChip("Stock bajo", true, verAlertas)
        chips.agregarChip("Kardex de movimientos", false, !verAlertas)
        sincronizando = false
        chips.setOnCheckedStateChangeListener { grupo, ids ->
            if (sincronizando || ids.isEmpty()) return@setOnCheckedStateChangeListener
            verAlertas = grupo.findViewById<Chip>(ids.first())?.tag as? Boolean ?: true
            recargar()
        }

        rv = view.findViewById(R.id.rvAdminLista)
        val manager = LinearLayoutManager(context)
        rv.layoutManager = manager
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!verAlertas && dy > 0 && !cargando && pagina < totalPaginas && manager.findLastVisibleItemPosition() >= adKardex.itemCount - 3) {
                    cargarKardex(pagina + 1)
                }
            }
        })

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabAdmin).apply {
            text = "Movimiento"
            visible(true)
            setOnClickListener { elegirTipo(null) }
        }
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() = recargar()

    fun mostrarAlertas(valor: Boolean) {
        verAlertas = valor
        if (view != null) {
            sincronizando = true
            for (i in 0 until chips.childCount) {
                val chip = chips.getChildAt(i) as? Chip ?: continue
                if (chip.tag == valor) chip.isChecked = true
            }
            sincronizando = false
        }
    }

    private fun recargar() {
        if (view == null) return
        trabajo?.cancel()
        cargando = false
        if (verAlertas) cargarAlertas() else cargarKardex(1)
    }

    private fun cargarAlertas() {
        val raiz = view ?: return
        rv.adapter = adAlertas
        refrescar.isRefreshing = true
        trabajo = viewLifecycleOwner.lifecycleScope.launch {
            val r = api { alertas() }
            refrescar.isRefreshing = false
            when (r) {
                is Resultado.Ok -> {
                    adAlertas.establecer(r.dato)
                    if (r.dato.isEmpty()) {
                        raiz.mostrarVacio(R.drawable.ic_check_circle, "Inventario saludable", "Ningún producto está por debajo de su stock mínimo.")
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar el inventario", r.mensaje, "Reintentar") { recargar() }
            }
        }
    }

    private fun cargarKardex(numero: Int) {
        val raiz = view ?: return
        rv.adapter = adKardex
        cargando = true
        if (numero == 1) refrescar.isRefreshing = true
        trabajo = viewLifecycleOwner.lifecycleScope.launch {
            val r = api { kardex(null, numero, 30) }
            refrescar.isRefreshing = false
            cargando = false
            when (r) {
                is Resultado.Ok -> {
                    pagina = r.dato.pagina
                    totalPaginas = r.dato.totalPaginas
                    if (numero == 1) adKardex.establecer(r.dato.items) else adKardex.agregar(r.dato.items)
                    if (r.dato.total == 0) raiz.mostrarVacio(R.drawable.ic_inventory, "Sin movimientos", "Aún no hay movimientos registrados.") else raiz.ocultarVacio()
                }
                is Resultado.Error -> if (numero == 1) {
                    raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar el kardex", r.mensaje, "Reintentar") { recargar() }
                }
            }
        }
    }

    private fun elegirTipo(alerta: AlertaStock?) {
        if (alerta != null) {
            registrar("Entrada", alerta.sku, alerta.nombre)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar movimiento")
            .setItems(arrayOf("Entrada de mercancía", "Salida por merma o ajuste")) { _, i ->
                registrar(if (i == 0) "Entrada" else "Salida", null, null)
            }
            .show()
    }

    private fun registrar(tipo: String, sku: String?, nombre: String?) {
        val campos = mutableListOf<Campo>()
        if (sku == null) campos.add(Campo("sku", "SKU del producto", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, "Ej. RTX4070-SUP"))
        campos.add(Campo("cantidad", "Cantidad", "", InputType.TYPE_CLASS_NUMBER, "Piezas"))
        campos.add(Campo("motivo", "Motivo", if (tipo == "Entrada") "Reabastecimiento de proveedor" else "", pista = "Ej. Producto dañado"))
        requireContext().dialogoFormulario(
            titulo = if (tipo == "Entrada") "Entrada de mercancía" else "Salida de inventario",
            mensaje = nombre,
            campos = campos,
            textoAceptar = "Registrar"
        ) { valores, dialogo -> guardarMovimiento(tipo, sku ?: valores["sku"].orEmpty(), valores, dialogo) }
    }

    private fun guardarMovimiento(tipo: String, sku: String, valores: Map<String, String>, dialogo: AlertDialog) {
        val cantidad = valores["cantidad"]?.toIntOrNull()
        val motivo = valores["motivo"].orEmpty()
        when {
            sku.isBlank() -> requireContext().toast("Escribe el SKU")
            cantidad == null || cantidad !in 1..10000 -> requireContext().toast("Cantidad no válida")
            motivo.length < 3 -> requireContext().toast("Describe el motivo")
            else -> viewLifecycleOwner.lifecycleScope.launch {
                val busqueda = api { productos(q = sku, incluirInactivos = true, tamano = 10) }
                val producto = (busqueda as? Resultado.Ok)?.dato?.items?.firstOrNull { it.sku.equals(sku, ignoreCase = true) }
                if (producto == null) {
                    requireContext().toast("No existe un producto con el SKU $sku")
                    return@launch
                }
                when (val r = api { registrarMovimiento(MovimientoCrear(producto.id, tipo, cantidad, motivo)) }) {
                    is Resultado.Ok -> {
                        dialogo.dismiss()
                        requireContext().toast("Movimiento registrado. Stock actual: ${r.dato.stockResultante}")
                        recargar()
                    }
                    is Resultado.Error -> requireContext().toast(r.mensaje)
                }
            }
        }
    }

    inner class AlertaAdapter : RecyclerView.Adapter<AlertaAdapter.VH>() {
        private var items: List<AlertaStock> = emptyList()

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        fun establecer(lista: List<AlertaStock>) {
            items = lista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_alerta))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val a = items[position]
            val v = holder.itemView
            v.findViewById<ImageView>(R.id.ivAlerta).cargarImagen(a.imagenUrl)
            v.findViewById<TextView>(R.id.tvAlertaNombre).text = a.nombre
            v.findViewById<TextView>(R.id.tvAlertaSku).text = "${a.sku} · ${a.categoria}"
            v.findViewById<TextView>(R.id.tvAlertaStock).apply {
                text = if (a.stock == 0) "Agotado · mínimo ${a.stockMinimo}" else "${a.stock} en stock · mínimo ${a.stockMinimo}"
                setBackgroundResource(if (a.stock == 0) R.drawable.bg_badge_rojo else R.drawable.bg_badge_ambar)
                setTextColor(ContextCompat.getColor(context, if (a.stock == 0) R.color.rojo_oscuro else R.color.ambar_oscuro))
            }
            v.findViewById<View>(R.id.btnReabastecer).setOnClickListener { elegirTipo(a) }
        }

        override fun getItemCount(): Int = items.size
    }

    inner class KardexAdapter : RecyclerView.Adapter<KardexAdapter.VH>() {
        private val items = mutableListOf<Movimiento>()

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        fun establecer(lista: List<Movimiento>) {
            items.clear()
            items.addAll(lista)
            notifyDataSetChanged()
        }

        fun agregar(lista: List<Movimiento>) {
            val inicio = items.size
            items.addAll(lista)
            notifyItemRangeInserted(inicio, lista.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_movimiento))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            val v = holder.itemView
            val entrada = m.tipoMovimiento == "Entrada"
            val color = ContextCompat.getColor(v.context, if (entrada) R.color.verde else R.color.rojo)
            v.findViewById<TextView>(R.id.tvMovSigno).apply {
                text = if (entrada) "+" else "−"
                setTextColor(color)
                setBackgroundResource(if (entrada) R.drawable.bg_badge_verde else R.drawable.bg_badge_rojo)
            }
            v.findViewById<TextView>(R.id.tvMovProducto).text = m.productoNombre
            v.findViewById<TextView>(R.id.tvMovMotivo).text = m.motivo
            v.findViewById<TextView>(R.id.tvMovMeta).text = "${m.sku} · ${m.responsable} · ${Fechas.hora(m.fecha)}"
            v.findViewById<TextView>(R.id.tvMovCantidad).apply {
                text = (if (entrada) "+" else "−") + m.cantidad
                setTextColor(color)
            }
            v.findViewById<TextView>(R.id.tvMovStock).text = "Stock: ${m.stockResultante}"
        }

        override fun getItemCount(): Int = items.size
    }
}
