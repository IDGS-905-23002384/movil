package com.maithear.reach.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.maithear.reach.R

fun View.mostrarVacio(
    icono: Int,
    titulo: String,
    mensaje: String,
    textoBoton: String? = null,
    accion: (() -> Unit)? = null
) {
    val contenedor = findViewById<View>(R.id.layoutVacio) ?: return
    contenedor.findViewById<ImageView>(R.id.ivVacio).setImageResource(icono)
    contenedor.findViewById<TextView>(R.id.tvVacioTitulo).text = titulo
    contenedor.findViewById<TextView>(R.id.tvVacioMensaje).text = mensaje
    val boton = contenedor.findViewById<MaterialButton>(R.id.btnVacio)
    if (textoBoton != null && accion != null) {
        boton.text = textoBoton
        boton.setOnClickListener { accion() }
        boton.visible(true)
    } else {
        boton.visible(false)
    }
    contenedor.visible(true)
}

fun View.ocultarVacio() {
    findViewById<View>(R.id.layoutVacio)?.visible(false)
}

fun LinearLayout.agregarFilaTotal(etiqueta: String, valor: String, destacado: Boolean = false, colorValor: Int? = null) {
    val fila = LayoutInflater.from(context).inflate(R.layout.item_fila_total, this, false)
    val tvEtiqueta = fila.findViewById<TextView>(R.id.tvFilaEtiqueta)
    val tvValor = fila.findViewById<TextView>(R.id.tvFilaValor)
    tvEtiqueta.text = etiqueta
    tvValor.text = valor
    if (destacado) {
        tvEtiqueta.setTextColor(ContextCompat.getColor(context, R.color.texto))
        tvEtiqueta.textSize = 15f
        tvEtiqueta.setTypeface(tvEtiqueta.typeface, android.graphics.Typeface.BOLD)
        tvValor.textSize = 17f
        tvValor.setTypeface(tvValor.typeface, android.graphics.Typeface.BOLD)
        tvValor.setTextColor(ContextCompat.getColor(context, R.color.azul))
    }
    if (colorValor != null) tvValor.setTextColor(ContextCompat.getColor(context, colorValor))
    addView(fila)
}

enum class TipoAviso { INFO, ALERTA, ERROR, EXITO }

fun LinearLayout.agregarAviso(texto: String, tipo: TipoAviso) {
    val fila = LayoutInflater.from(context).inflate(R.layout.item_aviso, this, false)
    val (fondo, color, icono) = when (tipo) {
        TipoAviso.INFO -> Triple(R.drawable.bg_aviso_azul, R.color.azul, R.drawable.ic_info)
        TipoAviso.ALERTA -> Triple(R.drawable.bg_aviso_ambar, R.color.ambar_oscuro, R.drawable.ic_warning)
        TipoAviso.ERROR -> Triple(R.drawable.bg_aviso_rojo, R.color.rojo_oscuro, R.drawable.ic_error)
        TipoAviso.EXITO -> Triple(R.drawable.bg_aviso_verde, R.color.verde_oscuro, R.drawable.ic_check_circle)
    }
    fila.setBackgroundResource(fondo)
    val tv = fila.findViewById<TextView>(R.id.tvAviso)
    tv.text = texto
    tv.setTextColor(ContextCompat.getColor(context, color))
    val iv = fila.findViewById<ImageView>(R.id.ivAviso)
    iv.setImageResource(icono)
    iv.setColorFilter(ContextCompat.getColor(context, color))
    addView(fila)
}

fun ChipGroup.agregarChip(texto: String, etiqueta: Any?, marcado: Boolean = false): Chip {
    val chip = LayoutInflater.from(context).inflate(R.layout.item_chip, this, false) as Chip
    chip.id = View.generateViewId()
    chip.text = texto
    chip.tag = etiqueta
    chip.isChecked = marcado
    addView(chip)
    return chip
}

fun View.configurarFila(icono: Int, titulo: String, subtitulo: String? = null, peligro: Boolean = false, accion: () -> Unit) {
    findViewById<ImageView>(R.id.ivFilaIcono).apply {
        setImageResource(icono)
        setColorFilter(ContextCompat.getColor(context, if (peligro) R.color.rojo else R.color.azul))
    }
    findViewById<View>(R.id.flFilaIcono).setBackgroundResource(if (peligro) R.drawable.bg_badge_rojo else R.drawable.bg_circulo_azul)
    findViewById<TextView>(R.id.tvFilaTitulo).apply {
        text = titulo
        setTextColor(ContextCompat.getColor(context, if (peligro) R.color.rojo else R.color.texto))
    }
    findViewById<TextView>(R.id.tvFilaSubtitulo).apply {
        text = subtitulo ?: ""
        visible(!subtitulo.isNullOrBlank())
    }
    setOnClickListener { accion() }
}

fun Context.dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

fun ViewGroup.inflar(layout: Int): View = LayoutInflater.from(context).inflate(layout, this, false)
