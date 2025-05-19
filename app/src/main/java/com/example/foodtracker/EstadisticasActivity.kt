package com.example.foodtracker

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AppDatabase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        // Menú inferior
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navAgregar = findViewById<ImageView>(R.id.navAgregar)
        val navEstadisticas = findViewById<ImageView>(R.id.navEstadisticas)
        val navNotificaciones = findViewById<ImageView>(R.id.navNotificaciones)

        // Resaltar el ícono actual
        navEstadisticas.setColorFilter(Color.parseColor("#FF9800"))

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navAgregar.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        navEstadisticas.setOnClickListener {
            //Toast.makeText(this, "Ya estás en Estadísticas", Toast.LENGTH_SHORT).show()
        }

        /*navNotificaciones.setOnClickListener {
            val testRequest = OneTimeWorkRequestBuilder<DailyCheckWorker>()
                .setInitialDelay(1, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(this).enqueue(testRequest)
            Toast.makeText(this, "Verificando alimentos...", Toast.LENGTH_SHORT).show()
        }*/
        findViewById<ImageView>(R.id.navNotificaciones).setOnClickListener {
            startActivity(Intent(this, NovedadesActivity::class.java))
        }

        // Inicializar vistas
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)

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
        // Agrupar por unidad y sumar cantidades
        val unidades = alimentos.groupBy { it.unidad }
            .mapValues { entry -> entry.value.sumOf { it.cantidad } }

        val barEntries = unidades.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val barDataSet = BarDataSet(barEntries, "Cantidad total por unidad").apply {
            color = Color.parseColor("#4CAF50") // Verde
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val unidadLabels = unidades.keys.toList()

        barChart.apply {
            data = BarData(barDataSet)
            description.text = ""
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = unidadLabels.size
                valueFormatter = IndexAxisValueFormatter(unidadLabels)
            }
            animateY(1000)
            legend.isEnabled = false
            invalidate()

            // Mostrar detalle al hacer clic en una barra
            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    val index = h?.x?.toInt() ?: return
                    val unidadSeleccionada = unidadLabels[index]
                    val alimentosEnUnidad = alimentos.filter { it.unidad == unidadSeleccionada }

                    val builder = androidx.appcompat.app.AlertDialog.Builder(this@EstadisticasActivity)
                        .setTitle("Detalle: $unidadSeleccionada")

                    val contenido = StringBuilder()

                    // Agrupar por categoría
                    val porCategoria = alimentosEnUnidad.groupBy { it.categoria }

                    for ((categoria, lista) in porCategoria) {
                        val total = lista.sumOf { it.cantidad }
                        contenido.append("📦 $categoria: $total $unidadSeleccionada\n")
                        lista.forEach {
                            contenido.append("• ${it.nombre} (${it.cantidad} $unidadSeleccionada)\n")
                        }
                        contenido.append("\n")
                    }

                    val scrollView = android.widget.ScrollView(this@EstadisticasActivity)
                    val textView = android.widget.TextView(this@EstadisticasActivity).apply {
                        text = contenido.toString().trim()
                        setPadding(32, 24, 32, 24)
                        textSize = 16f
                    }

                    scrollView.addView(textView)
                    builder.setView(scrollView)
                    builder.setPositiveButton("Cerrar", null)
                    builder.show()
                }

                override fun onNothingSelected() {}
            })
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarGraficoPastel(alimentos: List<Alimento>) {
        val formatter = DateTimeFormatter.ISO_DATE
        val hoy = LocalDate.now()

        val alimentosPorCaducar = mutableListOf<Alimento>()
        val alimentosSeguros = mutableListOf<Alimento>()

        for (alimento in alimentos) {
            try {
                val fecha = LocalDate.parse(alimento.fechaCaducidad, formatter)
                val diasRestantes = ChronoUnit.DAYS.between(hoy, fecha)
                if (diasRestantes in 0..7) {
                    alimentosPorCaducar.add(alimento)
                } else if (diasRestantes > 7) {
                    alimentosSeguros.add(alimento)
                }
            } catch (e: Exception) {}
        }

        val pieEntries = listOf(
            PieEntry(alimentosPorCaducar.size.toFloat(), "Por caducar (≤7 días)"),
            PieEntry(alimentosSeguros.size.toFloat(), "Seguros")
        )

        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = listOf(
                Color.parseColor("#FF5252"),
                Color.parseColor("#4CAF50")
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

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    val index = h?.x?.toInt() ?: return
                    val lista = if (index == 0) alimentosPorCaducar else alimentosSeguros
                    val titulo = if (index == 0) "Alimentos por caducar" else "Alimentos seguros"

                    if (lista.isEmpty()) {
                        Toast.makeText(this@EstadisticasActivity, "No hay alimentos en esta categoría.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val hoy = LocalDate.now()
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this@EstadisticasActivity)
                    builder.setTitle(titulo)

                    // Agrupar por categoría y construir texto
                    val agrupado = lista.groupBy { it.categoria }
                    val contenido = StringBuilder()

                    for ((categoria, alimentosEnCategoria) in agrupado) {
                        contenido.append("📦 $categoria:\n")
                        for (alimento in alimentosEnCategoria) {
                            try {
                                val fecha = LocalDate.parse(alimento.fechaCaducidad, DateTimeFormatter.ISO_DATE)
                                val dias = ChronoUnit.DAYS.between(hoy, fecha).toInt()
                                val diasTexto = when {
                                    dias < 0 -> "caducado"
                                    dias == 0 -> "caduca hoy"
                                    dias == 1 -> "caduca mañana"
                                    else -> "caduca en $dias días"
                                }
                                contenido.append("• ${alimento.nombre} ($diasTexto)\n")
                            } catch (e: Exception) {
                                contenido.append("• ${alimento.nombre} (fecha inválida)\n")
                            }
                        }
                        contenido.append("\n")
                    }

                    val scrollView = android.widget.ScrollView(this@EstadisticasActivity)
                    val textView = android.widget.TextView(this@EstadisticasActivity).apply {
                        text = contenido.toString().trim()
                        setPadding(32, 24, 32, 24)
                        textSize = 16f
                    }

                    scrollView.addView(textView)
                    builder.setView(scrollView)
                    builder.setPositiveButton("Cerrar", null)
                    builder.show()
                }

                override fun onNothingSelected() {}
            })


        }
    }

    private fun showEmptyState() {
        Toast.makeText(this, "No hay alimentos registrados", Toast.LENGTH_SHORT).show()
        finish()
    }
}

