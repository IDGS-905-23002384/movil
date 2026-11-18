package com.maithear.reach.pedidos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.core.ActualizarArmado
import com.maithear.reach.core.ActualizarEnvio
import com.maithear.reach.core.ApiService
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Campo
import com.maithear.reach.core.Estados
import com.maithear.reach.core.Fechas
import com.maithear.reach.core.PedidoDetalle
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.dialogoFormulario
import com.maithear.reach.core.inflar
import com.maithear.reach.core.mxn
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import kotlinx.coroutines.launch

class PedidoDetalleActivity : BaseActivity() {

    companion object {
        private const val EXTRA_ID = "pedido_id"
        private const val EXTRA_NUEVO = "nuevo"
        private const val EXTRA_ADMIN = "admin"

        fun abrir(context: Context, id: Int, nuevo: Boolean = false, admin: Boolean = false) {
            context.startActivity(
                Intent(context, PedidoDetalleActivity::class.java)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_NUEVO, nuevo)
                    .putExtra(EXTRA_ADMIN, admin)
            )
        }
    }

    private val pedidoId: Int
        get() = intent.getIntExtra(EXTRA_ID, 0)
    private val modoAdmin: Boolean
        get() = intent.getBooleanExtra(EXTRA_ADMIN, false)
    private var mostrarExito = false
    private var ocupado = false
    private lateinit var refrescar: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedido_detalle)
        configurarHeader("Detalle del pedido", if (modoAdmin) "Administración" else "Mis compras")
        mostrarExito = savedInstanceState == null && intent.getBooleanExtra(EXTRA_NUEVO, false)
        refrescar = findViewById(R.id.refrescarPedido)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        refrescar.isRefreshing = true
        lifecycleScope.launch {
            val r = api { if (modoAdmin) pedidoAdmin(pedidoId) else pedido(pedidoId) }
            refrescar.isRefreshing = false
            when (r) {
                is Resultado.Ok -> pintar(r.dato)
                is Resultado.Error -> {
                    toast(r.mensaje)
                    if (findViewById<View>(R.id.contenidoPedido).visibility != View.VISIBLE) finish()
                }
            }
        }
    }

    private fun estadoPrincipal(p: PedidoDetalle): String = when {
        p.estado == "Cancelado" -> "Cancelado"
        p.estadoEnvio == "Entregado" -> "Entregado"
        p.estadoEnvio == "Enviado" -> "Enviado"
        p.incluyeEnsamble && p.estadoArmado != "Terminado" -> p.estadoArmado
        else -> "Preparando paquete"
    }

    private fun pintar(p: PedidoDetalle) {
        findViewById<View>(R.id.contenidoPedido).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvHeaderTitulo).text = p.folio

        findViewById<View>(R.id.cardExito).visible(mostrarExito)
        findViewById<TextView>(R.id.tvExitoMensaje).text = "Tu pedido ${p.folio} fue confirmado. Te avisaremos cada avance."

        findViewById<TextView>(R.id.tvDetFolio).text = "Pedido ${p.folio}"
        findViewById<TextView>(R.id.tvDetFecha).text = "Realizado el ${Fechas.completa(p.fecha)}"
        Estados.aplicar(findViewById(R.id.tvDetEstado), estadoPrincipal(p))
        pintarTracker(p)

        findViewById<TextView>(R.id.tvDetEntrega).text = when {
            p.estado == "Cancelado" -> "Este pedido fue cancelado el ${Fechas.corta(p.fechaActualizacion)}."
            p.estadoEnvio == "Entregado" -> "Entregado el ${Fechas.corta(p.fechaActualizacion)}."
            !p.requiereDireccion -> "Recógelo en tienda a partir del ${Fechas.corta(p.fechaEstimadaEntrega)} presentando tu folio."
            else -> "Entrega estimada: ${Fechas.corta(p.fechaEstimadaEntrega)}"
        }

        val seguimiento = findViewById<LinearLayout>(R.id.llSeguimiento)
        seguimiento.removeAllViews()
        val eventos = p.seguimiento.reversed()
        eventos.forEachIndexed { i, e ->
            val fila = seguimiento.inflar(R.layout.item_seguimiento)
            fila.findViewById<TextView>(R.id.tvSegTitulo).text = e.titulo
            fila.findViewById<TextView>(R.id.tvSegDescripcion).text = e.descripcion
            fila.findViewById<TextView>(R.id.tvSegFecha).text = Fechas.completa(e.fecha)
            fila.findViewById<View>(R.id.vSegPunto).setBackgroundResource(
                if (i == 0) R.drawable.bg_circulo_azul_solido else R.drawable.bg_circulo_gris
            )
            fila.findViewById<View>(R.id.vSegLinea).visibility = if (i == eventos.size - 1) View.INVISIBLE else View.VISIBLE
            seguimiento.addView(fila)
        }

        findViewById<TextView>(R.id.tvDetMetodo).text = p.metodoEnvio + (p.paqueteria?.let { " · $it" } ?: "")
        findViewById<TextView>(R.id.tvDetDireccion).text = if (p.requiereDireccion) {
            listOfNotNull(
                p.envioNombreRecibe,
                p.envioDireccion,
                listOfNotNull(p.envioCiudad, p.envioEstado).joinToString(", ") + (p.envioCodigoPostal?.let { ", C.P. $it" } ?: ""),
                p.envioTelefono?.let { "Tel. $it" },
                p.envioReferencias?.let { "Referencias: $it" }
            ).joinToString("\n")
        } else {
            "Tienda Reach León · Blvd. Adolfo López Mateos 1502, León, Gto.\nRecibe: ${p.envioNombreRecibe.orEmpty()}"
        }
        val llGuia = findViewById<View>(R.id.llGuia)
        llGuia.visible(!p.numeroGuia.isNullOrBlank())
        findViewById<TextView>(R.id.tvDetGuia).text = p.numeroGuia.orEmpty()
        findViewById<View>(R.id.btnCopiarGuia).setOnClickListener {
            val portapapeles = getSystemService(ClipboardManager::class.java)
            portapapeles?.setPrimaryClip(ClipData.newPlainText("Guía", p.numeroGuia.orEmpty()))
            toast("Guía copiada")
        }

        val productos = findViewById<LinearLayout>(R.id.llDetProductos)
        productos.removeAllViews()
        var ensambleActual: String? = null
        for (d in p.detalles) {
            if (d.ensambleId != null && d.ensambleId != ensambleActual) {
                ensambleActual = d.ensambleId
                productos.addView(TextView(this).apply {
                    text = "${d.ensambleNombre ?: "PC personalizada"} · Ensamble incluido"
                    setTextColor(ContextCompat.getColor(context, R.color.azul))
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 12, 0, 4)
                })
            } else if (d.ensambleId == null && ensambleActual != null) {
                ensambleActual = null
                productos.addView(TextView(this).apply {
                    text = "Productos individuales"
                    setTextColor(ContextCompat.getColor(context, R.color.texto_ter))
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 12, 0, 4)
                })
            }
            val fila = productos.inflar(R.layout.item_pedido_producto)
            fila.findViewById<ImageView>(R.id.ivPp).cargarImagen(d.imagenUrl)
            fila.findViewById<TextView>(R.id.tvPpNombre).text = d.nombre
            fila.findViewById<TextView>(R.id.tvPpCantidad).text = "${d.cantidad} x ${d.precioUnitario.mxn()}"
            fila.findViewById<TextView>(R.id.tvPpSubtotal).text = d.subtotal.mxn()
            productos.addView(fila)
        }

        findViewById<TextView>(R.id.tvDetTarjeta).text = "${p.metodoPago}${p.tarjeta?.let { " · $it" } ?: ""}"
        val totales = findViewById<LinearLayout>(R.id.llDetTotales)
        totales.removeAllViews()
        totales.agregarFilaTotal("Productos", p.subtotal.mxn())
        if (p.costoManoObra > 0) totales.agregarFilaTotal("Ensamble y pruebas", p.costoManoObra.mxn())
        totales.agregarFilaTotal("Envío", if (p.costoEnvio > 0) p.costoEnvio.mxn() else "Gratis")
        totales.agregarFilaTotal("IVA incluido", p.iva.mxn())
        totales.agregarFilaTotal("Total", p.total.mxn(), destacado = true)

        val btnCancelar = findViewById<MaterialButton>(R.id.btnCancelarPedido)
        btnCancelar.visible(!modoAdmin && p.puedeCancelar)
        btnCancelar.setOnClickListener {
            confirmar("Cancelar pedido", "Se cancelará el pedido ${p.folio} y se reembolsará el pago. ¿Continuar?", "Cancelar pedido") {
                ejecutar("Pedido cancelado") { cancelarPedido(p.id) }
            }
        }

        pintarAdmin(p)
    }

    private fun pintarTracker(p: PedidoDetalle) {
        val contenedor = findViewById<LinearLayout>(R.id.llTracker)
        contenedor.removeAllViews()
        val pasos = mutableListOf<Pair<String, Int>>()
        pasos.add("Pagado" to R.drawable.ic_check)
        if (p.incluyeEnsamble) pasos.add("Ensamble" to R.drawable.ic_build)
        pasos.add((if (p.requiereDireccion) "Enviado" else "Listo") to R.drawable.ic_truck)
        pasos.add("Entregado" to R.drawable.ic_home)

        var completados = 1
        if (p.incluyeEnsamble && p.estadoArmado == "Terminado") completados++
        if (p.estadoEnvio == "Enviado" || p.estadoEnvio == "Entregado") completados = pasos.size - 1
        if (p.estadoEnvio == "Entregado") completados = pasos.size
        val cancelado = p.estado == "Cancelado"

        pasos.forEachIndexed { i, (nombre, icono) ->
            val vista = contenedor.inflar(R.layout.item_paso_tracker)
            val hecho = !cancelado && i < completados
            val actual = !cancelado && i == completados
            vista.findViewById<FrameLayout>(R.id.flTracker).setBackgroundResource(
                when {
                    hecho -> R.drawable.bg_circulo_verde
                    actual -> R.drawable.bg_circulo_azul_solido
                    else -> R.drawable.bg_circulo_gris
                }
            )
            vista.findViewById<ImageView>(R.id.ivTracker).setImageResource(icono)
            vista.findViewById<TextView>(R.id.tvTracker).apply {
                text = nombre
                setTextColor(ContextCompat.getColor(context, if (hecho || actual) R.color.texto else R.color.gris))
            }
            contenedor.addView(vista)
        }
    }

    private fun pintarAdmin(p: PedidoDetalle) {
        val tarjeta = findViewById<View>(R.id.cardAdmin)
        tarjeta.visible(modoAdmin)
        if (!modoAdmin) return
        findViewById<TextView>(R.id.tvAdminCliente).text =
            "Cliente: ${p.cliente} · ${p.clienteEmail}" + (p.notas?.let { "\nNotas: $it" } ?: "")
        val activo = p.estado != "Cancelado"

        val btnArmado = findViewById<MaterialButton>(R.id.btnAvanzarArmado)
        val siguienteArmado = when (p.estadoArmado) {
            "Atendido" -> "Ensamblando"
            "Ensamblando" -> "Terminado"
            else -> null
        }
        btnArmado.visible(activo && p.incluyeEnsamble && siguienteArmado != null)
        btnArmado.text = if (siguienteArmado == "Ensamblando") "Iniciar ensamble" else "Marcar ensamble terminado"
        btnArmado.setOnClickListener {
            val estado = siguienteArmado ?: return@setOnClickListener
            ejecutar("Ensamble actualizado") { actualizarArmado(p.id, ActualizarArmado(estado)) }
        }

        val btnEnvio = findViewById<MaterialButton>(R.id.btnAvanzarEnvio)
        val ensamblePendiente = p.incluyeEnsamble && p.estadoArmado != "Terminado"
        btnEnvio.visible(activo && p.estadoEnvio != "Entregado")
        btnEnvio.isEnabled = !ensamblePendiente
        btnEnvio.text = when {
            ensamblePendiente -> "Envío disponible al terminar el ensamble"
            p.estadoEnvio == "Preparando paquete" && p.requiereDireccion -> "Registrar guía y marcar enviado"
            p.estadoEnvio == "Preparando paquete" -> "Marcar listo para recoger"
            else -> "Marcar como entregado"
        }
        btnEnvio.setOnClickListener {
            when {
                p.estadoEnvio == "Preparando paquete" && p.requiereDireccion -> registrarGuia(p)
                p.estadoEnvio == "Preparando paquete" ->
                    ejecutar("Pedido listo para recoger") { actualizarEnvio(p.id, ActualizarEnvio("Enviado", null, null)) }
                else -> confirmar("Confirmar entrega", "¿El cliente ya recibió el pedido ${p.folio}?", "Sí, entregado") {
                    ejecutar("Pedido entregado") { actualizarEnvio(p.id, ActualizarEnvio("Entregado", null, null)) }
                }
            }
        }

        val btnCancelar = findViewById<MaterialButton>(R.id.btnCancelarAdmin)
        btnCancelar.visible(p.puedeCancelar)
        btnCancelar.setOnClickListener {
            confirmar("Cancelar pedido", "Se devolverán las piezas al inventario y se notificará el reembolso.", "Cancelar pedido") {
                ejecutar("Pedido cancelado") { cancelarPedidoAdmin(p.id) }
            }
        }
    }

    private fun registrarGuia(p: PedidoDetalle) {
        dialogoFormulario(
            titulo = "Enviar pedido",
            mensaje = "Captura los datos de la paquetería para que el cliente pueda rastrear su paquete.",
            campos = listOf(
                Campo("paqueteria", "Paquetería", p.paqueteria.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS),
                Campo("guia", "Número de guía", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, "Ej. PQX4471029384")
            ),
            textoAceptar = "Marcar enviado"
        ) { valores, dialogo ->
            val guia = valores["guia"].orEmpty()
            if (!Regex("^[A-Za-z0-9-]{6,60}$").matches(guia)) {
                toast("Escribe un número de guía válido")
                return@dialogoFormulario
            }
            dialogo.dismiss()
            ejecutar("Pedido marcado como enviado") {
                actualizarEnvio(p.id, ActualizarEnvio("Enviado", valores["paqueteria"]?.ifBlank { null }, guia))
            }
        }
    }

    private fun ejecutar(mensaje: String, llamada: suspend ApiService.() -> PedidoDetalle) {
        if (ocupado) return
        ocupado = true
        lifecycleScope.launch {
            when (val r = api(llamada)) {
                is Resultado.Ok -> {
                    toast(mensaje)
                    mostrarExito = false
                    pintar(r.dato)
                }
                is Resultado.Error -> toast(r.mensaje)
            }
            ocupado = false
        }
    }
}
