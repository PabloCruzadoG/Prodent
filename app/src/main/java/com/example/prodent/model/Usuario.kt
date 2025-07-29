package com.example.prodent.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val rol: String = "",
    val telefono: String = "",
    val fotoBase64: String? = null
)