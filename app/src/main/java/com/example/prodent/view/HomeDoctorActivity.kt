package com.example.prodent.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.prodent.R
import com.example.prodent.model.Cita
import com.example.prodent.viewmodel.CitaViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import android.graphics.Color
import android.view.Gravity
import com.example.prodent.databinding.ActivityHomedoctorBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class HomeDoctorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomedoctorBinding
    private val citaViewModel: CitaViewModel by viewModels()
    private var doctorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomedoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        binding.bottomNavigationView.selectedItemId = R.id.nav_home

        // Observa el LiveData del ViewModel
        citaViewModel.citasPendientes.observe(this, Observer { citas ->
            binding.contenedorCitas.removeAllViews()
            if (citas.isEmpty()) {
                val mensaje = TextView(this).apply {
                    text = "No hay citas pendientes"
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)
                }
                binding.contenedorCitas.addView(mensaje)
            } else {
                val formato = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
                formato.timeZone = TimeZone.getTimeZone("America/Lima")

                val ahora = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).time

                citas.forEach { cita ->
                    agregarCitaUI(cita)
                }

            }

        })

        // Fecha actual al abrir la pantalla
        val fechaActual = obtenerFechaActual()
        citaViewModel.cargarCitasPorFecha(doctorId, fechaActual)

        // Selección de fecha
        binding.calendarIcon.setOnClickListener {
            mostrarDatePicker()
        }

        // Navegación inferior
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
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

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("notificaciones")
            .whereEqualTo("uid", uid)
            .whereEqualTo("visto", false)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val badge = binding.bottomNavigationView.getOrCreateBadge(R.id.nav_notifications)
                    badge.isVisible = true
                }
            }

    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, y, m, d ->
                val fecha = "$d/${m + 1}/$y"
                citaViewModel.cargarCitasPorFecha(doctorId, fecha)
            },
            anio, mes, dia
        )
        datePicker.show()
    }

    private fun obtenerFechaActual(): String {
        val calendario = Calendar.getInstance()
        val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val mes = calendario.get(Calendar.MONTH) + 1
        val anio = calendario.get(Calendar.YEAR)
        return "$dia/$mes/$anio"
    }

    private fun agregarCitaUI(cita: Cita) {
        val tarjeta = layoutInflater.inflate(R.layout.item_cita, binding.contenedorCitas, false)
        tarjeta.findViewById<TextView>(R.id.tvFechaHora).text =
            "Tu próxima cita es el ${cita.fecha} a las ${cita.hora}"
        val tvPaciente = tarjeta.findViewById<TextView>(R.id.tvPaciente)
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(cita.paciente)
            .get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: ""
                val apellido = doc.getString("apellido") ?: ""
                tvPaciente.text = "Paciente: $nombre $apellido"
            }
            .addOnFailureListener {
                tvPaciente.text = "Paciente no disponible"
            }

        binding.contenedorCitas.addView(tarjeta)

        tarjeta.setOnClickListener {
            val intent = Intent(this, IndicacionesActivity::class.java).apply {
                putExtra("pacienteId", cita.paciente)
                putExtra("fecha", cita.fecha)
                putExtra("hora", cita.hora)
                putExtra("doctorId", cita.doctorId)
                putExtra("pacienteNombre", tvPaciente.text.toString())
            }
            startActivity(intent)
        }

    }
}
