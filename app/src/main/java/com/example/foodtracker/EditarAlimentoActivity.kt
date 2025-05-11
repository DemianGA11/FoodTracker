package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodtracker.modelos.Alimento

class EditarAlimentoActivity : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var spnUnidad: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var btnSumar: Button
    private lateinit var btnRestar: Button
    private lateinit var btnEliminar: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_alimento)

        edtNombre = findViewById(R.id.edtNombre)
        edtCantidad = findViewById(R.id.edtCantidad)
        spnUnidad = findViewById(R.id.spnUnidad)
        btnGuardar = findViewById(R.id.btnGuardar)

        btnSumar = findViewById(R.id.btnSumar)
        btnRestar = findViewById(R.id.btnRestar)
        btnEliminar = findViewById(R.id.btnEliminar)

        // Recibe el alimento directamente como Serializable
        val alimento = intent.getSerializableExtra("alimento") as? Alimento

        if (alimento != null) {
            // Llena los campos con los datos del alimento
            edtNombre.setText(alimento.nombre)
            edtCantidad.setText(alimento.cantidad.toString())

            // Configura el Spinner de unidad
            val unidades = arrayOf("Paquete", "Gramos", "Mililitros", "Litros", "Piezas")
            val adapterUnidades = ArrayAdapter(this, android.R.layout.simple_spinner_item, unidades)
            adapterUnidades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spnUnidad.adapter = adapterUnidades

            // Selecciona la unidad guardada en el alimento
            val unidadPosition = unidades.indexOf(alimento.unidad)
            if (unidadPosition >= 0) {
                spnUnidad.setSelection(unidadPosition)
            }
        }
        val spnCategoria = findViewById<Spinner>(R.id.spnCategoria)

        // Configura el Spinner de categoría
        val categorias = listOf("Lácteos", "Carnes", "Frutas", "Verduras", "Otros")
        val adapterCategorias = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spnCategoria.adapter = adapterCategorias
        // Selecciona la categoría guardada
        if (alimento != null) {
            val categoriaPosition = categorias.indexOf(alimento.categoria)
            if (categoriaPosition >= 0) {
                spnCategoria.setSelection(categoriaPosition)
            }
        }
        val txtFecha = findViewById<TextView>(R.id.txtFechaCaducidad)
        if (alimento != null) {
            txtFecha.text = "Vence: ${alimento.fechaCaducidad}"
        }
        btnGuardar.setOnClickListener {
            guardarCambios(alimento) // Pasamos el alimento original para mantener datos no editados (como fecha)
        }
        btnSumar.setOnClickListener {
            val cantidadActual = edtCantidad.text.toString().toDoubleOrNull() ?: 0.0
            edtCantidad.setText((cantidadActual + 1).toString())
        }

        // Listener para restar (no permitir valores negativos)
        btnRestar.setOnClickListener {
            val cantidadActual = edtCantidad.text.toString().toDoubleOrNull() ?: 0.0
            if (cantidadActual > 0) {
                edtCantidad.setText((cantidadActual - 1).toString())
            } else {
                Toast.makeText(this, "La cantidad no puede ser menor a 0", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para eliminar
        btnEliminar.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("posicionEliminar", intent.getIntExtra("position", -1))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun guardarCambios(alimentoOriginal: Alimento?) {
        val nombre = edtNombre.text.toString()
        val cantidad = edtCantidad.text.toString().toDoubleOrNull()?.takeIf { it >= 0 } ?: run {
            Toast.makeText(this, "Cantidad no válida", Toast.LENGTH_SHORT).show()
            return
        }
        val unidad = spnUnidad.selectedItem.toString()

        if (nombre.isNotEmpty() && cantidad != null && alimentoOriginal != null) {
            val alimentoEditado = Alimento(
                nombre,
                alimentoOriginal.fechaCaducidad, // Mantén la fecha original
                alimentoOriginal.categoria, // Mantén la categoría original
                cantidad,
                unidad
            )
            val resultIntent = Intent().apply {
                putExtra("alimentoEditado", alimentoEditado)
                putExtra("position", intent.getIntExtra("position", -1))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
        }
    }
}
