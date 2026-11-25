package com.maithear.reach.configurador

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.maithear.reach.R
import com.maithear.reach.core.AgregarEnsamble
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Recomendacion
import com.maithear.reach.core.RecomendacionRequest
import com.maithear.reach.core.Recomendaciones
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.UsoEquipo
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.dp
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mxn
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import com.maithear.reach.tienda.AccionesCarrito
import com.maithear.reach.tienda.ProductoDetalleActivity
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.launch

class RecomendadorActivity : BaseActivity() {

    private lateinit var chips: ChipGroup
    private lateinit var slider: Slider
    private lateinit var tvPresupuesto: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var btnRecomendar: MaterialButton
    private lateinit var progreso: LinearProgressIndicator
    private lateinit var aviso: TextView
    private lateinit var contenedor: LinearLayout
    private var usos: Map<String, UsoEquipo> = emptyMap()

    private val usoActual: String
        get() = findViewById<Chip>(chips.checkedChipId)?.tag as? String ?: "GAMING_FHD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recomendador)
        configurarHeader("Armado inteligente", "Configurador")
        findViewById<View>(R.id.contenidoRecomendador).insetsSistema(abajo = true)

        chips = findViewById(R.id.chipsUso)
        slider = findViewById(R.id.sliderPresupuesto)
        tvPresupuesto = findViewById(R.id.tvPresupuesto)
        tvDescripcion = findViewById(R.id.tvUsoDescripcion)
        btnRecomendar = findViewById(R.id.btnRecomendar)
        progreso = findViewById(R.id.progresoRecomendador)
        aviso = findViewById(R.id.tvAvisoRecomendador)
        contenedor = findViewById(R.id.llRecomendaciones)

        slider.setLabelFormatter { it.toDouble().mxn() }
        slider.addOnChangeListener { _, valor, _ -> pintarPresupuesto(valor) }
        pintarPresupuesto(slider.value)

