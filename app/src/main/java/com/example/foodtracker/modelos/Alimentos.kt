package com.example.foodtracker.modelos

import java.io.Serializable

data class Alimento(
    val nombre: String,
    val fechaCaducidad: String,
    val categoria: String,
    val cantidad: Double,
    val unidad: String
) : Serializable