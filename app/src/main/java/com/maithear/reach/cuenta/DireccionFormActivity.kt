package com.maithear.reach.cuenta

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Direccion
import com.maithear.reach.core.DireccionGuardar
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.api
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import com.maithear.reach.core.visible
import com.maithear.reach.mapas.Mapas
import com.maithear.reach.mapas.UbicacionActivity
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlinx.coroutines.launch

class DireccionFormActivity : BaseActivity() {

    companion object {
        const val EXTRA_DIRECCION = "direccion"

        private val ESTADOS = listOf(
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas", "Chihuahua",
            "Ciudad de México", "Coahuila", "Colima", "Durango", "Estado de México", "Guanajuato", "Guerrero",
            "Hidalgo", "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla", "Querétaro",
            "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz",
            "Yucatán", "Zacatecas"
        )
    }

    private var existente: Direccion? = null
    private var latitud: Double? = null
    private var longitud: Double? = null
    private lateinit var mapaMini: MapView

    private val selectorUbicacion = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val datos = r.data
        if (r.resultCode != Activity.RESULT_OK || datos == null) return@registerForActivityResult
        latitud = datos.getDoubleExtra(UbicacionActivity.EXTRA_LATITUD, 0.0)
        longitud = datos.getDoubleExtra(UbicacionActivity.EXTRA_LONGITUD, 0.0)
        completarSiVacio(R.id.etDirCalle, datos.getStringExtra(UbicacionActivity.EXTRA_CALLE))
        completarSiVacio(R.id.etDirExterior, datos.getStringExtra(UbicacionActivity.EXTRA_NUMERO))
        completarSiVacio(R.id.etDirColonia, datos.getStringExtra(UbicacionActivity.EXTRA_COLONIA))
        completarSiVacio(R.id.etDirCiudad, datos.getStringExtra(UbicacionActivity.EXTRA_CIUDAD))
        completarSiVacio(R.id.etDirEstado, datos.getStringExtra(UbicacionActivity.EXTRA_ESTADO))
        completarSiVacio(R.id.etDirCp, datos.getStringExtra(UbicacionActivity.EXTRA_CP)?.filter { it.isDigit() }?.take(5))
        pintarUbicacion()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direccion_form)
        existente = intent.getStringExtra(EXTRA_DIRECCION)?.let { Gson().fromJson(it, Direccion::class.java) }
        configurarHeader(if (existente == null) "Nueva dirección" else "Editar dirección", "Entregas")
        findViewById<View>(R.id.barraDireccion).insetsSistema(abajo = true)

        val contenedorMapa = findViewById<View>(R.id.mapaMiniDireccion)
        mapaMini = contenedorMapa.findViewById(R.id.mapaTarjeta)
        Mapas.preparar(mapaMini, zoom = 12.0, interactivo = false)
        contenedorMapa.findViewById<View>(R.id.vMapaBloqueo).apply {
            visible(true)
            setOnClickListener { abrirSelector() }
        }
        findViewById<MaterialButton>(R.id.btnUbicarMapa).setOnClickListener { abrirSelector() }

        val estado = findViewById<AutoCompleteTextView>(R.id.etDirEstado)
        estado.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ESTADOS))

        existente?.let { d ->
            findViewById<EditText>(R.id.etDirNombre).setText(d.nombreRecibe)
            findViewById<EditText>(R.id.etDirTelefono).setText(d.telefono)
            findViewById<EditText>(R.id.etDirCalle).setText(d.calle)
            findViewById<EditText>(R.id.etDirExterior).setText(d.numeroExterior)
            findViewById<EditText>(R.id.etDirInterior).setText(d.numeroInterior.orEmpty())
            findViewById<EditText>(R.id.etDirColonia).setText(d.colonia)
            findViewById<EditText>(R.id.etDirCp).setText(d.codigoPostal)
            findViewById<EditText>(R.id.etDirCiudad).setText(d.ciudad)
            estado.setText(d.estado, false)
            findViewById<EditText>(R.id.etDirReferencias).setText(d.referencias.orEmpty())
            findViewById<CheckBox>(R.id.cbDirPredeterminada).isChecked = d.esPredeterminada
            val chip = when (d.alias) {
                "Casa" -> R.id.chipCasa
                "Trabajo" -> R.id.chipTrabajo
                else -> R.id.chipOtro
            }
            findViewById<ChipGroup>(R.id.chipsAlias).check(chip)
            latitud = d.latitud
            longitud = d.longitud
        }
        pintarUbicacion()

        findViewById<MaterialButton>(R.id.btnGuardarDireccion).setOnClickListener { guardar() }
    }

    override fun onResume() {
        super.onResume()
        mapaMini.onResume()
    }

    override fun onPause() {
        mapaMini.onPause()
        super.onPause()
    }

    private fun abrirSelector() {
        val intent = Intent(this, UbicacionActivity::class.java)
        val lat = latitud
        val lng = longitud
        if (lat != null && lng != null) {
            intent.putExtra(UbicacionActivity.EXTRA_LATITUD, lat).putExtra(UbicacionActivity.EXTRA_LONGITUD, lng)
        }
        selectorUbicacion.launch(intent)
    }

    private fun completarSiVacio(id: Int, valor: String?) {
        val campo = findViewById<EditText>(id)
        if (campo.valor().isEmpty() && !valor.isNullOrBlank()) campo.setText(valor)
    }

    private fun pintarUbicacion() {
        val lat = latitud
        val lng = longitud
        Mapas.limpiar(mapaMini)
        val estado = findViewById<TextView>(R.id.tvUbicacionEstado)
        val boton = findViewById<MaterialButton>(R.id.btnUbicarMapa)
        if (lat != null && lng != null) {
            val punto = GeoPoint(lat, lng)
            Mapas.marcador(mapaMini, punto, "Domicilio", R.drawable.pin_destino)
            mapaMini.controller.setZoom(16.5)
            mapaMini.controller.setCenter(punto)
            estado.text = "Ubicación guardada. El repartidor llegará directo a tu puerta."
            boton.text = "Cambiar"
        } else {
            mapaMini.controller.setZoom(12.0)
            mapaMini.controller.setCenter(Mapas.LEON)
            estado.text = "Ubica tu domicilio para que el repartidor llegue sin problemas."
            boton.text = "Ubicar"
        }
        mapaMini.invalidate()
    }

    private fun campo(id: Int): EditText = findViewById(id)

    private fun guardar() {
        val nombre = campo(R.id.etDirNombre).valor()
        val telefono = campo(R.id.etDirTelefono).valor()
        val calle = campo(R.id.etDirCalle).valor()
        val exterior = campo(R.id.etDirExterior).valor()
        val colonia = campo(R.id.etDirColonia).valor()
        val cp = campo(R.id.etDirCp).valor()
        val ciudad = campo(R.id.etDirCiudad).valor()
        val estado = campo(R.id.etDirEstado).valor()

        val error = when {
            nombre.length < 2 -> R.id.etDirNombre to "Indica quién recibe"
            !Regex("^\\d{10}$").matches(telefono) -> R.id.etDirTelefono to "Teléfono de 10 dígitos"
            calle.isEmpty() -> R.id.etDirCalle to "Escribe la calle"
            exterior.isEmpty() -> R.id.etDirExterior to "Requerido"
            colonia.isEmpty() -> R.id.etDirColonia to "Escribe la colonia"
            !Regex("^\\d{5}$").matches(cp) -> R.id.etDirCp to "5 dígitos"
            ciudad.isEmpty() -> R.id.etDirCiudad to "Escribe la ciudad"
            estado.isEmpty() -> R.id.etDirEstado to "Escribe el estado"
            else -> null
        }
        if (error != null) {
            campo(error.first).apply {
                this.error = error.second
                requestFocus()
            }
            return
        }

        val grupo = findViewById<ChipGroup>(R.id.chipsAlias)
        val alias = grupo.findViewById<Chip>(grupo.checkedChipId)?.text?.toString() ?: "Casa"
        val datos = DireccionGuardar(
            alias = alias,
            nombreRecibe = nombre,
            telefono = telefono,
            calle = calle,
            numeroExterior = exterior,
            numeroInterior = campo(R.id.etDirInterior).valor().ifBlank { null },
            colonia = colonia,
            ciudad = ciudad,
            estado = estado,
            codigoPostal = cp,
            referencias = campo(R.id.etDirReferencias).valor().ifBlank { null },
            esPredeterminada = findViewById<CheckBox>(R.id.cbDirPredeterminada).isChecked,
            latitud = latitud,
            longitud = longitud
        )

        val boton = findViewById<MaterialButton>(R.id.btnGuardarDireccion)
        boton.ocultarTeclado()
        boton.isEnabled = false
        lifecycleScope.launch {
            val id = existente?.id
            val r = api { if (id == null) crearDireccion(datos) else actualizarDireccion(id, datos) }
            when (r) {
                is Resultado.Ok -> {
                    toast("Dirección guardada")
                    setResult(RESULT_OK)
                    finish()
                }
                is Resultado.Error -> {
                    toast(r.mensaje)
                    boton.isEnabled = true
                }
            }
        }
    }
}
