package com.example.foodtracker

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AppDatabase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        // Inicializar vistas
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        // Configurar botón de volver
        btnVolver.setOnClickListener { finish() }

        // Cargar datos desde Room
        lifecycleScope.launch {
            try {
                val alimentos = AppDatabase.getDatabase(this@EstadisticasActivity)
                    .alimentoDao()
                    .getAllForStats()

                if (alimentos.isEmpty()) {
                    showEmptyState()
                    return@launch
                }

                mostrarGraficos(alimentos)
            } catch (e: Exception) {
                Toast.makeText(
                    this@EstadisticasActivity,
                    "Error al cargar estadísticas",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarGraficos(alimentos: List<Alimento>) {
        mostrarGraficoBarras(alimentos)
        mostrarGraficoPastel(alimentos)
    }

    private fun mostrarGraficoBarras(alimentos: List<Alimento>) {
        // Agrupar por categoría
        val categorias = alimentos.groupBy { it.categoria }
            .mapValues { it.value.size }

        // Crear entradas para el gráfico
        val barEntries = categorias.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        // Configurar dataset
        val barDataSet = BarDataSet(barEntries, "Alimentos por Categoría").apply {
            color = Color.parseColor("#FF5722") // Naranja
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        // Configurar gráfico
        barChart.apply {
            data = BarData(barDataSet)
            description.text = " "
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = categorias.size
                valueFormatter = IndexAxisValueFormatter(categorias.keys.toList())
            }
            animateY(1000)
            legend.isEnabled = false
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarGraficoPastel(alimentos: List<Alimento>) {
        // Contar alimentos próximos a caducar
        val alimentosPorCaducar = alimentos.count { alimento ->
            try {
                val fechaCaducidad = LocalDate.parse(
                    alimento.fechaCaducidad,
                    DateTimeFormatter.ISO_DATE
                )
                ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad) <= 7
            } catch (e: Exception) {
                false // Ignorar alimentos con fechas inválidas
            }
        }

        // Crear entradas para el gráfico
        val pieEntries = listOf(
            PieEntry(alimentosPorCaducar.toFloat(), "Por caducar (≤7 días)"),
            PieEntry((alimentos.size - alimentosPorCaducar).toFloat(), "Seguros")
        )

        // Configurar dataset
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = listOf(
                Color.parseColor("#FF5252"), // Rojo
                Color.parseColor("#4CAF50")   // Verde
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        // Configurar gráfico
        pieChart.apply {
            data = PieData(pieDataSet)
            description.isEnabled = false
            centerText = "Estado de caducidad\nTotal: ${alimentos.size}"
            setEntryLabelColor(Color.BLACK)
            animateY(1000)
            legend.isEnabled = false
            invalidate()
        }
    }

    private fun showEmptyState() {
        Toast.makeText(this, "No hay alimentos registrados", Toast.LENGTH_SHORT).show()
        finish()
    }
}