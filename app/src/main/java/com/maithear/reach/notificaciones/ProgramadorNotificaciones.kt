package com.maithear.reach.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maithear.reach.core.Sesion
import java.util.concurrent.TimeUnit

object ProgramadorNotificaciones {
    private const val TRABAJO_PERIODICO = "reach_revisar_notificaciones"
    private const val TRABAJO_INMEDIATO = "reach_revisar_notificaciones_ahora"

    private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun programar(context: Context) {
        if (!Sesion.activa) return
        val periodico = PeriodicWorkRequestBuilder<RevisarNotificacionesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(conRed)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, periodico)
    }

    fun revisarAhora(context: Context) {
        if (!Sesion.activa) return
        val inmediato = OneTimeWorkRequestBuilder<RevisarNotificacionesWorker>()
            .setConstraints(conRed)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.REPLACE, inmediato)
    }

    fun cancelar(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(TRABAJO_PERIODICO)
        manager.cancelUniqueWork(TRABAJO_INMEDIATO)
        Notificador.limpiar(context)
    }
}
