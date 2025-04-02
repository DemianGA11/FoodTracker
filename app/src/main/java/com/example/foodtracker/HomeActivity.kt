package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
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
        alimentosAdapter = AlimentosAdapter(this, alimentosList)
        listView.adapter = alimentosAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, EditarAlimentoActivity::class.java).apply {
                putExtra("alimento", Gson().toJson(alimentosList[position]))
                putExtra("position", position) // Envía la posición para actualizar
            }
            startActivityForResult(intent, 1)
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
                alimentosList[position] = alimentoEditado
                guardarAlimentos()  // ¡Ahora sí existe este método!
                alimentosAdapter.notifyDataSetChanged()
            }
        }
    }
    private fun cargarAlimentos() {
        val gson = Gson()
        val json = sharedPreferences.getString("listaAlimentos", null) // Cambiado a "listaAlimentos"
        val type = object : TypeToken<MutableList<Alimento>>() {}.type
        alimentosList = gson.fromJson(json, type) ?: mutableListOf()
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
