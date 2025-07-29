package com.example.prodent.model

data class Cita(
    val paciente: String = "",
    val fecha: String = "",
    val hora: String = "",
    val estado: String = "",
    val doctorId: String = "",
    val indicacionesMedicamentos: String = "",
    val indicacionesCuidados: String = "",
    val puedeCalificar: Boolean = false,
    val calificacionId: String = ""
)