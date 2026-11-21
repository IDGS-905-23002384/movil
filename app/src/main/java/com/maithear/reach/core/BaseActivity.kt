package com.maithear.reach.core

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.maithear.reach.R
import com.maithear.reach.notificaciones.Notificador
import com.maithear.reach.notificaciones.ProgramadorNotificaciones

abstract class BaseActivity : AppCompatActivity() {

    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) ProgramadorNotificaciones.revisarAhora(this)
    }

    protected fun prepararNotificaciones() {
        if (!Sesion.activa) return
        ProgramadorNotificaciones.programar(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificador.tienePermiso(this) && !Sesion.permisoNotificacionesSolicitado) {
            Sesion.permisoNotificacionesSolicitado = true
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
    }

    protected fun configurarHeader(titulo: String, eyebrow: String? = null) {
        findViewById<View>(R.id.header).insetsSistema(arriba = true)
        findViewById<TextView>(R.id.tvHeaderTitulo).text = titulo
        val tvEyebrow = findViewById<TextView>(R.id.tvHeaderEyebrow)
        tvEyebrow.text = eyebrow ?: ""
        tvEyebrow.visible(!eyebrow.isNullOrBlank())
        findViewById<ImageButton>(R.id.btnAtras).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    protected fun accionHeader(icono: Int, descripcion: String, accion: () -> Unit) {
        val boton = findViewById<ImageButton>(R.id.btnHeaderAccion)
        boton.setImageResource(icono)
        boton.contentDescription = descripcion
        boton.visible(true)
        boton.setOnClickListener { accion() }
    }
}
