package com.maithear.reach.auth

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.maithear.reach.R
import com.maithear.reach.admin.AdminActivity
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Campo
import com.maithear.reach.core.LoginRequest
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.dialogoFormulario
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.ocultarTeclado
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.valor
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    companion object {
        const val EXTRA_RETORNO = "retorno"
        const val EXTRA_EXPIRADA = "expirada"
    }

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRecordar: CheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var progreso: LinearProgressIndicator
    private var passwordVisible = false

    private val esRetorno: Boolean
        get() = intent.getBooleanExtra(EXTRA_RETORNO, false)

    private val registro = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) sesionIniciada()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!esRetorno && Sesion.activa) {
            abrirInicio()
            return
        }
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.layoutBrand).insetsSistema(arriba = true)
        findViewById<View>(R.id.tvExplorar).insetsSistema(arriba = true)
        findViewById<View>(R.id.cardForm).insetsSistema(abajo = true)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRecordar = findViewById(R.id.cbRemember)
        btnLogin = findViewById(R.id.btnLogin)
        progreso = findViewById(R.id.progresoLogin)

        Sesion.ultimoEmail?.let { etEmail.setText(it) }
        if (intent.getBooleanExtra(EXTRA_EXPIRADA, false)) toast("Tu sesión expiró. Inicia sesión de nuevo.")

        ObjectAnimator.ofFloat(findViewById<View>(R.id.flOrbitContainer), View.TRANSLATION_Y, 0f, -12f, 0f).apply {
            duration = 3500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        val ivToggle = findViewById<ImageView>(R.id.ivTogglePassword)
        ivToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            ivToggle.alpha = if (passwordVisible) 1f else 0.5f
            etPassword.setSelection(etPassword.text.length)
        }

        etPassword.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                iniciarSesion()
                true
            } else {
                false
            }
        }

        btnLogin.setOnClickListener { iniciarSesion() }

        findViewById<TextView>(R.id.tvCrearCuenta).setOnClickListener {
            registro.launch(Intent(this, RegistroActivity::class.java))
        }

        findViewById<TextView>(R.id.tvExplorar).setOnClickListener {
            if (esRetorno) {
                finish()
            } else {
                startActivity(Intent(this, TiendaActivity::class.java))
                finish()
            }
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Recupera tu acceso")
                .setMessage("Escríbenos a soporte@reach.mx desde el correo de tu cuenta y te ayudaremos a restablecer tu contraseña.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<ImageView>(R.id.ivLogo).setOnLongClickListener {
            configurarServidor()
            true
        }
    }

    private fun iniciarSesion() {
        val email = etEmail.valor()
        val password = etPassword.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Escribe un correo válido"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Escribe tu contraseña"
            return
        }
        btnLogin.ocultarTeclado()
        cargando(true)
        lifecycleScope.launch {
            api { login(LoginRequest(email, password)) }
                .siOk {
                    Sesion.guardar(it, cbRecordar.isChecked)
                    sesionIniciada()
                }
                .siError { toast(it.mensaje) }
            cargando(false)
        }
    }

    private fun cargando(activo: Boolean) {
        btnLogin.isEnabled = !activo
        btnLogin.text = if (activo) "Verificando…" else "Iniciar sesión"
        progreso.visibility = if (activo) View.VISIBLE else View.INVISIBLE
    }

    private fun sesionIniciada() {
        if (esRetorno && !Sesion.esPersonal) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            abrirInicio()
        }
    }

    private fun abrirInicio() {
        val destino = if (Sesion.esPersonal) AdminActivity::class.java else TiendaActivity::class.java
        startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun configurarServidor() {
        dialogoFormulario(
            titulo = "Servidor de la API",
            mensaje = "Dirección del backend (IP de tu equipo y puerto). Ejemplo: http://192.168.1.9:5133/",
            campos = listOf(Campo("url", "URL base", Sesion.baseUrl, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        ) { valores, dialogo ->
            val url = valores["url"].orEmpty()
            if (url.isBlank()) {
                toast("Escribe una URL")
            } else {
                Sesion.baseUrl = url
                toast("Servidor actualizado")
                dialogo.dismiss()
            }
        }
    }
}
