package com.example.foodtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.foodtracker.modelos.AppDatabase
import com.example.foodtracker.modelos.Notificacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHelper(private val context: Context) {
    private val channelId = "food_expiry_channel"

    init {
        createNotificationChannel()
    }

    fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Verifica permisos para Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }

        guardarNotificacionEnBaseDeDatos(title, message)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de caducidad"
            val descriptionText = "Notificaciones sobre alimentos próximos a caducar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun guardarNotificacionEnBaseDeDatos(title: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).notificacionDao()
                val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                dao.insert(Notificacion(titulo = title, mensaje = message, fecha = ahora))
                Log.d("NotificationHelper", "Notificación guardada: $title - $message")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error guardando notificación", e)
            }
        }
    }
}
