package com.maithear.reach.admin

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Categoria
import com.maithear.reach.core.EspecificacionComponente
import com.maithear.reach.core.ProductoDetalle
import com.maithear.reach.core.ProductoGuardar
import com.maithear.reach.core.Proveedor
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.TiposComponente
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.dp
import com.maithear.reach.core.exito
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import com.maithear.reach.core.visible
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FormProductoActivity : BaseActivity() {

    companion object {
        const val EXTRA_ID = "producto_id"

        private data class CampoCompat(val clave: String, val etiqueta: String, val numerico: Boolean, val pista: String = "")

        private val CAMPOS: Map<String, List<CampoCompat>> = mapOf(
            TiposComponente.CPU to listOf(
                CampoCompat("socket", "Socket", false, "AM5, AM4, LGA1700, LGA1851"),
                CampoCompat("consumoW", "TDP (W)", true)
            ),
            TiposComponente.MOTHERBOARD to listOf(
                CampoCompat("socket", "Socket", false, "AM5"),
                CampoCompat("formFactor", "Formato", false, "ATX, mATX o Mini-ITX"),
                CampoCompat("tipoMemoria", "Tipo de memoria", false, "DDR4 o DDR5"),
                CampoCompat("slotsRam", "Ranuras de RAM", true),
                CampoCompat("ranurasM2", "Ranuras M.2", true),
                CampoCompat("consumoW", "Consumo (W)", true)
            ),
            TiposComponente.RAM to listOf(
                CampoCompat("tipoMemoria", "Tipo de memoria", false, "DDR4 o DDR5"),
                CampoCompat("capacidadGb", "Capacidad total (GB)", true),
                CampoCompat("modulosRam", "Número de módulos", true),
                CampoCompat("consumoW", "Consumo (W)", true)
            ),
            TiposComponente.STORAGE to listOf(
                CampoCompat("interfaz", "Interfaz", false, "M.2 NVMe o SATA"),
                CampoCompat("capacidadGb", "Capacidad (GB)", true),
                CampoCompat("consumoW", "Consumo (W)", true)
            ),
            TiposComponente.GPU to listOf(
                CampoCompat("longitudMm", "Longitud (mm)", true),
                CampoCompat("consumoW", "Consumo TBP (W)", true),
                CampoCompat("fuenteRecomendadaW", "Fuente recomendada (W)", true)
            ),
            TiposComponente.COOLER to listOf(
                CampoCompat("socketsSoportados", "Sockets soportados", false, "AM4,AM5,LGA1700,LGA1851"),
                CampoCompat("tdpMaxW", "TDP máximo (W)", true),
                CampoCompat("alturaMm", "Altura disipador aire (mm)", true),
                CampoCompat("radiadorMm", "Radiador líquido (mm)", true),
                CampoCompat("consumoW", "Consumo (W)", true)
            ),
            TiposComponente.CASE to listOf(
                CampoCompat("formFactorsSoportados", "Formatos soportados", false, "ATX,mATX,Mini-ITX"),
                CampoCompat("longitudMaxGpuMm", "GPU máxima (mm)", true),
                CampoCompat("alturaMaxCoolerMm", "Disipador máximo (mm)", true),
                CampoCompat("radiadorMaxMm", "Radiador máximo (mm)", true)
            ),
            TiposComponente.PSU to listOf(CampoCompat("capacidadW", "Potencia (W)", true))
        )
    }

    private val productoId: Int?
        get() = intent.getIntExtra(EXTRA_ID, 0).takeIf { it > 0 }

    private var listaCategorias: List<Categoria> = emptyList()
    private var listaProveedores: List<Proveedor> = emptyList()
    private var producto: ProductoDetalle? = null
    private val camposCompat = mutableMapOf<String, EditText>()
    private var cbGraficos: CheckBox? = null
    private var cbDisipador: CheckBox? = null
    private var tipoActual: String? = null

    private lateinit var spCategoria: Spinner
    private lateinit var spProveedor: Spinner
    private lateinit var btnGuardar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_producto)
        configurarHeader(if (productoId == null) "Nuevo producto" else "Editar producto", "Catálogo")
        findViewById<View>(R.id.barraFormProducto).insetsSistema(abajo = true)

        spCategoria = findViewById(R.id.spCategoria)
        spProveedor = findViewById(R.id.spProveedor)
        btnGuardar = findViewById(R.id.btnGuardarProducto)

        val etImagen = findViewById<EditText>(R.id.etImagenUrl)
        etImagen.setOnFocusChangeListener { _, foco ->
            if (!foco) findViewById<ImageView>(R.id.ivPreviewProducto).cargarImagen(etImagen.valor())
        }

        spCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                construirCompatibilidad(listaCategorias.getOrNull(position)?.tipoComponente, producto?.especificacion)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        btnGuardar.setOnClickListener { guardar() }
        cargar()
    }

    private fun cargar() {
        val progreso = findViewById<View>(R.id.progresoFormProducto)
        progreso.visible(true)
        btnGuardar.isEnabled = false
        lifecycleScope.launch {
            val rCategorias = async { api { categorias() } }
            val rProveedores = async { api { proveedores() } }
            val id = productoId
            val rProducto = if (id != null) async { api { producto(id) } } else null

            when (val c = rCategorias.await()) {
                is Resultado.Ok -> listaCategorias = c.dato
                is Resultado.Error -> {
                    toast(c.mensaje)
                    finish()
                    return@launch
                }
            }
            (rProveedores.await() as? Resultado.Ok)?.let { listaProveedores = it.dato }
            if (rProducto != null) {
                when (val p = rProducto.await()) {
                    is Resultado.Ok -> producto = p.dato
                    is Resultado.Error -> {
                        toast(p.mensaje)
                        finish()
                        return@launch
                    }
                }
            }

            spCategoria.adapter = ArrayAdapter(this@FormProductoActivity, android.R.layout.simple_spinner_dropdown_item, listaCategorias.map { it.nombre })
            val nombresProveedor = listOf("Sin proveedor") + listaProveedores.map { it.nombreEmpresa }
            spProveedor.adapter = ArrayAdapter(this@FormProductoActivity, android.R.layout.simple_spinner_dropdown_item, nombresProveedor)
            producto?.let { pintar(it) }
            progreso.visible(false)
            btnGuardar.isEnabled = true
        }
    }

    private fun texto(id: Int): EditText = findViewById(id)

    private fun pintar(p: ProductoDetalle) {
        texto(R.id.etSku).setText(p.sku)
        texto(R.id.etNombreProducto).setText(p.nombre)
        texto(R.id.etMarcaProducto).setText(p.marca)
        texto(R.id.etPrecioProducto).setText(formatear(p.precio))
        texto(R.id.etPrecioAnterior).setText(p.precioAnterior?.let { formatear(it) } ?: "")
        texto(R.id.etStock).setText(p.stock.toString())
        texto(R.id.etStockMinimo).setText(p.stockMinimo.toString())
        texto(R.id.etImagenUrl).setText(p.imagenUrl.orEmpty())
        texto(R.id.etDescripcionProducto).setText(p.descripcion.orEmpty())
        texto(R.id.etEspecificaciones).setText(p.especificacionesTecnicas.orEmpty())
        findViewById<MaterialSwitch>(R.id.swDestacado).isChecked = p.destacado
        findViewById<MaterialSwitch>(R.id.swEnvio).isChecked = p.envio
        findViewById<MaterialSwitch>(R.id.swActivo).isChecked = p.activo
        findViewById<ImageView>(R.id.ivPreviewProducto).cargarImagen(p.imagenUrl)

        val indiceCategoria = listaCategorias.indexOfFirst { it.id == p.categoriaId }
        if (indiceCategoria >= 0) spCategoria.setSelection(indiceCategoria)
        val indiceProveedor = listaProveedores.indexOfFirst { it.id == p.proveedorId }
        spProveedor.setSelection(if (indiceProveedor >= 0) indiceProveedor + 1 else 0)
        construirCompatibilidad(listaCategorias.getOrNull(indiceCategoria)?.tipoComponente, p.especificacion)

        val btnEstado = findViewById<MaterialButton>(R.id.btnEstadoProducto)
        btnEstado.visible(true)
        btnEstado.text = if (p.activo) "Desactivar" else "Activar"
        btnEstado.setOnClickListener { cambiarEstado(p) }
    }

    private fun formatear(valor: Double): String =
        if (valor % 1.0 == 0.0) valor.toLong().toString() else String.format(java.util.Locale.US, "%.2f", valor)

    private fun construirCompatibilidad(tipo: String?, especificacion: EspecificacionComponente?) {
        if (tipo == tipoActual && camposCompat.isNotEmpty()) return
        tipoActual = tipo
        val tarjeta = findViewById<View>(R.id.cardCompatibilidad)
        val contenedor = findViewById<LinearLayout>(R.id.llCompatibilidad)
        contenedor.removeAllViews()
        camposCompat.clear()
        cbGraficos = null
        cbDisipador = null
        val campos = CAMPOS[tipo]
        tarjeta.visible(campos != null)
        if (campos == null) return

        for (campo in campos) {
            contenedor.addView(TextView(this).apply {
                text = campo.etiqueta
                setTextColor(ContextCompat.getColor(context, R.color.texto))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, dp(6))
            })
            val entrada = EditText(this).apply {
                setText(valorDe(especificacion, campo.clave))
                hint = campo.pista
                inputType = if (campo.numerico) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
                maxLines = 1
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.texto))
                setHintTextColor(ContextCompat.getColor(context, R.color.gris))
                setBackgroundResource(R.drawable.bg_input)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(12)
                }
            }
            camposCompat[campo.clave] = entrada
            contenedor.addView(entrada)
        }
        if (tipo == TiposComponente.CPU) {
            cbGraficos = CheckBox(this).apply {
                text = "Tiene gráficos integrados"
                isChecked = especificacion?.graficosIntegrados == true
            }
            cbDisipador = CheckBox(this).apply {
                text = "Incluye disipador de fábrica"
                isChecked = especificacion?.incluyeDisipador == true
            }
            contenedor.addView(cbGraficos)
            contenedor.addView(cbDisipador)
        }
    }

    private fun valorDe(e: EspecificacionComponente?, clave: String): String {
        if (e == null) return ""
        val v: Any? = when (clave) {
            "socket" -> e.socket
            "socketsSoportados" -> e.socketsSoportados
            "tipoMemoria" -> e.tipoMemoria
            "formFactor" -> e.formFactor
            "formFactorsSoportados" -> e.formFactorsSoportados
            "interfaz" -> e.interfaz
            "consumoW" -> e.consumoW
            "capacidadW" -> e.capacidadW
            "fuenteRecomendadaW" -> e.fuenteRecomendadaW
            "tdpMaxW" -> e.tdpMaxW
            "longitudMm" -> e.longitudMm
            "longitudMaxGpuMm" -> e.longitudMaxGpuMm
            "alturaMm" -> e.alturaMm
            "alturaMaxCoolerMm" -> e.alturaMaxCoolerMm
            "radiadorMm" -> e.radiadorMm
            "radiadorMaxMm" -> e.radiadorMaxMm
            "slotsRam" -> e.slotsRam
            "modulosRam" -> e.modulosRam
            "capacidadGb" -> e.capacidadGb
            "ranurasM2" -> e.ranurasM2
            else -> null
        }
        return v?.toString() ?: ""
    }

    private fun especificacion(): EspecificacionComponente? {
        if (camposCompat.isEmpty()) return null
        fun t(clave: String): String? = camposCompat[clave]?.valor()?.ifBlank { null }
        fun n(clave: String): Int? = t(clave)?.toIntOrNull()
        return EspecificacionComponente(
            socket = t("socket")?.uppercase(),
            socketsSoportados = t("socketsSoportados")?.uppercase()?.replace(" ", ""),
            tipoMemoria = t("tipoMemoria")?.uppercase(),
            formFactor = t("formFactor"),
            formFactorsSoportados = t("formFactorsSoportados")?.replace(" ", ""),
            interfaz = t("interfaz"),
            consumoW = n("consumoW"),
            capacidadW = n("capacidadW"),
            fuenteRecomendadaW = n("fuenteRecomendadaW"),
            tdpMaxW = n("tdpMaxW"),
            longitudMm = n("longitudMm"),
            longitudMaxGpuMm = n("longitudMaxGpuMm"),
            alturaMm = n("alturaMm"),
            alturaMaxCoolerMm = n("alturaMaxCoolerMm"),
            radiadorMm = n("radiadorMm"),
            radiadorMaxMm = n("radiadorMaxMm"),
            slotsRam = n("slotsRam"),
            modulosRam = n("modulosRam"),
            capacidadGb = n("capacidadGb"),
            ranurasM2 = n("ranurasM2"),
            graficosIntegrados = cbGraficos?.isChecked,
            incluyeDisipador = cbDisipador?.isChecked
        )
    }

    private fun guardar() {
        val sku = texto(R.id.etSku).valor().uppercase()
        val nombre = texto(R.id.etNombreProducto).valor()
        val marca = texto(R.id.etMarcaProducto).valor()
        val precio = texto(R.id.etPrecioProducto).valor().toDoubleOrNull()
        val anteriorTexto = texto(R.id.etPrecioAnterior).valor()
        val anterior = anteriorTexto.toDoubleOrNull()
        val stock = texto(R.id.etStock).valor().ifBlank { "0" }.toIntOrNull()
        val minimo = texto(R.id.etStockMinimo).valor().ifBlank { "5" }.toIntOrNull()
        val categoria = listaCategorias.getOrNull(spCategoria.selectedItemPosition)

        val error = when {
            !Regex("^[A-Z0-9-]{3,40}$").matches(sku) -> R.id.etSku to "Solo letras, números y guiones (3 a 40)"
            nombre.length < 3 -> R.id.etNombreProducto to "Escribe el nombre"
            marca.isEmpty() -> R.id.etMarcaProducto to "Escribe la marca"
            precio == null || precio <= 0 -> R.id.etPrecioProducto to "Precio no válido"
            anteriorTexto.isNotEmpty() && (anterior == null || anterior <= precio) -> R.id.etPrecioAnterior to "Debe ser mayor al precio actual"
            stock == null || stock < 0 -> R.id.etStock to "Cantidad no válida"
            minimo == null || minimo < 0 -> R.id.etStockMinimo to "Cantidad no válida"
            else -> null
        }
        if (error != null) {
            texto(error.first).apply {
                this.error = error.second
                requestFocus()
            }
            return
        }
        if (categoria == null) {
            toast("Selecciona una categoría")
            return
        }

        val proveedorIndice = spProveedor.selectedItemPosition - 1
        val datos = ProductoGuardar(
            sku = sku,
            nombre = nombre,
            marca = marca,
            descripcion = texto(R.id.etDescripcionProducto).valor(),
            precio = precio!!,
            precioAnterior = anterior,
            stock = stock!!,
            stockMinimo = minimo!!,
            imagenUrl = texto(R.id.etImagenUrl).valor(),
            especificacionesTecnicas = texto(R.id.etEspecificaciones).text.toString().trim(),
            categoriaId = categoria.id,
            proveedorId = listaProveedores.getOrNull(proveedorIndice)?.id,
            envio = findViewById<MaterialSwitch>(R.id.swEnvio).isChecked,
            destacado = findViewById<MaterialSwitch>(R.id.swDestacado).isChecked,
            activo = findViewById<MaterialSwitch>(R.id.swActivo).isChecked,
            especificacion = especificacion()
        )

        btnGuardar.ocultarTeclado()
        btnGuardar.isEnabled = false
        lifecycleScope.launch {
            val id = productoId
            val r = api { if (id == null) crearProducto(datos) else actualizarProducto(id, datos) }
            when (r) {
                is Resultado.Ok -> {
                    toast(if (id == null) "Producto creado" else "Producto actualizado")
                    finish()
                }
                is Resultado.Error -> {
                    toast(r.mensaje)
                    btnGuardar.isEnabled = true
                }
            }
        }
    }

    private fun cambiarEstado(p: ProductoDetalle) {
        val activar = !p.activo
        confirmar(
            if (activar) "Activar producto" else "Desactivar producto",
            if (activar) "El producto volverá a mostrarse en la tienda." else "El producto dejará de mostrarse y se quitará de los carritos.",
            if (activar) "Activar" else "Desactivar"
        ) {
            lifecycleScope.launch {
                val r = api { if (activar) activarProducto(p.id).exito() else desactivarProducto(p.id).exito() }
                when (r) {
                    is Resultado.Ok -> {
                        toast(if (activar) "Producto activado" else "Producto desactivado")
                        finish()
                    }
                    is Resultado.Error -> toast(r.mensaje)
                }
            }
        }
    }
}
