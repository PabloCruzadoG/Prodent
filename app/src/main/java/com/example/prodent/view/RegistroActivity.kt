package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.databinding.ActivityRegistroBinding
import com.example.prodent.viewmodel.RegistroViewModel

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegistro.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val apellido = binding.etApellido.text.toString()
            val correo = binding.etEmail.text.toString()
            val telefono = binding.etPhone.text.toString()
            val contrasena = binding.etPassword.text.toString()
            val repetirContrasena = binding.etPasswordR.text.toString()

            if (!viewModel.isPhoneValid(telefono)) {
                Toast.makeText(this, "Número de teléfono inválido. Debe comenzar con 9 y tener 9 dígitos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!viewModel.isPasswordSecure(contrasena)) {
                Toast.makeText(this, "La contraseña no cumple con los requisitos.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.registrarPaciente(
                nombre,
                apellido,
                correo,
                telefono,
                contrasena,
                repetirContrasena
            )
        }

        viewModel.registroExitoso.observe(this) { exitoso ->
            if (exitoso) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)

            }
        }

        viewModel.mensajeError.observe(this) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }
}