package com.maithear.reach.auth

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.RegistroRequest
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import kotlinx.coroutines.launch

class RegistroActivity : BaseActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var cbTerminos: CheckBox
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var progreso: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        findViewById<View>(R.id.headerRegistro).insetsSistema(arriba = true)
        findViewById<View>(R.id.scrollRegistro).insetsSistema(abajo = true)

        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etPassword = findViewById(R.id.etPassword)
        etConfirmar = findViewById(R.id.etConfirmar)
        cbTerminos = findViewById(R.id.cbTerminos)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progreso = findViewById(R.id.progresoRegistro)

        findViewById<ImageButton>(R.id.btnAtras).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvIrLogin).setOnClickListener { finish() }
        btnRegistrar.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nombre = etNombre.valor()
        val email = etEmail.valor()
        val telefono = etTelefono.valor()
        val password = etPassword.text.toString()

        when {
            nombre.length < 2 -> { etNombre.error = "Escribe tu nombre"; return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { etEmail.error = "Correo no válido"; return }
            telefono.isNotEmpty() && !Regex("^\\d{10}$").matches(telefono) -> { etTelefono.error = "Debe tener 10 dígitos"; return }
            password.length < 8 || !password.any { it.isLetter() } || !password.any { it.isDigit() } -> {
                etPassword.error = "Mínimo 8 caracteres con letras y números"; return
            }
            password != etConfirmar.text.toString() -> { etConfirmar.error = "Las contraseñas no coinciden"; return }
            !cbTerminos.isChecked -> { toast("Acepta el aviso de privacidad para continuar"); return }
        }

        btnRegistrar.ocultarTeclado()
        btnRegistrar.isEnabled = false
        progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            api { registro(RegistroRequest(nombre, email, telefono.ifEmpty { null }, password)) }
                .siOk {
                    Sesion.guardar(it, true)
                    toast("¡Bienvenido, ${it.usuario.nombre.substringBefore(' ')}!")
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .siError { toast(it.mensaje) }
            btnRegistrar.isEnabled = true
            progreso.visibility = View.INVISIBLE
        }
    }
}
