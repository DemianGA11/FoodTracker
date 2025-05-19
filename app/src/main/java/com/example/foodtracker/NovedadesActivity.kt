package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.foodtracker.modelos.AppDatabase
import com.example.foodtracker.modelos.NotificacionesAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.foodtracker.modelos.Notificacion

class NovedadesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novedades)

        val listView = findViewById<ListView>(R.id.listViewNovedades)
        val adapter = NotificacionesAdapter(this, mutableListOf())
        listView.adapter = adapter

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.notificacionDao().getTodas().collect { lista ->
                runOnUiThread {
                    adapter.clear()
                    adapter.addAll(lista)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        // Menú inferior
        findViewById<ImageView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navAgregar).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navEstadisticas).setOnClickListener {
            startActivity(Intent(this, EstadisticasActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navNotificaciones).setOnClickListener {
            // Ya estás aquí
        }
    }
}
