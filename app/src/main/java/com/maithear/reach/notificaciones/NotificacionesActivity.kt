package com.maithear.reach.notificaciones

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Fechas
import com.maithear.reach.core.Notificacion
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.exito
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import com.maithear.reach.pedidos.PedidoDetalleActivity
import kotlinx.coroutines.launch

class NotificacionesActivity : BaseActivity() {

    companion object {
        const val EXTRA_NOTIFICACION_ID = "notificacion_id"
        const val EXTRA_PEDIDO_ID = "notificacion_pedido_id"
    }

    private lateinit var refrescar: SwipeRefreshLayout
    private val adaptador = NotificacionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requiereSesion()) {
            finish()
            return
        }
        setContentView(R.layout.activity_lista)
        configurarHeader("Notificaciones", "Avisos")
        accionHeader(R.drawable.ic_check, "Marcar todas como leídas") { marcarTodas() }
        refrescar = findViewById(R.id.refrescarLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }
        findViewById<RecyclerView>(R.id.rvLista).apply {
            layoutManager = LinearLayoutManager(this@NotificacionesActivity)
            adapter = adaptador
        }
        procesarIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (Sesion.activa) cargar()
    }

    private fun procesarIntent(intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIFICACION_ID, 0)
        val pedido = intent.getIntExtra(EXTRA_PEDIDO_ID, 0)
        intent.removeExtra(EXTRA_NOTIFICACION_ID)
        intent.removeExtra(EXTRA_PEDIDO_ID)
        if (id <= 0) return
        lifecycleScope.launch { api { marcarNotificacionLeida(id).exito() } }
        if (pedido > 0) PedidoDetalleActivity.abrir(this, pedido, admin = Sesion.esPersonal)
    }

    private fun cargar() {
        refrescar.isRefreshing = true
        lifecycleScope.launch {
            val r = api { notificaciones() }
            refrescar.isRefreshing = false
            val raiz = findViewById<View>(android.R.id.content)
            when (r) {
                is Resultado.Ok -> {
                    adaptador.establecer(r.dato.items)
                    if (r.dato.items.isEmpty()) {
                        raiz.mostrarVacio(R.drawable.ic_campana, "Sin notificaciones", "Aquí verás cada avance de tus pedidos y los avisos de la tienda.")
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar tus avisos", r.mensaje, "Reintentar") { cargar() }
            }
        }
    }

    private fun marcarTodas() {
        lifecycleScope.launch {
            api { marcarTodasLeidas().exito() }
                .siOk {
                    Notificador.limpiar(this@NotificacionesActivity)
                    toast("Todo al día")
                    cargar()
                }
                .siError { toast(it.mensaje) }
        }
    }

    private fun abrir(n: Notificacion) {
        Notificador.cancelar(this, n.id)
        if (!n.leida) {
            adaptador.marcarLeida(n.id)
            lifecycleScope.launch { api { marcarNotificacionLeida(n.id).exito() } }
        }
        val pedido = n.pedidoId
        if (pedido != null) PedidoDetalleActivity.abrir(this, pedido, admin = Sesion.esPersonal)
    }

    inner class NotificacionAdapter : RecyclerView.Adapter<NotificacionAdapter.VH>() {
        private val items = mutableListOf<Notificacion>()

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        fun establecer(lista: List<Notificacion>) {
            items.clear()
            items.addAll(lista)
            notifyDataSetChanged()
        }

        fun marcarLeida(id: Int) {
            val i = items.indexOfFirst { it.id == id }
            if (i >= 0) {
                items[i] = items[i].copy(leida = true)
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_notificacion))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val n = items[position]
            val v = holder.itemView
            val (icono, color, fondo) = when (n.tipo) {
                "Inventario" -> Triple(R.drawable.ic_warning, R.color.ambar_oscuro, R.drawable.bg_badge_ambar)
                "Sistema" -> Triple(R.drawable.ic_campana, R.color.morado, R.drawable.bg_badge_morado)
                else -> Triple(R.drawable.ic_receipt, R.color.azul, R.drawable.bg_circulo_azul)
            }
            v.findViewById<View>(R.id.flNotifIcono).setBackgroundResource(fondo)
            v.findViewById<ImageView>(R.id.ivNotifIcono).apply {
                setImageResource(icono)
                setColorFilter(ContextCompat.getColor(context, color))
            }
            v.findViewById<TextView>(R.id.tvNotifTitulo).apply {
                text = n.titulo
                setTypeface(null, if (n.leida) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
            }
            v.findViewById<TextView>(R.id.tvNotifMensaje).text = n.mensaje
            v.findViewById<TextView>(R.id.tvNotifFecha).text = Fechas.relativa(n.fecha)
            v.findViewById<View>(R.id.vNotifNoLeida).visible(!n.leida)
            v.alpha = if (n.leida) 0.85f else 1f
            v.setOnClickListener { abrir(n) }
        }

        override fun getItemCount(): Int = items.size
    }
}
