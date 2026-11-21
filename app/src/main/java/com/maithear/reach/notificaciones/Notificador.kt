package com.maithear.reach.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.maithear.reach.R
import com.maithear.reach.core.Notificacion

object Notificador {
    const val CANAL_PEDIDOS = "reach_pedidos"
    const val CANAL_TIENDA = "reach_tienda"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CANAL_PEDIDOS, "Mis pedidos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Confirmación, ensamble, envío y entrega de tus pedidos"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_TIENDA, "Tienda e inventario", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos generales, nuevos pedidos y stock bajo"
            }
        )
    }

    fun tienePermiso(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun mostrar(context: Context, n: Notificacion) {
        if (!tienePermiso(context)) return
        val intent = Intent(context, NotificacionesActivity::class.java)
            .putExtra(NotificacionesActivity.EXTRA_NOTIFICACION_ID, n.id)
            .putExtra(NotificacionesActivity.EXTRA_PEDIDO_ID, n.pedidoId ?: 0)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendiente = PendingIntent.getActivity(
            context, n.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canal = if (n.tipo == "Pedido") CANAL_PEDIDOS else CANAL_TIENDA
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(context, R.color.azul))
            .setContentTitle(n.titulo)
            .setContentText(n.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.mensaje))
            .setPriority(if (canal == CANAL_PEDIDOS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (n.tipo == "Pedido") NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(n.id, notificacion)
        } catch (e: SecurityException) {
            return
        }
    }

    fun cancelar(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun limpiar(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
