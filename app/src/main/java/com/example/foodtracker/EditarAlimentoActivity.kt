package com.example.foodtracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AppDatabase
import kotlinx.coroutines.launch
import java.util.*
import java.util.Locale

class EditarAlimentoActivity : AppCompatActivity() {

    private var alimento: Alimento? = null

    private lateinit var campoNombre: EditText
    private lateinit var campoCantidad: EditText
    private lateinit var spinnerUnidad: Spinner
    private lateinit var spinnerCategoria: Spinner
    private lateinit var campoFecha: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnAumentar: Button
    private lateinit var btnDisminuir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_alimento)

        campoNombre = findViewById(R.id.campoNombre)
        campoCantidad = findViewById(R.id.campoCantidad)
        spinnerUnidad = findViewById(R.id.spinnerUnidad)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        campoFecha = findViewById(R.id.campoFecha)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnAumentar = findViewById(R.id.btnAumentar)
        btnDisminuir = findViewById(R.id.btnDisminuir)

        btnGuardar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        btnEliminar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        val categorias = listOf("Lácteos", "Carnes", "Frutas", "Verduras", "Otros")
        val unidades = listOf("Paquete", "Gramos", "Mililitros", "Litros", "Piezas")

        spinnerCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerUnidad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unidades)

        val alimentoId = intent.getIntExtra("alimento_id", -1)
        if (alimentoId == -1) {
            Toast.makeText(this, "Error: ID inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@EditarAlimentoActivity).alimentoDao()
            alimento = dao.getAlimentoById(alimentoId)

            if (alimento == null) {
                Toast.makeText(this@EditarAlimentoActivity, "Alimento no encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            campoNombre.setText(alimento!!.nombre)
            campoCantidad.setText(alimento!!.cantidad.toString())
            campoFecha.setText(alimento!!.fechaCaducidad)
            spinnerCategoria.setSelection(categorias.indexOf(alimento!!.categoria))
            spinnerUnidad.setSelection(unidades.indexOf(alimento!!.unidad))
        }

        btnAumentar.setOnClickListener {
            val cantidad = campoCantidad.text.toString().toDoubleOrNull() ?: 0.0
            campoCantidad.setText((cantidad + 1).toString())
        }

        btnDisminuir.setOnClickListener {
            val cantidad = campoCantidad.text.toString().toDoubleOrNull() ?: 0.0
            val nuevaCantidad = cantidad - 1
            if (nuevaCantidad >= 0) campoCantidad.setText(nuevaCantidad.toString())
        }

        campoFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val anio = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val mesFormateado = String.format("%02d", month + 1)
                val diaFormateado = String.format("%02d", dayOfMonth)
                campoFecha.setText("$year-$mesFormateado-$diaFormateado")
            }, anio, mes, dia)

            datePicker.show()
        }

        btnGuardar.setOnClickListener {
            val nombre = campoNombre.text.toString()
            val cantidad = campoCantidad.text.toString().toDoubleOrNull()
            val unidad = spinnerUnidad.selectedItem.toString()
            val categoria = spinnerCategoria.selectedItem.toString()
            val fecha = campoFecha.text.toString()

            if (nombre.isEmpty() || cantidad == null || cantidad < 0 || fecha.isEmpty()) {
                Toast.makeText(this, "Campos inválidos o cantidad negativa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(this@EditarAlimentoActivity).alimentoDao()
                if (cantidad == 0.0) {
                    alimento?.let { dao.delete(it) }
                    Toast.makeText(this@EditarAlimentoActivity, "Alimento eliminado por cantidad cero", Toast.LENGTH_SHORT).show()
                } else {
                    val actualizado = alimento?.copy(
                        nombre = nombre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        cantidad = cantidad,
                        unidad = unidad,
                        categoria = categoria,
                        fechaCaducidad = fecha
                    )
                    actualizado?.let { dao.update(it) }
                    Toast.makeText(this@EditarAlimentoActivity, "Alimento actualizado", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        btnEliminar.setOnClickListener {
            val actual = alimento
            if (actual == null) {
                Toast.makeText(this, "Error: alimento no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@EditarAlimentoActivity)
                db.alimentoDao().delete(actual)
                Toast.makeText(this@EditarAlimentoActivity, "Alimento eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
