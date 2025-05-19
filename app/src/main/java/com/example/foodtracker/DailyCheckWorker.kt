package com.example.foodtracker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.modelos.AppDatabase
import com.example.foodtracker.modelos.Alimento
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationHelper = NotificationHelper(context)

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ISO_DATE

    override suspend fun doWork(): Result {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                checkExpiringFoods() // Ejecuta el chequeo solo en Android 8+
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Error en doWork", e)
            Result.retry() // Si falla, WorkManager lo intenta de nuevo
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun checkExpiringFoods() {
        val db = AppDatabase.getDatabase(applicationContext)
        val hoy = LocalDate.now()
        val limite = hoy.plusDays(7) // Límite de 7 días para alerta

        val alimentosProximos = db.alimentoDao().getAllForStats().filter {
            try {
                val fecha = LocalDate.parse(it.fechaCaducidad, dateFormatter)
                val diasRestantes = ChronoUnit.DAYS.between(hoy, fecha)
                diasRestantes in 0..7 && !it.notificado // evita duplicados
            } catch (e: Exception) {
                false // Ignora si hay error en fecha
            }
        }

        alimentosProximos.forEach { alimento ->
            notifyFood(alimento) // Envía la notificación
            db.alimentoDao().update(alimento.copy(notificado = true)) // ← actualiza como notificado
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notifyFood(alimento: Alimento) {
        try {
            val fechaCaducidad = LocalDate.parse(alimento.fechaCaducidad, dateFormatter)
            val diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad)

            val (title, message) = when {
                diasRestantes <= 0 -> "¡Alimento caducado!" to "${alimento.nombre} ha caducado"
                diasRestantes == 1L -> "¡Alerta urgente!" to "${alimento.nombre} caduca mañana"
                diasRestantes <= 3 -> "¡Atención!" to "${alimento.nombre} caduca en $diasRestantes días"
                else -> "Recordatorio" to "${alimento.nombre} caduca pronto (en $diasRestantes días)"
            }

            notificationHelper.showNotification(title, message)

        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Error al procesar ${alimento.nombre}", e)
        }
    }
}
