package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import com.example.foodtracker.modelos.Alimento
import com.example.foodtracker.modelos.AlimentosAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var alimentosAdapter: AlimentosAdapter
    private lateinit var alimentosList: MutableList<Alimento>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        listView = findViewById(R.id.listViewAlimentos)
        sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)

        // Cargar alimentos desde SharedPreferences
        cargarAlimentos()

        // Configurar adaptador
        alimentosList = cargarAlimentos() // Carga los datos iniciales
        alimentosAdapter = AlimentosAdapter(this, alimentosList)
        listView.adapter = alimentosAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, EditarAlimentoActivity::class.java).apply {
                putExtra("alimento", alimentosList[position]) // Envía el objeto serializable directamente
                putExtra("position", position)
            }
            startActivityForResult(intent, 1)
        }
        val btnAgregarAlimento = findViewById<Button>(R.id.btnAgregarAlimento)
        btnAgregarAlimento.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    override fun onResume() {
        super.onResume()
        cargarAlimentos()
        alimentosAdapter.notifyDataSetChanged()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val alimentoEditado = data.getSerializableExtra("alimentoEditado") as Alimento
            val position = data.getIntExtra("position", -1)

            if (position != -1) {
                // 1. Actualiza la lista en memoria
                alimentosList[position] = alimentoEditado

                // 2. Notifica al adaptador del cambio
                alimentosAdapter.notifyDataSetChanged()

                // 3. Guarda la lista actualizada en SharedPreferences
                guardarAlimentos()

                // Opcional: Muestra un mensaje de confirmación
                Toast.makeText(this, "Alimento actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun cargarAlimentos(): MutableList<Alimento> {
        val json = sharedPreferences.getString("listaAlimentos", null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<MutableList<Alimento>>() {}.type)
        } else {
            mutableListOf()
        }
    }
    private fun guardarAlimentos() {
        val sharedPreferences = getSharedPreferences("FoodTrackerPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(alimentosList)  // Convierte la lista a JSON
        editor.putString("listaAlimentos", json)  // Usa la misma clave que en MainActivity
        editor.apply()  // Guarda los cambios
    }
}
