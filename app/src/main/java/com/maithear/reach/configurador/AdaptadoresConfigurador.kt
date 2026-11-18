package com.maithear.reach.configurador

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.maithear.reach.R
import com.maithear.reach.core.OpcionComponente
import com.maithear.reach.core.PasoConfigurador
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mxn
import com.maithear.reach.core.visible

class PasoAdapter(private val alTocar: (Int) -> Unit) : RecyclerView.Adapter<PasoAdapter.VH>() {

    private var pasos: List<PasoConfigurador> = emptyList()
    private var actual = 0

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val numero: TextView = v.findViewById(R.id.tvPasoNumero)
        val nombre: TextView = v.findViewById(R.id.tvPasoNombre)
    }

    fun establecer(lista: List<PasoConfigurador>, indice: Int) {
        pasos = lista
        actual = indice
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_paso))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val paso = pasos[position]
        val ctx = holder.itemView.context
        val completo = ConfiguradorEstado.tieneSeleccion(paso.tipo)
        holder.nombre.text = paso.nombre
        when {
            position == actual -> {
                holder.numero.setBackgroundResource(R.drawable.bg_buscador)
                holder.numero.setTextColor(ContextCompat.getColor(ctx, R.color.azul))
                holder.numero.text = if (completo) "✓" else (position + 1).toString()
                holder.nombre.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            }
            completo -> {
                holder.numero.setBackgroundResource(R.drawable.bg_circulo_verde)
                holder.numero.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                holder.numero.text = "✓"
                holder.nombre.setTextColor(ContextCompat.getColor(ctx, R.color.lavanda))
            }
            else -> {
                holder.numero.setBackgroundResource(R.drawable.bg_avatar)
                holder.numero.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                holder.numero.text = (position + 1).toString()
                holder.nombre.setTextColor(ContextCompat.getColor(ctx, R.color.lavanda))
            }
        }
        holder.itemView.setOnClickListener { alTocar(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = pasos.size
}

class OpcionAdapter(private val alTocar: (OpcionComponente) -> Unit) : RecyclerView.Adapter<OpcionAdapter.VH>() {

    private var opciones: List<OpcionComponente> = emptyList()
    private var tipo: String = ""

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tarjeta: LinearLayout = v.findViewById(R.id.cardOpcion)
        val imagen: ImageView = v.findViewById(R.id.ivOpcion)
        val marca: TextView = v.findViewById(R.id.tvOpcionMarca)
        val nombre: TextView = v.findViewById(R.id.tvOpcionNombre)
        val detalle: TextView = v.findViewById(R.id.tvOpcionDetalle)
        val motivo: TextView = v.findViewById(R.id.tvOpcionMotivo)
        val precio: TextView = v.findViewById(R.id.tvOpcionPrecio)
        val check: ImageView = v.findViewById(R.id.ivOpcionCheck)
    }

    fun establecer(lista: List<OpcionComponente>, tipoPaso: String) {
        opciones = lista
        tipo = tipoPaso
        notifyDataSetChanged()
    }

    fun refrescarSeleccion() = notifyDataSetChanged()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflar(R.layout.item_opcion))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = opciones[position]
        val p = o.producto
        val seleccionado = ConfiguradorEstado.estaSeleccionado(tipo, p.id)
        holder.imagen.cargarImagen(p.imagenUrl)
        holder.marca.text = p.marca
        holder.nombre.text = p.nombre
        holder.detalle.text = o.resumen.orEmpty()
        holder.detalle.visible(!o.resumen.isNullOrBlank())
        holder.precio.text = p.precio.mxn()
        val motivos = o.motivos.orEmpty()
        holder.motivo.text = motivos.joinToString("\n")
        holder.motivo.visible(!o.compatible && motivos.isNotEmpty())
        holder.tarjeta.alpha = if (o.compatible || seleccionado) 1f else 0.55f
        holder.tarjeta.setBackgroundResource(if (seleccionado) R.drawable.bg_card_seleccionada else R.drawable.bg_card_borde)
        holder.check.visibility = if (seleccionado) View.VISIBLE else View.INVISIBLE
        holder.itemView.setOnClickListener { alTocar(o) }
    }

    override fun getItemCount(): Int = opciones.size
}
