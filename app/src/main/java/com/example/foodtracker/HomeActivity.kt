package com.example.foodtracker

import DailyCheckWorker
import NotificationHelper
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentosAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var alimentosAdapter: AlimentosAdapter
    private lateinit var alimentosList: MutableList<Alimento>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        listView = findViewById(R.id.listViewAlimentos)
        sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)

        // Cargar alimentos desde SharedPreferences
        cargarAlimentos()

        // Configurar adaptador
        alimentosList = cargarAlimentos() // Carga los datos iniciales
        alimentosAdapter = AlimentosAdapter(this, alimentosList)
        listView.adapter = alimentosAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, EditarAlimentoActivity::class.java).apply {
                putExtra("alimento", alimentosList[position]) // Envía el objeto serializable directamente
                putExtra("position", position)
            }
            startActivityForResult(intent, 1)
        }
        val btnAgregarAlimento = findViewById<Button>(R.id.btnAgregarAlimento)
        btnAgregarAlimento.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        scheduleDailyCheck()
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val testRequest = OneTimeWorkRequestBuilder<DailyCheckWorker>().build()
            WorkManager.getInstance(this).enqueue(testRequest)
            Toast.makeText(this, "Worker ejecutado", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnVerEstadisticas).setOnClickListener {
            startActivity(Intent(this, EstadisticasActivity::class.java))
        }
    }
    override fun onResume() {
        super.onResume()
        cargarAlimentos()
        alimentosAdapter.notifyDataSetChanged()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Caso 1: Resultado exitoso y datos no nulos
        if (resultCode == Activity.RESULT_OK && data != null) {

                // --- Caso A: Alimento editado ---
                if (data.hasExtra("alimentoEditado") && data.hasExtra("position")) {
                    val alimentoEditado = data.getSerializableExtra("alimentoEditado") as? Alimento
                    val position = data.getIntExtra("position", -1)

                    if (position != -1 && alimentoEditado != null && position < alimentosList.size) {
                        alimentosList[position] = alimentoEditado
                        alimentosAdapter.notifyDataSetChanged()
                        guardarAlimentos()
                        Toast.makeText(this, "Alimento actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: Posición o datos inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
                // --- Caso B: Alimento eliminado ---
                else if (data.hasExtra("posicionEliminar")) {
                    val position = data.getIntExtra("posicionEliminar", -1)

                    if (position != -1 && position < alimentosList.size) {
                        alimentosList.removeAt(position)
                        alimentosAdapter.notifyDataSetChanged()
                        guardarAlimentos()
                        Toast.makeText(this, "Alimento eliminado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: No se pudo eliminar", Toast.LENGTH_SHORT).show()
                    }
                }

        }
        // Caso 2: Resultado cancelado por el usuario
        else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Edición cancelada", Toast.LENGTH_SHORT).show()
        }
    }
    private fun cargarAlimentos(): MutableList<Alimento> {
        val json = sharedPreferences.getString("listaAlimentos", null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<MutableList<Alimento>>() {}.type)
        } else {
            mutableListOf()
        }
    }
    private fun guardarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(alimentosList)  // Convierte la lista a JSON
        editor.putString("listaAlimentos", json)  // Usa la misma clave que en MainActivity
        editor.apply()  // Guarda los cambios
    }

    @OptIn(UnstableApi::class)
    private fun scheduleDailyCheck() {
        val workManager = WorkManager.getInstance(this)

        // Verifica si ya existe una tarea programada con el mismo nombre
        workManager.getWorkInfosForUniqueWorkLiveData("dailyFoodCheck").observe(this) { workInfos ->
            if (workInfos.isNullOrEmpty()) { // Si no hay tarea existente, la crea
                val dailyCheckRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(
                    24, // Cada 24 horas
                    TimeUnit.HOURS
                ).setInitialDelay(1, TimeUnit.MINUTES) // Espera 1 minuto antes de la primera ejecución (para pruebas)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "dailyFoodCheck",
                    ExistingPeriodicWorkPolicy.KEEP, // Evita duplicados
                    dailyCheckRequest
                )

                Log.d("WorkManager", "Tarea programada para revisión diaria")
            }
        }
    }
}
