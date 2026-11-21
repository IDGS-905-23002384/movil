package com.maithear.reach.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api

class RevisarNotificacionesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!Sesion.activa) return Result.success()
        val desde = Sesion.ultimaNotificacion()
        if (desde < 0) {
            when (val conteo = api { conteoNotificaciones() }) {
                is Resultado.Ok -> Sesion.guardarUltimaNotificacion(conteo.dato.ultimoId)
                is Resultado.Error -> return if (conteo.codigo == 401) Result.success() else Result.retry()
            }
            return Result.success()
        }
        return when (val r = api { notificacionesNuevas(desde) }) {
            is Resultado.Ok -> {
                val nuevas = r.dato
                nuevas.takeLast(5).forEach { Notificador.mostrar(applicationContext, it) }
                nuevas.maxOfOrNull { it.id }?.let { Sesion.guardarUltimaNotificacion(it) }
                Result.success()
            }
            is Resultado.Error -> if (r.codigo == 401 || r.codigo == 403) Result.success() else Result.retry()
        }
    }
}
