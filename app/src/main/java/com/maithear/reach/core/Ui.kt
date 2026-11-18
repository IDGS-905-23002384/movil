package com.maithear.reach.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.maithear.reach.R
import com.maithear.reach.auth.MainActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val LOCALE_MX: Locale = Locale.Builder().setLanguage("es").setRegion("MX").build()
private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_MX)

fun Double.mxn(): String = formatoMoneda.format(this)

fun Context.toast(mensaje: String) {
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
}

fun View.visible(mostrar: Boolean) {
    visibility = if (mostrar) View.VISIBLE else View.GONE
}

fun EditText.valor(): String = text.toString().trim()

fun View.ocultarTeclado() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun ImageView.cargarImagen(ruta: String?) {
    Glide.with(this)
        .load(ApiClient.urlImagen(ruta))
        .placeholder(R.drawable.bg_imagen)
        .error(R.drawable.bg_imagen)
        .fitCenter()
        .into(this)
}

fun TextView.precioAnterior(valor: Double?, actual: Double) {
    if (valor == null || valor <= actual) {
        visibility = View.GONE
    } else {
        text = valor.mxn()
        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        visibility = View.VISIBLE
    }
}

fun TextView.existencias(stock: Int) {
    when {
        stock <= 0 -> {
            text = "Agotado"
            setTextColor(ContextCompat.getColor(context, R.color.rojo))
        }
        stock <= 5 -> {
            text = "Últimas $stock piezas"
            setTextColor(ContextCompat.getColor(context, R.color.ambar_oscuro))
        }
        else -> {
            text = "$stock disponibles"
            setTextColor(ContextCompat.getColor(context, R.color.verde))
        }
    }
}

fun View.insetsSistema(arriba: Boolean = false, abajo: Boolean = false) {
    val inicialArriba = paddingTop
    val inicialAbajo = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(
            v.paddingLeft,
            if (arriba) inicialArriba + barras.top else v.paddingTop,
            v.paddingRight,
            if (abajo) inicialAbajo + maxOf(barras.bottom, teclado.bottom) else v.paddingBottom
        )
        insets
    }
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}

fun iniciales(nombre: String?): String {
    if (nombre.isNullOrBlank()) return "R"
    return nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

fun saludo(): String {
    val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hora) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

fun Activity.requiereSesion(): Boolean {
    if (Sesion.activa) return true
    startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_RETORNO, true))
    toast("Inicia sesión para continuar")
    return false
}

object Fechas {
    private fun parser(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(iso: String?): Date? {
        if (iso.isNullOrBlank() || iso.length < 19) return null
        return try {
            parser().parse(iso.substring(0, 19))
        } catch (e: Exception) {
            null
        }
    }

    fun milis(iso: String?): Long? = parse(iso)?.time

    fun corta(iso: String?): String = parse(iso)?.let { SimpleDateFormat("d MMM yyyy", LOCALE_MX).format(it) } ?: ""

    fun completa(iso: String?): String = parse(iso)?.let { SimpleDateFormat("d MMM yyyy, HH:mm", LOCALE_MX).format(it) } ?: ""

    fun hora(iso: String?): String = parse(iso)?.let { SimpleDateFormat("d MMM, HH:mm", LOCALE_MX).format(it) } ?: ""
}

object Estados {
    fun aplicar(vista: TextView, estado: String) {
        val (fondo, color) = when (estado) {
            "Completado", "Entregado", "Terminado" -> R.drawable.bg_badge_verde to R.color.verde_oscuro
            "Cancelado" -> R.drawable.bg_badge_rojo to R.color.rojo_oscuro
            "Enviado" -> R.drawable.bg_badge_morado to R.color.morado
            "Ensamblando", "Atendido", "Preparando paquete" -> R.drawable.bg_badge_ambar to R.color.ambar_oscuro
            "Pagado" -> R.drawable.bg_badge_azul_claro to R.color.azul
            else -> R.drawable.bg_badge_gris to R.color.texto_sec
        }
        vista.text = estado
        vista.setBackgroundResource(fondo)
        vista.setTextColor(ContextCompat.getColor(vista.context, color))
    }

    fun principal(pedido: PedidoResumen): String = when {
        pedido.estado == "Cancelado" -> "Cancelado"
        pedido.estadoEnvio == "Entregado" -> "Entregado"
        pedido.estadoEnvio == "Enviado" -> "Enviado"
        pedido.incluyeEnsamble && pedido.estadoArmado != "Terminado" -> if (pedido.estadoArmado == "Ensamblando") "Ensamblando" else "Atendido"
        else -> "Preparando paquete"
    }
}
