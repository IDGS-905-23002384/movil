package com.maithear.reach.core

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.maithear.reach.R

abstract class BaseActivity : AppCompatActivity() {

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
