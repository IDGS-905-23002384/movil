package com.maithear.reach.cuenta

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direccion_form)
        existente = intent.getStringExtra(EXTRA_DIRECCION)?.let { Gson().fromJson(it, Direccion::class.java) }
        configurarHeader(if (existente == null) "Nueva dirección" else "Editar dirección", "Entregas")
        findViewById<View>(R.id.barraDireccion).insetsSistema(abajo = true)

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
        }

        findViewById<MaterialButton>(R.id.btnGuardarDireccion).setOnClickListener { guardar() }
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
            esPredeterminada = findViewById<CheckBox>(R.id.cbDirPredeterminada).isChecked
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