        chips.setOnCheckedStateChangeListener { _, _ ->
            aplicarUso(usoActual, moverPresupuesto = true)
            limpiarResultados()
        }
        btnRecomendar.setOnClickListener { recomendar() }
        cargarUsos()
    }

    private fun cargarUsos() {
        lifecycleScope.launch {
            api { usosEquipo() }.siOk { lista ->
                usos = lista.associateBy { it.clave }
                aplicarUso(usoActual, moverPresupuesto = true)
            }
        }
    }

    private fun aplicarUso(clave: String, moverPresupuesto: Boolean) {
        val uso = usos[clave] ?: return
        tvDescripcion.text = uso.descripcion
        if (moverPresupuesto) {
            val sugerido = uso.presupuestoSugerido.toFloat().coerceIn(slider.valueFrom, slider.valueTo)
            slider.value = (Math.round(sugerido / slider.stepSize) * slider.stepSize)
        }
    }

    private fun pintarPresupuesto(valor: Float) {
        tvPresupuesto.text = valor.toDouble().mxn()
    }

    private fun limpiarResultados() {
        contenedor.removeAllViews()
        aviso.visible(false)
        findViewById<View>(R.id.tvTituloResultados).visible(false)
        findViewById<View>(R.id.tvSubtituloResultados).visible(false)
    }

    private fun recomendar() {
        limpiarResultados()
        btnRecomendar.isEnabled = false
        progreso.visible(true)
        lifecycleScope.launch {
            val r = api { recomendar(RecomendacionRequest(slider.value.toDouble(), usoActual)) }
            btnRecomendar.isEnabled = true
            progreso.visible(false)
            when (r) {
                is Resultado.Ok -> pintarResultados(r.dato)
                is Resultado.Error -> mostrarAviso(r.mensaje)
            }
        }
    }

    private fun mostrarAviso(mensaje: String) {
        aviso.text = mensaje
        aviso.visible(true)
    }

    private fun pintarResultados(datos: Recomendaciones) {
        if (datos.opciones.isEmpty()) {
            mostrarAviso("No encontramos una combinación disponible. Intenta con otro presupuesto.")
            return
        }
        val titulo = findViewById<TextView>(R.id.tvTituloResultados)
        titulo.text = if (datos.opciones.size == 1) {
            "Tu equipo ideal para ${datos.usoNombre.lowercase()}"
        } else {
            "${datos.opciones.size} opciones para ${datos.usoNombre.lowercase()}"
        }
        titulo.visible(true)
        findViewById<View>(R.id.tvSubtituloResultados).visible(true)
        if (datos.opciones.size == 1 && datos.opciones.first().ahorro > 0) {
            mostrarAviso("Con ${datos.opciones.first().resumen.total.mxn()} ya obtienes el máximo rendimiento útil para este uso.")
        }
        datos.opciones.forEach { contenedor.addView(crearTarjeta(it, datos.usoNombre)) }
        val scroll = findViewById<NestedScrollView>(R.id.scrollRecomendador)
        scroll.post { scroll.smoothScrollTo(0, titulo.top - dp(8)) }
    }

    private fun crearTarjeta(rec: Recomendacion, usoNombre: String): View {
        val vista = contenedor.inflar(R.layout.item_recomendacion)
        val nivel = vista.findViewById<TextView>(R.id.tvRecNivel)
        nivel.text = rec.titulo.uppercase()
        nivel.setBackgroundResource(
            when (rec.nivel) {
                "MAXIMA" -> R.drawable.bg_badge_morado
                "EQUILIBRADA" -> R.drawable.bg_badge_azul
                else -> R.drawable.bg_badge_verde
            }
        )
        nivel.setTextColor(
            ContextCompat.getColor(
                this,
                when (rec.nivel) {
                    "MAXIMA" -> R.color.white
                    "EQUILIBRADA" -> R.color.white
                    else -> R.color.verde_oscuro
                }
            )
        )
        vista.findViewById<TextView>(R.id.tvRecAhorro).apply {
            visible(rec.ahorro >= 1)
            text = "Ahorras ${rec.ahorro.mxn()}"
        }
        vista.findViewById<TextView>(R.id.tvRecTotal).text = rec.resumen.total.mxn()
        vista.findViewById<TextView>(R.id.tvRecDescripcion).text = rec.descripcion
        vista.findViewById<TextView>(R.id.tvRecPuntaje).text = "${rec.puntaje}/100"
        vista.findViewById<LinearProgressIndicator>(R.id.progRecPuntaje).progress = rec.puntaje

        val destacados = vista.findViewById<LinearLayout>(R.id.llRecDestacados)
        rec.destacados.forEach { destacados.addView(filaDestacado(it)) }

        val componentes = vista.findViewById<LinearLayout>(R.id.llRecComponentes)
        rec.resumen.componentes.forEach { c ->
            val fila = componentes.inflar(R.layout.item_resumen_componente)
            fila.findViewById<ImageView>(R.id.ivRc).cargarImagen(c.producto.imagenUrl)
            fila.findViewById<TextView>(R.id.tvRcPaso).text = c.paso
            fila.findViewById<TextView>(R.id.tvRcNombre).text = c.producto.nombre
            fila.findViewById<TextView>(R.id.tvRcPrecio).text = c.producto.precio.mxn()
            fila.setOnClickListener { ProductoDetalleActivity.abrir(this, c.producto.id) }
            componentes.addView(fila)
        }
        val totales = vista.findViewById<LinearLayout>(R.id.llRecTotales)
        totales.agregarFilaTotal("Componentes", rec.resumen.subtotal.mxn())
        totales.agregarFilaTotal("Ensamble y pruebas", rec.resumen.costoEnsamble.mxn())
        totales.agregarFilaTotal("Total", rec.resumen.total.mxn(), destacado = true)
        rec.resumen.advertencias.forEach { totales.addView(filaNota(it)) }

        val detalle = vista.findViewById<MaterialButton>(R.id.btnRecDetalle)
        val textoDetalle = "Ver los ${rec.resumen.componentes.size} componentes"
        detalle.text = textoDetalle
        detalle.setOnClickListener {
            val abrir = componentes.visibility != View.VISIBLE
            componentes.visible(abrir)
            totales.visible(abrir)
            detalle.text = if (abrir) "Ocultar componentes" else textoDetalle
        }

        vista.findViewById<MaterialButton>(R.id.btnRecPersonalizar).setOnClickListener {
            ConfiguradorEstado.cargarDesde(rec.resumen.componentes)
            startActivity(
                Intent(this, TiendaActivity::class.java)
                    .putExtra(TiendaActivity.EXTRA_TAB, R.id.nav_arma)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }

        val agregar = vista.findViewById<MaterialButton>(R.id.btnRecAgregar)
        agregar.setOnClickListener {
            if (!requiereSesion()) return@setOnClickListener
            agregar.isEnabled = false
            lifecycleScope.launch {
                val nombre = "PC $usoNombre ${rec.titulo}".take(60)
                when (val r = api { agregarEnsamble(AgregarEnsamble(rec.seleccion, nombre)) }) {
                    is Resultado.Ok -> AccionesCarrito.notificar(this@RecomendadorActivity, r.dato, "Equipo agregado al carrito")
                    is Resultado.Error -> toast(r.mensaje)
                }
                agregar.isEnabled = true
            }
        }
        return vista
    }

    private fun filaDestacado(texto: String): TextView = TextView(this).apply {
        text = texto
        textSize = 13f
        setTextColor(ContextCompat.getColor(context, R.color.texto))
        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
        compoundDrawablePadding = dp(8)
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun filaNota(texto: String): TextView = TextView(this).apply {
        text = texto
        textSize = 11f
        setTextColor(ContextCompat.getColor(context, R.color.texto_ter))
        setPadding(0, dp(4), 0, 0)
    }
}
