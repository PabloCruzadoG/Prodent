package com.example.prodent.model

// Asegúrate de que el modelo Doctor tenga un constructor sin argumentos.
data class Doctor(
    val id: String = "",
    val usuarioId: String = "",
    val especialidad: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val calificacionPromedio: Double = 0.0,
)
