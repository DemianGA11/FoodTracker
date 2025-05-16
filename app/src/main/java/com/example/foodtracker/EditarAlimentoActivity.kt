package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentoDao
import com.example.foodtracker.modelos.AppDatabase
import kotlinx.coroutines.launch

class EditarAlimentoActivity : AppCompatActivity() {

    private lateinit var alimentoDao: AlimentoDao
    private lateinit var alimentoActual: Alimento

    // Views
    private lateinit var edtNombre: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var spnUnidad: Spinner
    private lateinit var spnCategoria: Spinner
    private lateinit var txtFecha: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_alimento)

        // Inicializar Room
        val db = AppDatabase.getDatabase(this)
        alimentoDao = db.alimentoDao()

        // Inicializar vistas
        initViews()

        // Obtener ID del alimento
        val alimentoId = intent.getIntExtra("alimento_id", -1)

        if (alimentoId == -1) {
            Toast.makeText(this, "Error: Alimento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar datos del alimento
        lifecycleScope.launch {
            alimentoDao.getAlimentoById(alimentoId)?.let { alimento ->
                alimentoActual = alimento
                mostrarDatosAlimento(alimento)
            } ?: run {
                Toast.makeText(this@EditarAlimentoActivity, "Alimento no encontrado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initViews() {
        edtNombre = findViewById(R.id.edtNombre)
        edtCantidad = findViewById(R.id.edtCantidad)
        spnUnidad = findViewById(R.id.spnUnidad)
        spnCategoria = findViewById(R.id.spnCategoria)
        txtFecha = findViewById(R.id.txtFechaCaducidad)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEliminar = findViewById(R.id.btnEliminar)

        // Configurar Spinners
        configurarSpinnerUnidad()
        configurarSpinnerCategoria()

        // Configurar botones
        btnGuardar.setOnClickListener { guardarCambios() }
        btnEliminar.setOnClickListener { eliminarAlimento() }
        findViewById<Button>(R.id.btnSumar).setOnClickListener { modificarCantidad(1) }
        findViewById<Button>(R.id.btnRestar).setOnClickListener { modificarCantidad(-1) }
    }

    private fun mostrarDatosAlimento(alimento: Alimento) {
        edtNombre.setText(alimento.nombre)
        edtCantidad.setText(alimento.cantidad.toString())
        txtFecha.text = "Vence: ${alimento.fechaCaducidad}"

        // Seleccionar unidad y categoría en los spinners
        (spnUnidad.adapter as? ArrayAdapter<String>)?.let { adapter ->
            val posUnidad = adapter.getPosition(alimento.unidad)
            if (posUnidad >= 0) spnUnidad.setSelection(posUnidad)
        }

        (spnCategoria.adapter as? ArrayAdapter<String>)?.let { adapter ->
            val posCategoria = adapter.getPosition(alimento.categoria)
            if (posCategoria >= 0) spnCategoria.setSelection(posCategoria)
        }
    }

    private fun configurarSpinnerUnidad() {
        val unidades = arrayOf("Paquete", "Gramos", "Mililitros", "Litros", "Piezas")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unidades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnUnidad.adapter = adapter
    }

    private fun configurarSpinnerCategoria() {
        val categorias = listOf("Lácteos", "Carnes", "Frutas", "Verduras", "Otros")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnCategoria.adapter = adapter
    }

    private fun modificarCantidad(incremento: Int) {
        val cantidadActual = edtCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val nuevaCantidad = cantidadActual + incremento

        if (nuevaCantidad >= 0) {
            edtCantidad.setText(nuevaCantidad.toString())
        } else {
            Toast.makeText(this, "La cantidad no puede ser menor a 0", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarCambios() {
        val nombre = edtNombre.text.toString()
        val cantidad = edtCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val unidad = spnUnidad.selectedItem.toString()
        val categoria = spnCategoria.selectedItem.toString()

        if (nombre.isBlank()) {
            Toast.makeText(this, "Ingrese un nombre válido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val alimentoActualizado = alimentoActual.copy(
                nombre = nombre,
                cantidad = cantidad,
                unidad = unidad,
                categoria = categoria
            )

            alimentoDao.update(alimentoActualizado)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun eliminarAlimento() {
        lifecycleScope.launch {
            alimentoDao.delete(alimentoActual)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}