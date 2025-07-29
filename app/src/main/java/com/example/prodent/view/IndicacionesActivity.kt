package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.R
import com.example.prodent.databinding.ActivityIndicacionesBinding
import com.example.prodent.viewmodel.IndicacionesViewModel

class IndicacionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIndicacionesBinding
    private val viewModel: IndicacionesViewModel by viewModels()

    private lateinit var pacienteId: String
    private lateinit var fecha: String
    private lateinit var hora: String
    private lateinit var doctorId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIndicacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar datos
        pacienteId = intent.getStringExtra("pacienteId") ?: ""
        fecha = intent.getStringExtra("fecha") ?: ""
        hora = intent.getStringExtra("hora") ?: ""
        doctorId = intent.getStringExtra("doctorId") ?: ""

        val pacienteNombre = intent.getStringExtra("pacienteNombre") ?: ""

        binding.tvPaciente.text = "Paciente: $pacienteNombre\nFecha: $fecha - $hora"

        binding.btnGuardar.setOnClickListener {
            val medicamentos = binding.etMedicamentos.text.toString()
            val cuidados = binding.etCuidados.text.toString()

            if (medicamentos.isBlank() && cuidados.isBlank()) {
                Toast.makeText(this, "Escribe al menos una indicación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.guardarIndicaciones(
                pacienteId,
                fecha,
                hora,
                doctorId,
                medicamentos,
                cuidados
            )
        }

        viewModel.estadoGuardado.observe(this) { guardado ->
            if (guardado == true) {
                Toast.makeText(this, "Indicaciones guardadas", Toast.LENGTH_SHORT).show()
                finish()
            } else if (guardado == false) {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeDoctorActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, HorarioActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificacionesActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_configuracion -> {
                    startActivity(Intent(this, ConfiguracionActivity::class.java))
                    finish()
                    true
                }

                else -> false


            }
        }
    }
}
