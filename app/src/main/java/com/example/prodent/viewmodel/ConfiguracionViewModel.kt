package com.example.prodent.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class ConfiguracionViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    fun cerrarSesion(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
}
