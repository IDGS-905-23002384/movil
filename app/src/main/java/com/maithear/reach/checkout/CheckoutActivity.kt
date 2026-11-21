package com.maithear.reach.checkout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Carrito
import com.maithear.reach.core.Checkout
import com.maithear.reach.core.Direccion
import com.maithear.reach.core.MetodoEnvio
import com.maithear.reach.core.Pago
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.api
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mxn
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import com.maithear.reach.core.visible
import com.maithear.reach.cuenta.DireccionesActivity
import com.maithear.reach.pedidos.PedidoDetalleActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar

class CheckoutActivity : BaseActivity() {

    private var carritoActual: Carrito? = null
    private var listaMetodos: List<MetodoEnvio> = emptyList()
    private var listaDirecciones: List<Direccion> = emptyList()
    private var metodoId: Int? = null
    private var direccionId: Int? = null

    private lateinit var etTitular: EditText
    private lateinit var etTarjeta: EditText
    private lateinit var etVencimiento: EditText
    private lateinit var etCvv: EditText
    private lateinit var btnPagar: MaterialButton

    private val seleccionDireccion = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val id = r.data?.getIntExtra(DireccionesActivity.EXTRA_DIRECCION_ID, 0) ?: 0
            if (id > 0) direccionId = id
        }
        recargarDirecciones()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        configurarHeader("Finalizar compra", "Pago seguro")
        findViewById<View>(R.id.barraCheckout).insetsSistema(abajo = true)

        etTitular = findViewById(R.id.etTitular)
        etTarjeta = findViewById(R.id.etTarjeta)
        etVencimiento = findViewById(R.id.etVencimiento)
        etCvv = findViewById(R.id.etCvv)
        btnPagar = findViewById(R.id.btnPagar)
        btnPagar.isEnabled = false

        configurarTarjeta()
        findViewById<MaterialButton>(R.id.btnCambiarDireccion).setOnClickListener {
            seleccionDireccion.launch(
                Intent(this, DireccionesActivity::class.java).putExtra(DireccionesActivity.EXTRA_SELECCION, true)
            )
        }
        btnPagar.setOnClickListener { pagar() }
        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            val rCarrito = api { carrito() }
            if (rCarrito is Resultado.Error) {
                toast(rCarrito.mensaje)
                finish()
                return@launch
            }
            val c = (rCarrito as Resultado.Ok).dato
            if (c.items.isEmpty()) {
                toast("Tu carrito está vacío")
                finish()
                return@launch
            }
            carritoActual = c
            val rMetodos = async { api { metodosEnvio(c.subtotalProductos) } }
            val rDirecciones = async { api { direcciones() } }
            when (val m = rMetodos.await()) {
                is Resultado.Ok -> listaMetodos = m.dato
                is Resultado.Error -> toast(m.mensaje)
            }
            when (val d = rDirecciones.await()) {
                is Resultado.Ok -> listaDirecciones = d.dato
                is Resultado.Error -> toast(d.mensaje)
            }
            metodoId = listaMetodos.firstOrNull()?.id
            direccionId = listaDirecciones.firstOrNull { it.esPredeterminada }?.id ?: listaDirecciones.firstOrNull()?.id
            findViewById<TextView>(R.id.tvCheckoutArticulos).text =
                if (c.totalArticulos == 1) "1 artículo" else "${c.totalArticulos} artículos"
            pintarMetodos()
            pintarDireccion()
            pintarTotales()
            btnPagar.isEnabled = true
        }
    }

    private fun recargarDirecciones() {
        lifecycleScope.launch {
            val d = api { direcciones() }
            if (d is Resultado.Ok) {
                listaDirecciones = d.dato
                if (listaDirecciones.none { it.id == direccionId }) {
                    direccionId = listaDirecciones.firstOrNull { it.esPredeterminada }?.id ?: listaDirecciones.firstOrNull()?.id
                }
                pintarDireccion()
            }
        }
    }

    private val metodoActual: MetodoEnvio?
        get() = listaMetodos.firstOrNull { it.id == metodoId }

    private fun pintarMetodos() {
        val contenedor = findViewById<LinearLayout>(R.id.llMetodosEnvio)
        contenedor.removeAllViews()
        for (m in listaMetodos) {
            val fila = contenedor.inflar(R.layout.item_metodo_envio)
            val seleccionado = m.id == metodoId
            fila.setBackgroundResource(if (seleccionado) R.drawable.bg_card_seleccionada else R.drawable.bg_card_borde)
            fila.findViewById<ImageView>(R.id.ivMetodoRadio).setColorFilter(
                ContextCompat.getColor(this, if (seleccionado) R.color.azul else R.color.borde)
            )
            fila.findViewById<TextView>(R.id.tvMetodoNombre).text = m.nombre
            val plazo = when {
                !m.requiereDireccion -> "Listo para recoger en 24 h"
                m.diasMin == m.diasMax -> "Llega en ${m.diasMax} días hábiles"
                else -> "Llega en ${m.diasMin} a ${m.diasMax} días hábiles"
            }
            fila.findViewById<TextView>(R.id.tvMetodoDescripcion).text = "$plazo · ${m.paqueteria}"
            fila.findViewById<TextView>(R.id.tvMetodoCosto).apply {
                text = if (m.esGratis) "Gratis" else m.costoAplicado.mxn()
                setTextColor(ContextCompat.getColor(context, if (m.esGratis) R.color.verde else R.color.texto))
            }
            fila.setOnClickListener {
                metodoId = m.id
                pintarMetodos()
                pintarDireccion()
                pintarTotales()
            }
            contenedor.addView(fila)
        }
    }

    private fun pintarDireccion() {
        val requiere = metodoActual?.requiereDireccion ?: true
        findViewById<View>(R.id.cardDireccion).visible(requiere)
        findViewById<TextView>(R.id.tvPasoPago).text = if (requiere) "Paso 3" else "Paso 2"
        val boton = findViewById<MaterialButton>(R.id.btnCambiarDireccion)
        val tvNombre = findViewById<TextView>(R.id.tvDireccionNombre)
        val tvTexto = findViewById<TextView>(R.id.tvDireccionTexto)
        val d = listaDirecciones.firstOrNull { it.id == direccionId }
        if (d == null) {
            tvNombre.text = "Agrega una dirección de envío"
            tvTexto.text = "La necesitamos para enviarte tu pedido."
            boton.text = "Agregar"
        } else {
            tvNombre.text = "${d.alias} · ${d.nombreRecibe}"
            val interior = if (d.numeroInterior.isNullOrBlank()) "" else " Int. ${d.numeroInterior}"
            tvTexto.text = "${d.calle} ${d.numeroExterior}$interior, ${d.colonia}\n${d.ciudad}, ${d.estado}, C.P. ${d.codigoPostal}\nTel. ${d.telefono}"
            boton.text = "Cambiar"
        }
    }

    private fun totalAPagar(): Double {
        val c = carritoActual ?: return 0.0
        return c.total + (metodoActual?.costoAplicado ?: 0.0)
    }

    private fun pintarTotales() {
        val c = carritoActual ?: return
        val contenedor = findViewById<LinearLayout>(R.id.llTotalesCheckout)
        contenedor.removeAllViews()
        contenedor.agregarFilaTotal("Productos", c.subtotalProductos.mxn())
        if (c.costoEnsamble > 0) contenedor.agregarFilaTotal("Ensamble y pruebas", c.costoEnsamble.mxn())
        val m = metodoActual
        if (m != null) {
            contenedor.agregarFilaTotal("Envío", if (m.esGratis) "Gratis" else m.costoAplicado.mxn(), colorValor = if (m.esGratis) R.color.verde else null)
        }
        val total = totalAPagar()
        contenedor.agregarFilaTotal("IVA incluido", (total - total / 1.16).mxn())
        contenedor.agregarFilaTotal("Total", total.mxn(), destacado = true)
        findViewById<TextView>(R.id.tvTotalPagar).text = total.mxn()
        btnPagar.text = "Pagar"
    }

    private fun configurarTarjeta() {
        val tvMarca = findViewById<TextView>(R.id.tvMarcaTarjeta)
        etTarjeta.addTextChangedListener(object : TextWatcher {
            private var editando = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (editando || s == null) return
                editando = true
                val digitos = s.filter { it.isDigit() }.take(16)
                val formateado = digitos.chunked(4).joinToString(" ")
                if (formateado != s.toString()) {
                    s.replace(0, s.length, formateado)
                }
                tvMarca.text = when {
                    digitos.startsWith("4") -> "VISA"
                    digitos.startsWith("34") || digitos.startsWith("37") -> "AMEX"
                    digitos.startsWith("5") || digitos.startsWith("2") -> "MASTERCARD"
                    else -> ""
                }
                editando = false
            }
        })
        etVencimiento.addTextChangedListener(object : TextWatcher {
            private var editando = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (editando || s == null) return
                editando = true
                val digitos = s.filter { it.isDigit() }.take(4)
                val formateado = if (digitos.length > 2) "${digitos.substring(0, 2)}/${digitos.substring(2)}" else digitos.toString()
                if (formateado != s.toString()) s.replace(0, s.length, formateado)
                editando = false
            }
        })
    }

    private fun validarPago(): Pago? {
        val titular = etTitular.valor()
        val numero = etTarjeta.text.filter { it.isDigit() }.toString()
        val vencimiento = etVencimiento.valor()
        val cvv = etCvv.valor()
        if (titular.length < 3) {
            etTitular.error = "Escribe el nombre del titular"
            return null
        }
        if (numero.length !in 15..16 || !luhn(numero)) {
            etTarjeta.error = "Número de tarjeta no válido"
            return null
        }
        val partes = vencimiento.split("/")
        val mes = partes.getOrNull(0)?.toIntOrNull()
        val anio = partes.getOrNull(1)?.toIntOrNull()
        if (mes == null || anio == null || mes !in 1..12) {
            etVencimiento.error = "Usa MM/AA"
            return null
        }
        val hoy = Calendar.getInstance()
        val anioCompleto = 2000 + anio
        if (anioCompleto < hoy.get(Calendar.YEAR) || (anioCompleto == hoy.get(Calendar.YEAR) && mes < hoy.get(Calendar.MONTH) + 1)) {
            etVencimiento.error = "La tarjeta está vencida"
            return null
        }
        if (cvv.length !in 3..4) {
            etCvv.error = "CVV no válido"
            return null
        }
        return Pago(titular, numero, vencimiento, cvv)
    }

    private fun luhn(numero: String): Boolean {
        var suma = 0
        var doble = false
        for (i in numero.length - 1 downTo 0) {
            var d = numero[i] - '0'
            if (doble) {
                d *= 2
                if (d > 9) d -= 9
            }
            suma += d
            doble = !doble
        }
        return suma % 10 == 0
    }

    private fun pagar() {
        val metodo = metodoActual
        if (metodo == null) {
            toast("Selecciona un método de envío")
            return
        }
        if (metodo.requiereDireccion && direccionId == null) {
            toast("Agrega una dirección de envío")
            return
        }
        val pago = validarPago() ?: return
        btnPagar.ocultarTeclado()
        btnPagar.isEnabled = false
        btnPagar.text = "Procesando…"
        val notas = findViewById<EditText>(R.id.etNotas).valor().ifBlank { null }
        lifecycleScope.launch {
            val r = api { crearPedido(Checkout(if (metodo.requiereDireccion) direccionId else null, metodo.id, pago, notas)) }
            when (r) {
                is Resultado.Ok -> {
                    PedidoDetalleActivity.abrir(this@CheckoutActivity, r.dato.id, nuevo = true)
                    finish()
                }
                is Resultado.Error -> {
                    toast(r.mensaje)
                    btnPagar.isEnabled = true
                    btnPagar.text = "Pagar"
                }
            }
        }
    }
}
