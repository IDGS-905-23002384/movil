package com.maithear.reach.carrito

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maithear.reach.R
import com.maithear.reach.core.CarritoItem
import com.maithear.reach.core.TipoAviso
import com.maithear.reach.core.agregarAviso
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mxn

sealed class FilaCarrito {
    data class Aviso(val texto: String) : FilaCarrito()
    data class Ensamble(val id: String, val nombre: String, val piezas: List<CarritoItem>, val costoEnsamble: Double) : FilaCarrito()
    data class Item(val item: CarritoItem) : FilaCarrito()
}

class CarritoAdapter(
    private val alCambiarCantidad: (CarritoItem, Int) -> Unit,
    private val alEliminar: (CarritoItem) -> Unit,
    private val alEliminarEnsamble: (String, String) -> Unit,
    private val alTocarProducto: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TIPO_AVISO = 0
        const val TIPO_ENSAMBLE = 1
        const val TIPO_ITEM = 2
    }

    private var filas: List<FilaCarrito> = emptyList()

    fun establecer(lista: List<FilaCarrito>) {
        filas = lista
        notifyDataSetChanged()
    }

    class AvisoVH(v: View) : RecyclerView.ViewHolder(v)

    class EnsambleVH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvEnsambleNombre)
        val detalle: TextView = v.findViewById(R.id.tvEnsambleDetalle)
        val piezas: LinearLayout = v.findViewById(R.id.llEnsamblePiezas)
        val total: TextView = v.findViewById(R.id.tvEnsambleTotal)
        val eliminar: ImageButton = v.findViewById(R.id.btnEnsambleEliminar)
    }

    class ItemVH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.ivCarrito)
        val marca: TextView = v.findViewById(R.id.tvCarritoMarca)
        val nombre: TextView = v.findViewById(R.id.tvCarritoNombre)
        val precio: TextView = v.findViewById(R.id.tvCarritoPrecio)
        val cantidad: TextView = v.findViewById(R.id.tvCarritoCantidad)
        val subtotal: TextView = v.findViewById(R.id.tvCarritoSubtotal)
        val menos: ImageButton = v.findViewById(R.id.btnCarritoMenos)
        val mas: ImageButton = v.findViewById(R.id.btnCarritoMas)
        val eliminar: ImageButton = v.findViewById(R.id.btnCarritoEliminar)
    }

    override fun getItemViewType(position: Int): Int = when (filas[position]) {
        is FilaCarrito.Aviso -> TIPO_AVISO
        is FilaCarrito.Ensamble -> TIPO_ENSAMBLE
        is FilaCarrito.Item -> TIPO_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TIPO_AVISO -> AvisoVH(LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        TIPO_ENSAMBLE -> EnsambleVH(parent.inflar(R.layout.item_ensamble))
        else -> ItemVH(parent.inflar(R.layout.item_carrito))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is FilaCarrito.Aviso -> {
                val contenedor = holder.itemView as LinearLayout
                contenedor.removeAllViews()
                contenedor.agregarAviso(fila.texto, TipoAviso.ALERTA)
            }
            is FilaCarrito.Ensamble -> {
                val h = holder as EnsambleVH
                h.nombre.text = fila.nombre
                h.detalle.text = "${fila.piezas.size} piezas · Ensamble y pruebas ${fila.costoEnsamble.mxn()}"
                h.piezas.removeAllViews()
                for (pieza in fila.piezas) {
                    val renglon = h.piezas.inflar(R.layout.item_pedido_producto)
                    renglon.findViewById<ImageView>(R.id.ivPp).cargarImagen(pieza.imagenUrl)
                    renglon.findViewById<TextView>(R.id.tvPpNombre).text = pieza.nombre
                    renglon.findViewById<TextView>(R.id.tvPpCantidad).text = pieza.marca
                    renglon.findViewById<TextView>(R.id.tvPpSubtotal).text = pieza.subtotal.mxn()
                    renglon.setOnClickListener { alTocarProducto(pieza.productoId) }
                    h.piezas.addView(renglon)
                }
                h.total.text = (fila.piezas.sumOf { it.subtotal } + fila.costoEnsamble).mxn()
                h.eliminar.setOnClickListener { alEliminarEnsamble(fila.id, fila.nombre) }
            }
            is FilaCarrito.Item -> {
                val h = holder as ItemVH
                val item = fila.item
                h.imagen.cargarImagen(item.imagenUrl)
                h.marca.text = item.marca
                h.nombre.text = item.nombre
                h.precio.text = "${item.precio.mxn()} c/u"
                h.cantidad.text = item.cantidad.toString()
                h.subtotal.text = item.subtotal.mxn()
                h.menos.isEnabled = item.cantidad > 1
                h.menos.alpha = if (item.cantidad > 1) 1f else 0.35f
                h.mas.isEnabled = item.cantidad < minOf(item.stock, 20)
                h.mas.alpha = if (h.mas.isEnabled) 1f else 0.35f
                h.menos.setOnClickListener { alCambiarCantidad(item, item.cantidad - 1) }
                h.mas.setOnClickListener { alCambiarCantidad(item, item.cantidad + 1) }
                h.eliminar.setOnClickListener { alEliminar(item) }
                h.itemView.setOnClickListener { alTocarProducto(item.productoId) }
            }
        }
    }

    override fun getItemCount(): Int = filas.size
}
