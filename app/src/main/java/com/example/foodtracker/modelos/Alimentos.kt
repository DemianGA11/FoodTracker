package com.example.foodtracker.modelos

// Alimento.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alimentos") // Define el nombre de la tabla
data class Alimento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Clave primaria autogenerada
    val nombre: String,
    val fechaCaducidad: String, // Formato: "yyyy-MM-dd" (ej: "2025-05-20")
    val categoria: String,
    val cantidad: Double,
    val unidad: String,
    val notificado: Boolean = false // Útil para evitar notificaciones duplicadas

)