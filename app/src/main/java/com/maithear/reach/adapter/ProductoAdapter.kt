package com.maithear.reach.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.maithear.reach.FormProductoActivity
import com.maithear.reach.R
import com.maithear.reach.model.Producto

class ProductoAdapter(private val listaProductos: List<Producto>) :
    RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMarca: TextView = itemView.findViewById(R.id.tvMarca)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val ivProducto: ImageView = itemView.findViewById(R.id.ivProducto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        holder.tvMarca.text = producto.marca ?: "ASUS"
        holder.tvNombre.text = producto.nombre
        holder.tvPrecio.text = "$ ${producto.precio}"

        if (!producto.imagenUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(producto.imagenUrl)
                .placeholder(R.drawable.circuit)
                .into(holder.ivProducto)
        } else {
            holder.ivProducto.setImageResource(R.drawable.circuit)
        }

        // Al hacer clic en la tarjeta se abre detalles
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val dialog = BottomSheetDialog(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_detalle_producto, null)

            val tvNombre = dialogView.findViewById<TextView>(R.id.tvDetalleNombre)
            val tvMarca = dialogView.findViewById<TextView>(R.id.tvDetalleMarca)
            val tvPrecio = dialogView.findViewById<TextView>(R.id.tvDetallePrecio)
            val tvStock = dialogView.findViewById<TextView>(R.id.tvDetalleStock)
            val tvDesc = dialogView.findViewById<TextView>(R.id.tvDetalleDescripcion)
            val ivImagen = dialogView.findViewById<ImageView>(R.id.ivDetalleImagen)
            val btnEditar = dialogView.findViewById<Button>(R.id.btnEditarModal)

            tvNombre.text = producto.nombre
            tvMarca.text = producto.marca ?: "ASUS"
            tvPrecio.text = "$ ${producto.precio}"
            tvStock.text = "Stock: ${producto.stock} disponibles"
            tvDesc.text = producto.descripcion ?: "Sin descripción."

            if (!producto.imagenUrl.isNullOrEmpty()) {
                Glide.with(context).load(producto.imagenUrl).into(ivImagen)
            }

            btnEditar.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(context, FormProductoActivity::class.java).apply {
                    putExtra("PRODUCTO_ID", producto.id)
                    putExtra("PRODUCTO_SKU", producto.sku)
                    putExtra("PRODUCTO_NOMBRE", producto.nombre)
                    putExtra("PRODUCTO_MARCA", producto.marca)
                    putExtra("PRODUCTO_PRECIO", producto.precio)
                    putExtra("PRODUCTO_STOCK", producto.stock)
                    putExtra("PRODUCTO_IMAGEN", producto.imagenUrl)
                    putExtra("PRODUCTO_DESC", producto.descripcion)
                }
                context.startActivity(intent)
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }
    }

    override fun getItemCount(): Int = listaProductos.size
}