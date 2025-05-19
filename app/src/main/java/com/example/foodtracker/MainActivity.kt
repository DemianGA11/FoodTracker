package com.example.foodtracker

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.DailyCheckWorker
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentoDao
import com.example.foodtracker.modelos.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.*
import android.os.Build

class MainActivity : AppCompatActivity() {
    private lateinit var alimentoDao: AlimentoDao
    private val listaAlimentos = mutableListOf<Alimento>()
    private lateinit var categoriaSeleccionada: String
    private var fechaCaducidadSeleccionada: String = ""
    private lateinit var campoCantidad: EditText
    private lateinit var spinnerUnidad: Spinner
    private var unidadSeleccionada: String = "Paquete"

    private fun guardarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(listaAlimentos)
        editor.putString("listaAlimentos", json)
        editor.apply()
    }

    private fun cargarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("listaAlimentos", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Alimento>>() {}.type
            listaAlimentos.clear()
            listaAlimentos.addAll(gson.fromJson(json, type))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = AppDatabase.getDatabase(this)
        alimentoDao = db.alimentoDao()

        cargarAlimentos()

        val campoNombre = findViewById<EditText>(R.id.campoNombre)
        val botonRegresar = findViewById<Button>(R.id.botonRegresar)
        val botonAgregar = findViewById<Button>(R.id.botonAgregar)
        //val listaVistaAlimentos = findViewById<ListView>(R.id.listaVistaAlimentos)
        val botonSeleccionarFecha = findViewById<Button>(R.id.botonSeleccionarFecha)
        val textoFecha = findViewById<TextView>(R.id.textoFecha)
        val spinnerCategoria = findViewById<Spinner>(R.id.spinnerCategoria)
        campoCantidad = findViewById(R.id.campoCantidad)
        spinnerUnidad = findViewById(R.id.spinnerUnidad)

        // Unidades
        val unidades = arrayOf("Paquete", "Gramos", "Mililitros", "Litros", "Piezas")
        val adaptadorUnidades = ArrayAdapter(this, android.R.layout.simple_spinner_item, unidades)
        adaptadorUnidades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnidad.adapter = adaptadorUnidades
        spinnerUnidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                unidadSeleccionada = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Categorías
        val categorias = listOf("Lácteos", "Carnes", "Frutas", "Verduras", "Otros")
        val adaptadorSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adaptadorSpinner
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                categoriaSeleccionada = categorias[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // DatePicker
        botonSeleccionarFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val anio = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val mesFormateado = String.format("%02d", month + 1)
                val diaFormateado = String.format("%02d", dayOfMonth)
                fechaCaducidadSeleccionada = "$year-$mesFormateado-$diaFormateado"
                textoFecha.text = "Fecha: $fechaCaducidadSeleccionada"
            }, anio, mes, dia)

            datePicker.show()
        }

        // Lista de alimentos
        val adaptador = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaAlimentos.map { it.nombre })
        //listaVistaAlimentos.adapter = adaptador

        // Botón regresar
        botonRegresar.visibility = View.GONE
        val navHome = findViewById<ImageView>(R.id.navHome)
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Botón agregar alimento
        botonAgregar.setOnClickListener {
            lifecycleScope.launch {
                val nombreAlimento = campoNombre.text.toString()
                val cantidadTexto = campoCantidad.text.toString()

                if (nombreAlimento.isNotEmpty() && cantidadTexto.isNotEmpty() && fechaCaducidadSeleccionada.isNotEmpty()) {
                    val cantidad = cantidadTexto.toDouble()

                    val nuevoAlimento = Alimento(
                        nombre = nombreAlimento.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        fechaCaducidad = fechaCaducidadSeleccionada,
                        categoria = categoriaSeleccionada,
                        cantidad = cantidad,
                        unidad = unidadSeleccionada
                    )

                    guardarAlimento(nuevoAlimento)
                    Toast.makeText(this@MainActivity, "Alimento agregado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Botón Estadísticas
        val navEstadisticas = findViewById<ImageView>(R.id.navEstadisticas)
        navEstadisticas.setOnClickListener {
            val intent = Intent(this, EstadisticasActivity::class.java)
            startActivity(intent)
        }

        // Botón Notificaciones (lanza revisión manual)
        /*val navNotificaciones = findViewById<ImageView>(R.id.navNotificaciones)
        navNotificaciones.setOnClickListener {
            val request = OneTimeWorkRequestBuilder<DailyCheckWorker>().build()
            WorkManager.getInstance(this).enqueue(request)
            Toast.makeText(this, "Verificando alimentos próximos a caducar...", Toast.LENGTH_SHORT).show()
        }*/
        findViewById<ImageView>(R.id.navNotificaciones).setOnClickListener {
            startActivity(Intent(this, NovedadesActivity::class.java))
        }

        // navAgregar no hace nada porque ya estás en esta pantalla
    }

    private suspend fun guardarAlimento(alimento: Alimento) {
        alimentoDao.insert(alimento)

        // Verificar si caduca pronto y notificar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hoy = java.time.LocalDate.now()
                val fecha = java.time.LocalDate.parse(alimento.fechaCaducidad)
                val dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, fecha)

                if (dias in 0..7) {
                    val (titulo, mensaje) = when {
                        dias <= 0 -> "¡Alimento caducado!" to "${alimento.nombre} ha caducado"
                        dias == 1L -> "¡Alerta urgente!" to "${alimento.nombre} caduca mañana"
                        dias <= 3L -> "¡Atención!" to "${alimento.nombre} caduca en $dias días"
                        else -> "Recordatorio" to "${alimento.nombre} caduca pronto (en $dias días)"
                    }

                    NotificationHelper(this).showNotification(titulo, mensaje)

                    // Actualizar como notificado para evitar duplicado
                    alimentoDao.update(alimento.copy(notificado = true))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
