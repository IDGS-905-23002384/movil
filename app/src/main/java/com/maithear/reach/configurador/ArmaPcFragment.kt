package com.maithear.reach.configurador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.maithear.reach.R
import com.maithear.reach.core.AgregarEnsamble
import com.maithear.reach.core.OpcionComponente
import com.maithear.reach.core.OpcionesRequest
import com.maithear.reach.core.PasoConfigurador
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.ResumenConfiguracion
import com.maithear.reach.core.TipoAviso
import com.maithear.reach.core.TiposComponente
import com.maithear.reach.core.agregarAviso
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.api
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.inflar
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.mxn
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import com.maithear.reach.core.visible
import com.maithear.reach.tienda.AccionesCarrito
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ArmaPcFragment : Fragment(), Refrescable {

    companion object {
        fun nombreTipo(tipo: String): String = when (tipo) {
            TiposComponente.CPU -> "Procesador"
            TiposComponente.MOTHERBOARD -> "Tarjeta madre"
            TiposComponente.RAM -> "Memoria RAM"
            TiposComponente.STORAGE -> "Almacenamiento"
            TiposComponente.GPU -> "Tarjeta de video"
            TiposComponente.COOLER -> "Enfriamiento"
            TiposComponente.CASE -> "Gabinete"
            TiposComponente.PSU -> "Fuente de poder"
            else -> tipo
        }
    }

    private var listaPasos: List<PasoConfigurador> = emptyList()
    private var indice = 0
    private var listaOpciones: List<OpcionComponente> = emptyList()
    private var resumen: ResumenConfiguracion? = null
    private var versionVista = -1
    private var trabajoOpciones: Job? = null
    private var trabajoResumen: Job? = null

    private lateinit var pasoAdapter: PasoAdapter
    private lateinit var opcionAdapter: OpcionAdapter
    private lateinit var rvPasos: RecyclerView
    private lateinit var rvOpciones: RecyclerView
    private lateinit var swIncompatibles: MaterialSwitch
    private lateinit var btnSiguiente: MaterialButton

    private val pasoActual: PasoConfigurador?
        get() = listaPasos.getOrNull(indice)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_arma_pc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerArma).insetsSistema(arriba = true)
        rvPasos = view.findViewById(R.id.rvPasos)
        rvOpciones = view.findViewById(R.id.rvOpciones)
        swIncompatibles = view.findViewById(R.id.swIncompatibles)
        btnSiguiente = view.findViewById(R.id.btnSiguientePaso)

        pasoAdapter = PasoAdapter { irAPaso(it) }
        rvPasos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPasos.adapter = pasoAdapter

        opcionAdapter = OpcionAdapter { alSeleccionar(it) }
        rvOpciones.layoutManager = LinearLayoutManager(context)
        rvOpciones.adapter = opcionAdapter

        swIncompatibles.setOnCheckedChangeListener { _, _ -> pintarOpciones() }
        btnSiguiente.setOnClickListener {
            if (indice >= listaPasos.size - 1) abrirResumen() else irAPaso(indice + 1)
        }
        view.findViewById<MaterialButton>(R.id.btnVerResumen).setOnClickListener { abrirResumen() }
        view.findViewById<View>(R.id.btnReiniciar).setOnClickListener {
            if (ConfiguradorEstado.vacio) return@setOnClickListener
            requireContext().confirmar("Reiniciar configuración", "Se quitarán todos los componentes seleccionados.", "Reiniciar") {
                ConfiguradorEstado.limpiar()
                irAPaso(0)
            }
        }
        pintarResumen()
        if (!isHidden) alMostrar()
    }

    override fun alMostrar() {
        if (view == null) return
        if (listaPasos.isEmpty()) {
            cargarPasos()
        } else if (ConfiguradorEstado.version != versionVista || ConfiguradorEstado.pasoInicial >= 0) {
            irAPaso(if (ConfiguradorEstado.pasoInicial >= 0) ConfiguradorEstado.pasoInicial else indice)
        }
    }

    private fun cargarPasos() {
        val raiz = view ?: return
        raiz.findViewById<View>(R.id.progresoOpciones).visible(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = api { pasos() }) {
                is Resultado.Ok -> {
                    listaPasos = r.dato
                    irAPaso(if (ConfiguradorEstado.pasoInicial >= 0) ConfiguradorEstado.pasoInicial else 0)
                }
                is Resultado.Error -> {
                    raiz.findViewById<View>(R.id.progresoOpciones).visible(false)
                    raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar el configurador", r.mensaje, "Reintentar") { cargarPasos() }
                }
            }
        }
    }

    private fun irAPaso(nuevo: Int) {
        val raiz = view ?: return
        if (listaPasos.isEmpty()) return
        indice = nuevo.coerceIn(0, listaPasos.size - 1)
        ConfiguradorEstado.pasoInicial = -1
        versionVista = ConfiguradorEstado.version
        val paso = listaPasos[indice]

        pasoAdapter.establecer(listaPasos, indice)
        rvPasos.smoothScrollToPosition(indice)
        raiz.findViewById<TextView>(R.id.tvPasoTitulo).text = "${indice + 1}. ${paso.nombre}"
        raiz.findViewById<TextView>(R.id.tvPasoAyuda).text = paso.ayuda
        raiz.findViewById<TextView>(R.id.tvPasoObligatorio).apply {
            text = if (paso.obligatorio) "Obligatorio" else "Según tu equipo"
            setBackgroundResource(if (paso.obligatorio) R.drawable.bg_badge_azul_claro else R.drawable.bg_badge_gris)
            setTextColor(ContextCompat.getColor(context, if (paso.obligatorio) R.color.azul else R.color.texto_sec))
        }
        btnSiguiente.text = if (indice >= listaPasos.size - 1) "Revisar y agregar" else "Siguiente: ${listaPasos[indice + 1].nombre}"
        swIncompatibles.isChecked = false
        cargarOpciones()
        actualizarResumen()
    }

    private fun cargarOpciones() {
        val raiz = view ?: return
        val paso = pasoActual ?: return
        trabajoOpciones?.cancel()
        raiz.ocultarVacio()
        opcionAdapter.establecer(emptyList(), paso.tipo)
        raiz.findViewById<View>(R.id.progresoOpciones).visible(true)
        trabajoOpciones = viewLifecycleOwner.lifecycleScope.launch {
            val r = api { opciones(OpcionesRequest(paso.tipo, ConfiguradorEstado.seleccionPc())) }
            raiz.findViewById<View>(R.id.progresoOpciones).visible(false)
            when (r) {
                is Resultado.Ok -> {
                    listaOpciones = r.dato
                    pintarOpciones()
                }
                is Resultado.Error -> {
                    listaOpciones = emptyList()
                    raiz.mostrarVacio(R.drawable.ic_warning, "Sin conexión", r.mensaje, "Reintentar") { cargarOpciones() }
                }
            }
        }
    }

    private fun pintarOpciones() {
        val raiz = view ?: return
        val paso = pasoActual ?: return
        val incompatibles = listaOpciones.count { !it.compatible }
        swIncompatibles.text = "Mostrar incompatibles ($incompatibles)"
        swIncompatibles.visible(incompatibles > 0)
        val lista = if (swIncompatibles.isChecked) {
            listaOpciones
        } else {
            listaOpciones.filter { it.compatible || ConfiguradorEstado.estaSeleccionado(paso.tipo, it.producto.id) }
        }
        opcionAdapter.establecer(lista, paso.tipo)
        if (lista.isEmpty()) {
            raiz.mostrarVacio(
                R.drawable.ic_chip,
                "Sin opciones compatibles",
                "Ninguna pieza de este paso es compatible con lo que elegiste. Cambia algún componente anterior."
            )
        } else {
            raiz.ocultarVacio()
        }
    }

    private fun alSeleccionar(opcion: OpcionComponente) {
        val paso = pasoActual ?: return
        val yaSeleccionado = ConfiguradorEstado.estaSeleccionado(paso.tipo, opcion.producto.id)
        if (!opcion.compatible && !yaSeleccionado) {
            requireContext().toast(opcion.motivos?.firstOrNull() ?: "Este componente no es compatible con tu selección.")
            return
        }
        ConfiguradorEstado.alternar(paso.tipo, opcion.producto)
        versionVista = ConfiguradorEstado.version
        opcionAdapter.refrescarSeleccion()
        pasoAdapter.establecer(listaPasos, indice)
        actualizarResumen()

        val ahoraSeleccionado = ConfiguradorEstado.estaSeleccionado(paso.tipo, opcion.producto.id)
        if (paso.tipo != TiposComponente.STORAGE && ahoraSeleccionado && indice < listaPasos.size - 1) {
            val pasoOrigen = indice
            rvOpciones.postDelayed({ if (indice == pasoOrigen && view != null) irAPaso(indice + 1) }, 350)
        }
    }

    private fun actualizarResumen() {
        if (view == null) return
        trabajoResumen?.cancel()
        trabajoResumen = viewLifecycleOwner.lifecycleScope.launch {
            api { validar(ConfiguradorEstado.seleccionPc()) }.siOk {
                resumen = it
                pintarResumen()
            }
        }
    }

    private fun pintarResumen() {
        val raiz = view ?: return
        val r = resumen
        val tvTotal = raiz.findViewById<TextView>(R.id.tvResumenTotal)
        val tvConsumo = raiz.findViewById<TextView>(R.id.tvResumenConsumo)
        val tvEstado = raiz.findViewById<TextView>(R.id.tvResumenEstado)
        if (r == null || r.componentes.isEmpty()) {
            tvTotal.text = 0.0.mxn()
            tvConsumo.text = "Elige tus componentes paso a paso"
            tvEstado.text = "Sin piezas"
            tvEstado.setBackgroundResource(R.drawable.bg_badge_gris)
            tvEstado.setTextColor(ContextCompat.getColor(raiz.context, R.color.texto_sec))
            return
        }
        tvTotal.text = r.total.mxn()
        tvConsumo.text = "Consumo ${r.consumoEstimadoW} W · Fuente sugerida ${r.fuenteRecomendadaW} W"
        val (texto, fondo, color) = when {
            r.errores.isNotEmpty() -> Triple(
                if (r.errores.size == 1) "1 conflicto" else "${r.errores.size} conflictos",
                R.drawable.bg_badge_rojo, R.color.rojo_oscuro
            )
            r.completo -> Triple("Listo", R.drawable.bg_badge_verde, R.color.verde_oscuro)
            else -> Triple(
                if (r.pendientes.size == 1) "1 pendiente" else "${r.pendientes.size} pendientes",
                R.drawable.bg_badge_ambar, R.color.ambar_oscuro
            )
        }
        tvEstado.text = texto
        tvEstado.setBackgroundResource(fondo)
        tvEstado.setTextColor(ContextCompat.getColor(raiz.context, color))
    }

    private fun abrirResumen() {
        val contexto = context ?: return
        val r = resumen
        if (r == null || r.componentes.isEmpty()) {
            contexto.toast("Primero elige algunos componentes")
            return
        }
        val dialogo = BottomSheetDialog(contexto)
        val vista = LayoutInflater.from(contexto).inflate(R.layout.sheet_resumen_pc, null)

        val avisos = vista.findViewById<LinearLayout>(R.id.llAvisosResumen)
        r.errores.forEach { avisos.agregarAviso(it, TipoAviso.ERROR) }
        r.pendientes.forEach { avisos.agregarAviso(it, TipoAviso.ALERTA) }
        r.advertencias.forEach { avisos.agregarAviso(it, TipoAviso.INFO) }
        if (r.completo) avisos.agregarAviso("Todas las piezas son compatibles entre sí.", TipoAviso.EXITO)

        val componentes = vista.findViewById<LinearLayout>(R.id.llComponentesResumen)
        for (c in r.componentes) {
            val fila = componentes.inflar(R.layout.item_resumen_componente)
            fila.findViewById<ImageView>(R.id.ivRc).cargarImagen(c.producto.imagenUrl)
            fila.findViewById<TextView>(R.id.tvRcPaso).text = c.paso
            fila.findViewById<TextView>(R.id.tvRcNombre).text = c.producto.nombre
            fila.findViewById<TextView>(R.id.tvRcPrecio).text = c.producto.precio.mxn()
            fila.setOnClickListener {
                dialogo.dismiss()
                val destino = listaPasos.indexOfFirst { it.tipo == c.tipo }
                if (destino >= 0) irAPaso(destino)
            }
            componentes.addView(fila)
        }

        val totales = vista.findViewById<LinearLayout>(R.id.llTotalesResumen)
        totales.agregarFilaTotal("Componentes", r.subtotal.mxn())
        totales.agregarFilaTotal("Ensamble, cableado y pruebas", r.costoEnsamble.mxn())
        totales.agregarFilaTotal("Consumo estimado", "${r.consumoEstimadoW} W")
        totales.agregarFilaTotal("Total", r.total.mxn(), destacado = true)

        val etNombre = vista.findViewById<EditText>(R.id.etNombreEquipo)
        val btnAgregar = vista.findViewById<MaterialButton>(R.id.btnAgregarEnsamble)
        btnAgregar.isEnabled = r.completo
        if (!r.completo) btnAgregar.text = "Completa tu configuración"
        btnAgregar.setOnClickListener {
            val actividad = activity ?: return@setOnClickListener
            if (!actividad.requiereSesion()) return@setOnClickListener
            btnAgregar.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                when (val resultado = api { agregarEnsamble(AgregarEnsamble(ConfiguradorEstado.seleccionPc(), etNombre.valor().ifBlank { null })) }) {
                    is Resultado.Ok -> {
                        dialogo.dismiss()
                        ConfiguradorEstado.limpiar()
                        resumen = null
                        irAPaso(0)
                        AccionesCarrito.notificar(actividad, resultado.dato, "Tu PC se agregó al carrito")
                        (actividad as? TiendaActivity)?.actualizarBadge(resultado.dato.totalArticulos)
                    }
                    is Resultado.Error -> {
                        btnAgregar.isEnabled = true
                        actividad.toast(resultado.mensaje)
                    }
                }
            }
        }

        dialogo.setContentView(vista)
        dialogo.show()
    }
}
