package com.maithear.reach.tienda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.ProductoDetalle
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.existencias
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mxn
import com.maithear.reach.core.precioAnterior
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.launch

class ProductoDetalleActivity : BaseActivity() {

    companion object {
        private const val EXTRA_ID = "producto_id"

        fun abrir(context: Context, id: Int) {
            context.startActivity(Intent(context, ProductoDetalleActivity::class.java).putExtra(EXTRA_ID, id))
        }
    }

    private var producto: ProductoDetalle? = null
    private var cantidad = 1
    private lateinit var tvCantidad: TextView
    private lateinit var btnAgregar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_producto_detalle)
        configurarHeader("Detalle del producto")
        accionHeader(R.drawable.ic_cart, "Ver carrito") { AccionesCarrito.abrirCarrito(this) }
        findViewById<View>(R.id.barraDetalle).insetsSistema(abajo = true)

        tvCantidad = findViewById(R.id.tvCantidad)
        btnAgregar = findViewById(R.id.btnAgregarCarrito)

        findViewById<ImageButton>(R.id.btnMenos).setOnClickListener { cambiarCantidad(-1) }
        findViewById<ImageButton>(R.id.btnMas).setOnClickListener { cambiarCantidad(1) }
        btnAgregar.setOnClickListener { agregar() }

        cargar(intent.getIntExtra(EXTRA_ID, 0))
    }

    private fun cargar(id: Int) {
        lifecycleScope.launch {
            api { producto(id) }
                .siOk { pintar(it) }
                .siError {
                    toast(it.mensaje)
                    finish()
                }
            findViewById<View>(R.id.progresoDetalle).visible(false)
        }
    }

    private fun pintar(p: ProductoDetalle) {
        producto = p
        findViewById<View>(R.id.scrollDetalle).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvHeaderTitulo).text = p.categoriaNombre
        findViewById<ImageView>(R.id.ivDetalle).cargarImagen(p.imagenUrl)
        findViewById<TextView>(R.id.tvDetalleMarca).text = p.marca
        findViewById<TextView>(R.id.tvDetalleNombre).text = p.nombre
        findViewById<TextView>(R.id.tvDetalleSku).text = "SKU: ${p.sku}"
        findViewById<TextView>(R.id.tvDetallePrecio).text = p.precio.mxn()
        findViewById<TextView>(R.id.tvDetallePrecioAnterior).precioAnterior(p.precioAnterior, p.precio)
        findViewById<TextView>(R.id.tvDetalleDescuento).apply {
            text = "-${p.descuentoPorcentaje}%"
            visible(p.descuentoPorcentaje > 0)
        }
        findViewById<TextView>(R.id.tvDetalleStock).existencias(p.stock)
        findViewById<TextView>(R.id.tvDetalleEnvio).text = when {
            !p.envio -> "Disponible solo para recoger en tienda"
            p.precio >= 1999 -> "Envío gratis a todo México"
            else -> "Envío gratis en compras desde $1,999"
        }
        findViewById<TextView>(R.id.tvDetalleDescripcion).text = p.descripcion.orEmpty().ifBlank { "Sin descripción." }

        val contenedor = findViewById<LinearLayout>(R.id.llEspecificaciones)
        contenedor.removeAllViews()
        val lineas = p.especificaciones.orEmpty()
        lineas.forEachIndexed { i, linea ->
            val fila = contenedor.inflar(R.layout.item_spec)
            fila.findViewById<TextView>(R.id.tvSpecNombre).text = linea.nombre
            fila.findViewById<TextView>(R.id.tvSpecValor).text = linea.valor
            if (i % 2 == 0) fila.setBackgroundColor(ContextCompat.getColor(this, R.color.input))
            contenedor.addView(fila)
        }
        findViewById<View>(R.id.cardEspecificaciones).visible(lineas.isNotEmpty())

        val disponible = p.stock > 0 && p.activo
        btnAgregar.isEnabled = disponible
        btnAgregar.text = if (disponible) "Agregar al carrito" else "Agotado"
        findViewById<View>(R.id.barraDetalle).visible(!Sesion.esPersonal)
        cantidad = 1
        tvCantidad.text = "1"
    }

    private fun cambiarCantidad(delta: Int) {
        val p = producto ?: return
        val maximo = minOf(p.stock, 20).coerceAtLeast(1)
        cantidad = (cantidad + delta).coerceIn(1, maximo)
        tvCantidad.text = cantidad.toString()
        if (delta > 0 && cantidad == maximo && p.stock <= 20) toast("Solo hay ${p.stock} disponibles")
    }

    private fun agregar() {
        val p = producto ?: return
        btnAgregar.isEnabled = false
        lifecycleScope.launch {
            AccionesCarrito.agregar(this@ProductoDetalleActivity, p.id, cantidad, p.nombre)
            btnAgregar.isEnabled = p.stock > 0
        }
    }
}
