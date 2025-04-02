package com.example.foodtracker

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodtracker.modelos.Alimento
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class MainActivity : AppCompatActivity() {

    private val listaAlimentos = mutableListOf<Alimento>()  // Lista de alimentos
    private lateinit var categoriaSeleccionada: String
    private var fechaCaducidadSeleccionada: String = ""
    private lateinit var campoCantidad: EditText
    private lateinit var spinnerUnidad: Spinner
    private var unidadSeleccionada: String = "Paquete"

    private fun guardarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val json = gson.toJson(listaAlimentos)  // Convertimos la lista a JSON

        editor.putString("listaAlimentos", json)  // Guardamos el JSON
        editor.apply()  // Guardamos los cambios
    }
    private fun cargarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("listaAlimentos", null)

        if (json != null) {
            val type = object : TypeToken<MutableList<Alimento>>() {}.type
            listaAlimentos.clear()
            listaAlimentos.addAll(gson.fromJson(json, type))  // Convertimos el JSON de vuelta a una lista
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cargarAlimentos()
        // Referencias a los elementos de la interfaz
        val campoNombre = findViewById<EditText>(R.id.campoNombre)
        val botonRegresar = findViewById<Button>(R.id.botonRegresar)
        val botonAgregar = findViewById<Button>(R.id.botonAgregar)
        val listaVistaAlimentos = findViewById<ListView>(R.id.listaVistaAlimentos)
        val botonSeleccionarFecha = findViewById<Button>(R.id.botonSeleccionarFecha)
        val textoFecha = findViewById<TextView>(R.id.textoFecha)
        val spinnerCategoria = findViewById<Spinner>(R.id.spinnerCategoria)

        campoCantidad = findViewById(R.id.campoCantidad)
        spinnerUnidad = findViewById(R.id.spinnerUnidad)

        // Configurar opciones del Spinner
        val unidades = arrayOf("Paquete", "Gramos", "Mililitros", "Litros", "Piezas")
        val adaptadorUnidades = ArrayAdapter(this, android.R.layout.simple_spinner_item, unidades)
        adaptadorUnidades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnidad.adapter = adaptadorUnidades

        // Capturar selección del Spinner
        spinnerUnidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                unidadSeleccionada = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Configuración del Spinner (categoría)
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

        // Configuración del DatePicker
        botonSeleccionarFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val anio = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                fechaCaducidadSeleccionada = "$year-${month + 1}-$dayOfMonth"
                textoFecha.text = "Fecha: $fechaCaducidadSeleccionada"
            }, anio, mes, dia)

            datePicker.show()
        }

        // Adaptador para mostrar los alimentos en la lista
        val adaptador = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaAlimentos.map { it.nombre })
        listaVistaAlimentos.adapter = adaptador
        //Botón para regresar al inicio
        botonRegresar.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Botón para agregar alimentos
        botonAgregar.setOnClickListener {
            val nombreAlimento = campoNombre.text.toString()
            val cantidadTexto = campoCantidad.text.toString()

            if (nombreAlimento.isNotEmpty() && fechaCaducidadSeleccionada.isNotEmpty() && cantidadTexto.isNotEmpty()) {
                val cantidad = cantidadTexto.toDoubleOrNull() ?: 0.0  // Convertir a número
                val nuevoAlimento = Alimento(nombreAlimento, fechaCaducidadSeleccionada, categoriaSeleccionada, cantidad, unidadSeleccionada)
                listaAlimentos.add(nuevoAlimento)

                adaptador.clear()
                adaptador.addAll(listaAlimentos.map { "${it.nombre} - ${it.cantidad} ${it.unidad}" })
                adaptador.notifyDataSetChanged()

                guardarAlimentos()

                campoNombre.text.clear()
                campoCantidad.text.clear()
                textoFecha.text = "Fecha no seleccionada"
                fechaCaducidadSeleccionada = ""
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
            guardarAlimentos()
            Toast.makeText(this, "Alimento agregado", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

