package com.example.foodtracker

import DailyCheckWorker
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentosAdapter
import com.example.foodtracker.modelos.AlimentoDao
import com.example.foodtracker.modelos.AppDatabase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    companion object {
        const val EDITAR_ALIMENTO_REQUEST = 1
    }
    private lateinit var listView: ListView
    private lateinit var alimentoDao: AlimentoDao
    private lateinit var alimentosAdapter: AlimentosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("listaAlimentos").apply()
        // Inicializar vistas
        listView = findViewById(R.id.listViewAlimentos)
        val btnAgregarAlimento = findViewById<Button>(R.id.btnAgregarAlimento)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val btnVerEstadisticas = findViewById<Button>(R.id.btnVerEstadisticas)

        // Inicializar Room
        val db = AppDatabase.getDatabase(this)
        alimentoDao = db.alimentoDao()

        // Configurar adaptador
        alimentosAdapter = AlimentosAdapter(this, mutableListOf())
        listView.adapter = alimentosAdapter

        // Observar cambios en la base de datos
        observarAlimentos()

        // Solicitar permisos de notificación (si es necesario)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Configurar listeners
        btnAgregarAlimento.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val testRequest = OneTimeWorkRequestBuilder<DailyCheckWorker>()
                .setInitialDelay(1, TimeUnit.SECONDS) // Para pruebas inmediatas
                .build()

            WorkManager.getInstance(this).enqueue(testRequest)
            Toast.makeText(this, "Verificando alimentos...", Toast.LENGTH_SHORT).show()
        }

        btnVerEstadisticas.setOnClickListener {
            startActivity(Intent(this, EstadisticasActivity::class.java))
        }

        // Programar chequeo diario
        scheduleDailyCheck()
    }

    private fun observarAlimentos() {
        lifecycleScope.launch {
            alimentoDao.getAllAlimentos().collect { alimentos ->
                alimentosAdapter.updateList(alimentos)

                // Configurar clics en los items
                listView.setOnItemClickListener { _, _, position, _ ->
                    val alimento = alimentos[position]
                    val intent = Intent(this@HomeActivity, EditarAlimentoActivity::class.java).apply {
                        putExtra("alimento_id", alimento.id) // Envía solo el ID
                    }
                    startActivityForResult(intent, EDITAR_ALIMENTO_REQUEST)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            // Manejar resultados de edición/eliminación si es necesario
        }
    }

    @OptIn(UnstableApi::class)
    private fun scheduleDailyCheck() {
        val workManager = WorkManager.getInstance(this)

        // Configuración mejorada del trabajo periódico
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyCheckRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(
            24, // Cada 24 horas
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        // Política para evitar duplicados
        workManager.enqueueUniquePeriodicWork(
            "daily_food_check",
            ExistingPeriodicWorkPolicy.UPDATE, // Actualiza si ya existe
            dailyCheckRequest
        )
    }
}
