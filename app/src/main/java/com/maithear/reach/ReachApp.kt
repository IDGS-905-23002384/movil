package com.maithear.reach

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.maithear.reach.auth.MainActivity
import com.maithear.reach.core.ApiClient
import com.maithear.reach.core.Sesion
import com.maithear.reach.mapas.Mapas
import com.maithear.reach.notificaciones.Notificador
import com.maithear.reach.notificaciones.ProgramadorNotificaciones

class ReachApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Sesion.iniciar(this)
        Notificador.crearCanales(this)
        Mapas.iniciar(this)
        ApiClient.alExpirarSesion = {
            ProgramadorNotificaciones.cancelar(this)
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(MainActivity.EXTRA_EXPIRADA, true)
            startActivity(intent)
        }
    }
}
