// HomeActivity.kt
package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentoDao
import com.example.foodtracker.modelos.AlimentosAdapter
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
    private var todosLosAlimentos: List<Alimento> = listOf()

    private fun programarNotificacionesDiarias() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(10, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_food_check",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }

    private fun scheduleDailyWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        listView = findViewById(R.id.listViewAlimentos)

        val db = AppDatabase.getDatabase(this)
        alimentoDao = db.alimentoDao()

        alimentosAdapter = AlimentosAdapter(this, mutableListOf())
        listView.adapter = alimentosAdapter

        observarAlimentos()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        findViewById<ImageView>(R.id.imgCarnes).setOnClickListener {
            filtrarPorCategoria("Carnes")
        }

        findViewById<ImageView>(R.id.imgLacteos).setOnClickListener {
            filtrarPorCategoria("Lácteos")
        }

        findViewById<ImageView>(R.id.imgFrutas).setOnClickListener {
            filtrarPorCategoria("Frutas")
        }

        findViewById<ImageView>(R.id.imgVerduras).setOnClickListener {
            filtrarPorCategoria("Verduras")
        }

        findViewById<ImageView>(R.id.imgOtros).setOnClickListener {
            filtrarPorCategoria("Otros")
        }

        findViewById<ImageView>(R.id.navHome).setOnClickListener {
            alimentosAdapter.updateList(todosLosAlimentos)
            listView.smoothScrollToPosition(0)
        }

        findViewById<ImageView>(R.id.navAgregar).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<ImageView>(R.id.navEstadisticas).setOnClickListener {
            startActivity(Intent(this, EstadisticasActivity::class.java))
        }

        findViewById<ImageView>(R.id.navNotificaciones).setOnClickListener {
            startActivity(Intent(this, NovedadesActivity::class.java))
        }

        programarNotificacionesDiarias()
        scheduleDailyWorker()
    }

    private fun observarAlimentos() {
        lifecycleScope.launch {
            alimentoDao.getAllAlimentos().collect { alimentos ->
                todosLosAlimentos = alimentos
                alimentosAdapter.updateList(alimentos)

                listView.setOnItemClickListener { _, _, position, _ ->
                    val alimento = alimentosAdapter.getItem(position) as Alimento
                    val intent = Intent(this@HomeActivity, EditarAlimentoActivity::class.java)
                    intent.putExtra("alimento_id", alimento.id)
                    startActivityForResult(intent, EDITAR_ALIMENTO_REQUEST)
                }
            }
        }
    }

    private fun filtrarPorCategoria(categoria: String) {
        val filtrados = todosLosAlimentos.filter { it.categoria == categoria }
        alimentosAdapter.updateList(filtrados)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Opcional: recargar lista si hubo cambios
        }
    }
}
