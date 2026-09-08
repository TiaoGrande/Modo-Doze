package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_SERVICE_ID = "doze_service_channel"
    const val CHANNEL_SESSION_ID = "doze_session_channel"
    const val NOTIFICATION_SERVICE_ID = 1001
    const val NOTIFICATION_SESSION_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Serviço Modo Doze",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status em tempo real do Modo Doze Definitivo"
                setShowBadge(false)
            }

            val sessionChannel = NotificationChannel(
                CHANNEL_SESSION_ID,
                "Relatórios de Hibernação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Diagnóstico de economia de bateria ao ligar a tela"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(sessionChannel)
        }
    }

    fun buildForegroundNotification(context: Context, statusMessage: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setContentTitle("Modo Doze Definitivo Ativo")
            .setContentText(statusMessage)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showSessionSummaryNotification(
        context: Context,
        durationMinutes: Long,
        batteryDeltaPct: Int,
        batteryStart: Int,
        batteryEnd: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        val durationFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deltaText = if (batteryDeltaPct <= 0) "Consumo: 0% (Economia Máxima!)" else "Consumo: $batteryDeltaPct% ($batteryStart% → $batteryEnd%)"

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION_ID)
            .setContentTitle("Sessão Doze Concluída")
            .setContentText("$durationFormatted em sono profundo • $deltaText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "O aparelho permaneceu $durationFormatted em Sono Profundo (Deep Doze).\n" +
                            "Bateria: $batteryStart% no bloqueio → $batteryEnd% ao desbloquear ($deltaText).\n" +
                            "Reconexão escalonada das antenas realizada com sucesso sem picos de CPU."
                )
            )
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_SESSION_ID, notification)
    }
}
