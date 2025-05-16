import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.modelos.Alimento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationHelper = NotificationHelper(context)
    private val gson = Gson()

    @SuppressLint("NewApi") // Usaremos verificaciones manuales para APIs antiguas
    override suspend fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("FoodTrackerPrefs", Context.MODE_PRIVATE)
            val json = sharedPreferences.getString("listaAlimentos", null) ?: return Result.success()

            // Corrección clave: Deserialización correcta con TypeToken
            val typeToken = object : TypeToken<List<Alimento>>() {}.type
            val alimentos: List<Alimento> = gson.fromJson(json, typeToken)

            // Verificar si el dispositivo soporta LocalDate (API 26+)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                checkExpiringFoods(alimentos)
                Result.success()
            } else {
                // Alternativa para versiones antiguas (puedes implementar otra lógica)
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Error en doWork", e)
            return Result.retry() // Reintenta si hay error
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkExpiringFoods(alimentos: List<Alimento>) {
        val hoy = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")

        alimentos.forEach { alimento ->
            try {
                val fechaCaducidad = LocalDate.parse(alimento.fechaCaducidad, formatter)
                val diasRestantes = ChronoUnit.DAYS.between(hoy, fechaCaducidad)

                when {
                    diasRestantes == 1L -> {
                        notificationHelper.showNotification(
                            "¡Alimento por caducar!",
                            "${alimento.nombre} caduca mañana"
                        )
                    }
                    diasRestantes == 0L -> {
                        notificationHelper.showNotification(
                            "¡Alimento caducado!",
                            "${alimento.nombre} caduca hoy"
                        )
                    }
                    diasRestantes in 2..7 -> {
                        notificationHelper.showNotification(
                            "Atención: Alimento próximo a caducar",
                            "${alimento.nombre} caduca en $diasRestantes días"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DailyCheckWorker", "Error al procesar alimento ${alimento.nombre}. Fecha: ${alimento.fechaCaducidad}", e)
            }
        }
    }
}