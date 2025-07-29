package com.example.prodent.viewmodel

import androidx.lifecycle.ViewModel
import com.example.prodent.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun loginUsuario(
        email: String,
        password: String,
        onSuccess: (Usuario) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                db.collection("usuarios").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val usuario = Usuario(
                            id = userId,
                            nombre = doc.getString("nombre") ?: "",
                            apellido = doc.getString("apellido") ?: "",
                            correo = doc.getString("correo") ?: "",
                            rol = doc.getString("rol") ?: "",
                            telefono = doc.getString("telefono") ?: ""
                        )
                        onSuccess(usuario)
                    }
                    .addOnFailureListener {
                        onError("No se pudo recuperar el perfil del usuario")
                    }
            }
            .addOnFailureListener {
                onError("Correo o contraseña incorrectos")
            }
    }
}