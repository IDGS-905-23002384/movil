package com.maithear.reach.core

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maithear.reach.R

data class Campo(
    val clave: String,
    val etiqueta: String,
    val valor: String = "",
    val tipo: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
    val pista: String = "",
    val lineas: Int = 1
)

fun Context.dialogoFormulario(
    titulo: String,
    campos: List<Campo>,
    textoAceptar: String = "Guardar",
    mensaje: String? = null,
    alAceptar: (valores: Map<String, String>, dialogo: AlertDialog) -> Unit
) {
    val contenedor = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), 0)
    }
    if (!mensaje.isNullOrBlank()) {
        contenedor.addView(TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(context, R.color.texto_sec))
            textSize = 13f
            setPadding(0, 0, 0, dp(12))
        })
    }
    val entradas = mutableMapOf<String, EditText>()
    for (campo in campos) {
        contenedor.addView(TextView(this).apply {
            text = campo.etiqueta
            setTextColor(ContextCompat.getColor(context, R.color.texto))
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })
        val entrada = EditText(this).apply {
            setText(campo.valor)
            hint = campo.pista
            inputType = if (campo.lineas > 1) campo.tipo or InputType.TYPE_TEXT_FLAG_MULTI_LINE else campo.tipo
            if (campo.lineas > 1) {
                minLines = campo.lineas
                gravity = android.view.Gravity.TOP
            } else {
                maxLines = 1
            }
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.texto))
            setHintTextColor(ContextCompat.getColor(context, R.color.gris))
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14)
            }
        }
        entradas[campo.clave] = entrada
        contenedor.addView(entrada)
    }

    val dialogo = MaterialAlertDialogBuilder(this)
        .setTitle(titulo)
        .setView(contenedor)
        .setPositiveButton(textoAceptar, null)
        .setNegativeButton("Cancelar", null)
        .create()
    dialogo.setOnShowListener {
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            alAceptar(entradas.mapValues { it.value.valor() }, dialogo)
        }
    }
    dialogo.show()
}

fun Context.confirmar(titulo: String, mensaje: String, textoAceptar: String = "Aceptar", alAceptar: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setTitle(titulo)
        .setMessage(mensaje)
        .setPositiveButton(textoAceptar) { _, _ -> alAceptar() }
        .setNegativeButton("Cancelar", null)
        .show()
}
