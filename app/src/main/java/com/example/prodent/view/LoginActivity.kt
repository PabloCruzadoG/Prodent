package com.example.prodent.view

import android.content.Intent
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val rol = doc.getString("rol")
                                val nombre = doc.getString("nombre") ?: "Usuario"

                                Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()

                                when (rol) {
                                    "Doctor" -> {
                                        startActivity(Intent(this, HomeDoctorActivity::class.java))
                                    }
                                    "Paciente" -> {
                                        startActivity(Intent(this, HomePacienteActivity::class.java))
                                    }
                                    else -> {
                                        Toast.makeText(this, "Rol no válido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Usuario no registrado en Firestore", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al obtener datos", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
        }
        binding.tvRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

    }
}