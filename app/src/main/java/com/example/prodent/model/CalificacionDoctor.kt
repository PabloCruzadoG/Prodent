package com.example.prodent.model

data class CalificacionDoctor(
    val doctorId: String = "",
    val pacienteId: String = "",
    val calificacion: Double = 0.0,
    val comentario: String = "",
    val fecha: String = "",
    val calificado: Boolean = false

)
