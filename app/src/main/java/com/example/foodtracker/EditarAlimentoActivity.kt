package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodtracker.modelos.Alimento

class EditarAlimentoActivity : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var spnUnidad: Spinner
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_alimento)

        edtNombre = findViewById(R.id.edtNombre)
        edtCantidad = findViewById(R.id.edtCantidad)
        spnUnidad = findViewById(R.id.spnUnidad)
        btnGuardar = findViewById(R.id.btnGuardar)

        val alimento = intent.getSerializableExtra("alimento") as? Alimento
        if (alimento != null) {
            edtNombre.setText(alimento.nombre)
            edtCantidad.setText(alimento.cantidad.toString())
            // Aquí podrías seleccionar la unidad correcta en el Spinner
        }

        btnGuardar.setOnClickListener {
            val nombre = edtNombre.text.toString()
            val cantidad = edtCantidad.text.toString().toDoubleOrNull() // Cambiado a Double
            val unidad = spnUnidad.selectedItem.toString()

            if (nombre.isNotEmpty() && cantidad != null) {
                val alimentoEditado = Alimento(
                    nombre,
                    alimento?.fechaCaducidad ?: "",
                    alimento?.categoria ?: "Otros", // Asegura una categoría por defecto
                    cantidad,
                    unidad
                )

                val resultIntent = Intent()
                resultIntent.putExtra("alimentoEditado", alimentoEditado)
                resultIntent.putExtra("position", intent.getIntExtra("position", -1))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}
