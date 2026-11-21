package com.maithear.reach.notificaciones

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.maithear.reach.R
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.siOk
import com.maithear.reach.core.visible

object Campana {

    fun configurar(raiz: View, activity: Activity) {
        raiz.findViewById<View>(R.id.btnCampana).setOnClickListener {
            if (activity.requiereSesion()) activity.startActivity(Intent(activity, NotificacionesActivity::class.java))
        }
    }

    fun pintar(raiz: View, noLeidas: Int) {
        val contador = raiz.findViewById<TextView>(R.id.tvCampanaConteo)
        contador.text = if (noLeidas > 99) "99+" else noLeidas.toString()
        contador.visible(noLeidas > 0)
        raiz.contentDescription = if (noLeidas > 0) "Notificaciones, $noLeidas sin leer" else "Notificaciones"
    }

    suspend fun actualizar(raiz: View) {
        if (!Sesion.activa) {
            pintar(raiz, 0)
            return
        }
        api { conteoNotificaciones() }.siOk { pintar(raiz, it.noLeidas) }
    }
}
