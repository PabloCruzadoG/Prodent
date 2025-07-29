package com.example.prodent.model

data class ReseñaConPaciente(
    val calificacion: Double = 0.0,
    val comentario: String = "",
    val fecha: String = "",
    val nombrePaciente: String = "",
    val apellidoPaciente: String = "",
    val fotoPaciente: String = ""
)