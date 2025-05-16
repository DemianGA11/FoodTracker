package com.example.foodtracker

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.foodtracker.modelos.Alimento
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EstadisticasActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        val barChart = findViewById<BarChart>(R.id.barChart)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        // 1. Obtener datos de SharedPreferences
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("listaAlimentos", null)

        if (json != null) {
            val typeToken = object : TypeToken<List<Alimento>>() {}.type
            val alimentos = Gson().fromJson<List<Alimento>>(json, typeToken)

            if (alimentos.isEmpty()) {
                Toast.makeText(this, "No hay alimentos registrados", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // =============================================
            // GRÁFICO DE BARRAS (Por categoría)
            // =============================================
            val categorias = alimentos.groupBy { it.categoria }
                .mapValues { it.value.size }

            val barEntries = categorias.entries.mapIndexed { index, entry ->
                BarEntry(index.toFloat(), entry.value.toFloat())
            }

            val barDataSet = BarDataSet(barEntries, "Alimentos por Categoría").apply {
                color = Color.parseColor("#FF5722") // Naranja
                valueTextColor = Color.BLACK
                valueTextSize = 12f
            }

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

            // =============================================
            // GRÁFICO DE DONUT (Estado de caducidad)
            // =============================================
            val alimentosPorCaducar = alimentos.count { alimento ->
                try {
                    val fechaCaducidad = LocalDate.parse(alimento.fechaCaducidad, DateTimeFormatter.ISO_DATE)
                    ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad) <= 7
                } catch (e: Exception) {
                    false // Ignorar alimentos con fechas inválidas
                }
            }

            val pieEntries = listOf(
                PieEntry(alimentosPorCaducar.toFloat(), "Por caducar (≤7 días)"),
                PieEntry((alimentos.size - alimentosPorCaducar).toFloat(), "Seguros")
            )

            val pieDataSet = PieDataSet(pieEntries, "").apply {
                colors = listOf(
                    Color.parseColor("#FF5252"), // Rojo
                    Color.parseColor("#4CAF50")  // Verde
                )
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }

            pieChart.apply {
                data = PieData(pieDataSet)
                description.isEnabled = false
                centerText = "Estado de caducidad\nTotal: ${alimentos.size}"
                setEntryLabelColor(Color.BLACK)
                animateY(1000)
                legend.isEnabled = false
                invalidate()
            }
        } else {
            Toast.makeText(this, "No se encontraron datos", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnVolver.setOnClickListener { finish() }
    }
}