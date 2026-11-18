package com.maithear.reach.tienda

import android.app.Activity
import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import com.maithear.reach.R
import com.maithear.reach.core.AgregarCarrito
import com.maithear.reach.core.Carrito
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.api
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.toast

object AccionesCarrito {

    suspend fun agregar(activity: Activity, productoId: Int, cantidad: Int = 1, nombre: String? = null): Boolean {
        if (!activity.requiereSesion()) return false
        return when (val r = api { agregarCarrito(AgregarCarrito(productoId, cantidad)) }) {
            is Resultado.Ok -> {
                notificar(activity, r.dato, if (nombre != null) "$nombre agregado al carrito" else "Agregado al carrito")
                true
            }
            is Resultado.Error -> {
                activity.toast(r.mensaje)
                false
            }
        }
    }

    suspend fun agregarPrearmado(activity: Activity, prearmadoId: Int): Boolean {
        if (!activity.requiereSesion()) return false
        return when (val r = api { agregarPrearmado(prearmadoId) }) {
            is Resultado.Ok -> {
                notificar(activity, r.dato, "Equipo agregado al carrito")
                true
            }
            is Resultado.Error -> {
                activity.toast(r.mensaje)
                false
            }
        }
    }

    fun notificar(activity: Activity, carrito: Carrito, mensaje: String) {
        (activity as? TiendaActivity)?.actualizarBadge(carrito.totalArticulos)
        val raiz = activity.findViewById<android.view.View>(android.R.id.content)
        Snackbar.make(raiz, mensaje, Snackbar.LENGTH_LONG)
            .setAction("Ver carrito") { abrirCarrito(activity) }
            .show()
    }

    fun abrirCarrito(activity: Activity) {
        if (activity is TiendaActivity) {
            activity.irA(R.id.nav_carrito)
        } else {
            activity.startActivity(
                Intent(activity, TiendaActivity::class.java)
                    .putExtra(TiendaActivity.EXTRA_TAB, R.id.nav_carrito)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}
