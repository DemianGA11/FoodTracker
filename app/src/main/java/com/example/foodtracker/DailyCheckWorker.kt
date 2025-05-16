import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AppDatabase
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
                checkExpiringFoods()
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Error en doWork", e)
            Result.retry()
        }
    }

    @SuppressLint("NewApi")
    private suspend fun checkExpiringFoods() {
        val db = AppDatabase.getDatabase(applicationContext)
        val hoy = LocalDate.now().toString()
        val limite = LocalDate.now().plusDays(7).toString()

        val alimentosProximos = db.alimentoDao().getAlimentosProximosACaducar(hoy, limite)

        alimentosProximos.forEach { alimento ->
            if (!alimento.notificado) {
                notifyFood(alimento)
                db.alimentoDao().update(alimento.copy(notificado = true))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notifyFood(alimento: Alimento) {
        try {
            val fechaCaducidad = LocalDate.parse(alimento.fechaCaducidad, dateFormatter)
            val diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad)

            when {
                diasRestantes <= 0 -> notificationHelper.showNotification(
                    "¡Alimento caducado!",
                    "${alimento.nombre} ha caducado"
                )
                diasRestantes <= 1 -> notificationHelper.showNotification(
                    "¡Alerta urgente!",
                    "${alimento.nombre} caduca mañana"
                )
                diasRestantes <= 3 -> notificationHelper.showNotification(
                    "¡Atención!",
                    "${alimento.nombre} caduca en $diasRestantes días"
                )
                diasRestantes <= 7 -> notificationHelper.showNotification(
                    "Recordatorio",
                    "${alimento.nombre} caduca pronto (en $diasRestantes días)"
                )
            }
        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Error al procesar ${alimento.nombre}", e)
        }
    }
}