package com.maithear.reach.tienda

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maithear.reach.R
import com.maithear.reach.core.Categoria
import com.maithear.reach.core.Prearmado
import com.maithear.reach.core.ProductoLista
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.dp
import com.maithear.reach.core.existencias
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mxn
import com.maithear.reach.core.precioAnterior
import com.maithear.reach.core.visible

class ProductoAdapter(
    private val carrusel: Boolean = false,
    private val alAgregar: ((ProductoLista) -> Unit)? = null,
    private val alTocar: (ProductoLista) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.VH>() {

    private val items = mutableListOf<ProductoLista>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.ivProducto)
        val badge: TextView = v.findViewById(R.id.tvBadge)
        val inactivo: TextView = v.findViewById(R.id.tvInactivo)
        val marca: TextView = v.findViewById(R.id.tvMarca)
        val nombre: TextView = v.findViewById(R.id.tvNombre)
        val stock: TextView = v.findViewById(R.id.tvStock)
        val anterior: TextView = v.findViewById(R.id.tvPrecioAnterior)
        val precio: TextView = v.findViewById(R.id.tvPrecio)
        val agregar: ImageButton = v.findViewById(R.id.btnAgregar)
    }

    fun establecer(lista: List<ProductoLista>) {
        items.clear()
        items.addAll(lista)
        notifyDataSetChanged()
    }

    fun agregar(lista: List<ProductoLista>) {
        val inicio = items.size
        items.addAll(lista)
        notifyItemRangeInserted(inicio, lista.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vista = parent.inflar(R.layout.item_producto)
        if (carrusel) vista.layoutParams = vista.layoutParams.apply { width = parent.context.dp(164) }
        return VH(vista)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.imagen.cargarImagen(p.imagenUrl)
        holder.marca.text = p.marca
        holder.nombre.text = p.nombre
        holder.precio.text = p.precio.mxn()
        holder.anterior.precioAnterior(p.precioAnterior, p.precio)
        holder.badge.text = "-${p.descuentoPorcentaje}%"
        holder.badge.visible(p.descuentoPorcentaje > 0)
        holder.inactivo.visible(!p.activo)
        holder.stock.existencias(p.stock)
        holder.agregar.visible(alAgregar != null && p.stock > 0 && p.activo)
        holder.agregar.setOnClickListener { alAgregar?.invoke(p) }
        holder.itemView.setOnClickListener { alTocar(p) }
    }

    override fun getItemCount(): Int = items.size
}

class CategoriaAdapter(private val alTocar: (Categoria) -> Unit) : RecyclerView.Adapter<CategoriaAdapter.VH>() {

    private val items = mutableListOf<Categoria>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.ivCategoria)
        val nombre: TextView = v.findViewById(R.id.tvCategoria)
    }

    fun establecer(lista: List<Categoria>) {
        items.clear()
        items.addAll(lista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_categoria))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.nombre.text = c.nombre
        holder.imagen.cargarImagen(c.imagenUrl)
        holder.itemView.setOnClickListener { alTocar(c) }
    }

    override fun getItemCount(): Int = items.size
}

class PrearmadoAdapter(private val alTocar: (Prearmado) -> Unit) : RecyclerView.Adapter<PrearmadoAdapter.VH>() {

    private val items = mutableListOf<Prearmado>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.ivPrearmado)
        val uso: TextView = v.findViewById(R.id.tvPrearmadoUso)
        val nombre: TextView = v.findViewById(R.id.tvPrearmadoNombre)
        val descripcion: TextView = v.findViewById(R.id.tvPrearmadoDescripcion)
        val precio: TextView = v.findViewById(R.id.tvPrearmadoPrecio)
    }

    fun establecer(lista: List<Prearmado>) {
        items.clear()
        items.addAll(lista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_prearmado))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.imagen.cargarImagen(p.imagenUrl)
        holder.uso.text = p.categoriaUso
        holder.nombre.text = p.nombre
        holder.descripcion.text = p.descripcion
        holder.precio.text = p.precioTotal.mxn()
        holder.itemView.alpha = if (p.disponible) 1f else 0.6f
        holder.itemView.setOnClickListener { alTocar(p) }
    }

    override fun getItemCount(): Int = items.size
}
