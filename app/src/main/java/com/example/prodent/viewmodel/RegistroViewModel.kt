package com.example.prodent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.Paciente
import com.example.prodent.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegistroViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> = _mensajeError

    fun isPhoneValid(phone: String): Boolean {
        return phone.matches(Regex("^9\\d{8}$"))
    }

    fun isPasswordSecure(password: String): Boolean {
        return password.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}\$"
        ))
    }

    fun registrarPaciente(
        nombre: String,
        apellido: String,
        correo: String,
        telefono: String,
        contrasena: String,
        repetirContrasena: String
    ) {
        if (nombre.isBlank() || apellido.isBlank() || correo.isBlank() || telefono.isBlank() || contrasena.isBlank() || repetirContrasena.isBlank()) {
            _mensajeError.value = "Completa todos los campos"
            return
        }

        if (contrasena != repetirContrasena) {
            _mensajeError.value = "Las contraseñas no coinciden"
            return
        }


        // Registro en FirebaseAuth
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                // NO se guarda la contraseña aquí
                val usuario = Usuario(
                    id = uid,
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,
                    rol = "Paciente",
                    telefono = telefono

                )

                db.collection("usuarios").document(uid).set(usuario)
                    .addOnSuccessListener {
                        val paciente = Paciente(
                            id = db.collection("pacientes").document().id,
                            usuarioId = uid,
                            telefono = telefono
                        )
                        db.collection("pacientes").document(paciente.id).set(paciente)
                            .addOnSuccessListener {
                                _registroExitoso.value = true
                            }
                            .addOnFailureListener {
                                _mensajeError.value = "Error al registrar paciente: ${it.message}"
                            }
                    }
                    .addOnFailureListener {
                        _mensajeError.value = "Error al registrar usuario: ${it.message}"
                    }
            }


            .addOnFailureListener { e ->
                val mensaje = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "El formato del correo es inválido"
                    is FirebaseAuthUserCollisionException -> "Este correo ya está registrado"
                    else -> "Error al registrar: ${e.localizedMessage}"
                }
                _mensajeError.value = mensaje
            }

    }
}
