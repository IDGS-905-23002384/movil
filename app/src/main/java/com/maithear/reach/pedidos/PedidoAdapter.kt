package com.maithear.reach.pedidos

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maithear.reach.R
import com.maithear.reach.core.Estados
import com.maithear.reach.core.Fechas
import com.maithear.reach.core.PedidoResumen
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mxn
import com.maithear.reach.core.visible

class PedidoAdapter(
    private val mostrarCliente: Boolean = false,
    private val alTocar: (PedidoResumen) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.VH>() {

    private val items = mutableListOf<PedidoResumen>()

    class VH(v: View) : RecyclerView.ViewHolder(v)

    fun establecer(lista: List<PedidoResumen>) {
        items.clear()
        items.addAll(lista)
        notifyDataSetChanged()
    }

    fun agregar(lista: List<PedidoResumen>) {
        val inicio = items.size
        items.addAll(lista)
        notifyItemRangeInserted(inicio, lista.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_pedido))

    override fun onBindViewHolder(holder: VH, position: Int) {
        enlazar(holder.itemView, items[position], mostrarCliente, alTocar)
    }

    override fun getItemCount(): Int = items.size

    companion object {
        fun enlazar(v: View, p: PedidoResumen, mostrarCliente: Boolean, alTocar: (PedidoResumen) -> Unit) {
            v.findViewById<ImageView>(R.id.ivPedido).cargarImagen(p.imagenUrl)
            v.findViewById<TextView>(R.id.tvPedidoFolio).text = p.folio
            Estados.aplicar(v.findViewById(R.id.tvPedidoEstado), Estados.principal(p))
            v.findViewById<TextView>(R.id.tvPedidoCliente).apply {
                text = p.cliente.orEmpty()
                visible(mostrarCliente && !p.cliente.isNullOrBlank())
            }
            v.findViewById<TextView>(R.id.tvPedidoFecha).text =
                Fechas.completa(p.fecha) + if (p.incluyeEnsamble) " · Con ensamble" else ""
            v.findViewById<TextView>(R.id.tvPedidoArticulos).text = if (p.articulos == 1) "1 artículo" else "${p.articulos} artículos"
            v.findViewById<TextView>(R.id.tvPedidoTotal).text = p.total.mxn()
            v.setOnClickListener { alTocar(p) }
        }
    }
}
