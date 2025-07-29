package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.prodent.R
import com.example.prodent.databinding.ActivityRegistrarCitaBinding
import com.example.prodent.model.Cita
import com.example.prodent.model.Doctor
import com.example.prodent.model.NotificacionCita
import com.example.prodent.utils.decodeBase64ToBitmap
import com.example.prodent.viewmodel.CitaViewModel
import com.example.prodent.viewmodel.NotificacionesViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrarCitaActivity : AppCompatActivity() {

    private lateinit var layoutListaDoctores: LinearLayout
    private lateinit var layoutRegistroCita: ScrollView
    private lateinit var recyclerViewDoctores: RecyclerView
    private lateinit var doctorName: TextView
    private lateinit var doctorSpecialty: TextView
    private lateinit var doctorRating: TextView
    private lateinit var doctorImage: ImageView
    private lateinit var calendarView: CalendarView
    private lateinit var btnReservar: Button
    private lateinit var layoutChips: LinearLayout

    private lateinit var selectedDoctor: Doctor
    private lateinit var selectedDate: String
    private lateinit var selectedTime: String

    private val citaViewModel: CitaViewModel by viewModels()
    private val notificacionesViewModel: NotificacionesViewModel by viewModels()
    private lateinit var binding: ActivityRegistrarCitaBinding
    private var rolUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutListaDoctores = findViewById(R.id.layoutListaDoctores)
        layoutRegistroCita = findViewById<ScrollView>(R.id.layoutRegistroCita)
        recyclerViewDoctores = findViewById(R.id.recyclerViewDoctores)
        doctorName = findViewById(R.id.doctorName)
        doctorSpecialty = findViewById(R.id.tvDoctorSpecialty)
        doctorRating = findViewById(R.id.tvRatingNumber)
        doctorImage = findViewById(R.id.imgDoctor)
        calendarView = findViewById(R.id.calendarView)
        btnReservar = findViewById(R.id.btnReservar)
        layoutChips = findViewById(R.id.layoutChips)

        // Mostrar solo la lista de doctores al inicio
        layoutListaDoctores.visibility = View.VISIBLE
        layoutRegistroCita.visibility = View.GONE

        recyclerViewDoctores.layoutManager = LinearLayoutManager(this)
        citaViewModel.cargarDoctores()

        citaViewModel.doctores.observe(this) { doctores ->
            val adapter = DoctorAdapter(doctores) { doctor ->
                selectedDoctor = doctor

                cargarDatosDoctor(doctor)

                // Mostrar la pantalla de registro de cita
                layoutListaDoctores.visibility = View.GONE
                layoutRegistroCita.visibility = View.VISIBLE
            }

            recyclerViewDoctores.adapter = adapter
        }


        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            if (::selectedDoctor.isInitialized) {
                citaViewModel.cargarHorarios(selectedDoctor.id, selectedDate)
            }
        }


        citaViewModel.horarios.observe(this) { horarios ->
            layoutChips.removeAllViews()
            if (horarios.isEmpty()) {
                Toast.makeText(this, "No hay horarios disponibles para esta fecha", Toast.LENGTH_SHORT).show()
            }
            horarios.forEach { horario ->
                val chip = layoutInflater.inflate(R.layout.chip_horario, layoutChips, false)
                val text = "${horario.horaInicio} - ${horario.horaFin}"
                chip.findViewById<TextView>(R.id.textHorario).text = text
                chip.setOnClickListener {
                    selectedTime = text
                    Toast.makeText(this, "Horario seleccionado: $selectedTime", Toast.LENGTH_SHORT).show()
                }
                layoutChips.addView(chip)
            }
        }

        btnReservar.setOnClickListener {
            if (::selectedDoctor.isInitialized && ::selectedDate.isInitialized && ::selectedTime.isInitialized) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val db = FirebaseFirestore.getInstance()
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val nombre = doc.getString("nombre") ?: ""
                        val apellido = doc.getString("apellido") ?: ""
                        val cita = Cita(
                            paciente = uid,
                            fecha = selectedDate,
                            hora = selectedTime,
                            estado = "pendiente",
                            doctorId = selectedDoctor.id
                        )
                        citaViewModel.guardarCita(cita)

                        val horaSplit = selectedTime.split(" - ")
                        val notificacion = NotificacionCita(
                            mensaje = "Tienes una cita programada para el $selectedDate",
                            horaInicio = horaSplit[0],
                            horaFin = horaSplit[1]
                        )
                        notificacionesViewModel.agregarNotificacion(notificacion)

                        val notificacionDoctor = mapOf(
                            "uid" to selectedDoctor.id,
                            "mensaje" to "Se ha reservado una nueva cita para el $selectedDate",
                            "horaInicio" to horaSplit[0],
                            "horaFin" to horaSplit[1],
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("notificaciones").add(notificacionDoctor)

                        Toast.makeText(this, "Cita reservada para el $selectedDate a las $selectedTime", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "No se pudo obtener el nombre del paciente", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Selecciona una cita", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = if (rolUsuario == "Doctor") {
                        Intent(this, HomeDoctorActivity::class.java)
                    } else {
                        Intent(this, HomePacienteActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_calendar -> {
                    val intent = if (rolUsuario == "Doctor") {
                        Intent(this, HorarioActivity::class.java)
                    } else {
                        Intent(this, CitasPacienteActivity::class.java)
                    }
                    startActivity(intent)
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

    private fun cargarDatosDoctor(doctor: Doctor) {
        selectedDoctor = doctor

        doctorSpecialty.text = doctor.especialidad.ifEmpty { "Especialidad no disponible" }

        doctorRating.text = "Cargando..."

        calcularCalificacionReal(doctor.usuarioId)

        if (!doctor.usuarioId.isNullOrEmpty() && doctor.usuarioId != "usuarios") {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(doctor.usuarioId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val nombre = doc.getString("nombre") ?: doctor.nombre
                        val apellido = doc.getString("apellido") ?: doctor.apellido
                        doctorName.text = "Dr. $nombre $apellido"

                        val fotoBase64 = doc.getString("fotoBase64")
                        if (!fotoBase64.isNullOrEmpty()) {
                            val bitmap = decodeBase64ToBitmap(fotoBase64)
                            bitmap?.let {
                                Glide.with(this)
                                    .load(it)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_default_doctor)
                                    .error(R.drawable.ic_default_doctor)
                                    .into(doctorImage)
                            }
                        } else {
                            doctorImage.setImageResource(R.drawable.ic_default_doctor)
                        }
                    } else {
                        doctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
                        doctorImage.setImageResource(R.drawable.ic_default_doctor)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RegistrarCita", "Error al cargar usuario: ${doctor.usuarioId}", exception)
                    doctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
                    doctorImage.setImageResource(R.drawable.ic_default_doctor)
                }
        } else {
            // Si no tiene usuarioId, usar datos básicos del doctor
            doctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
            doctorImage.setImageResource(R.drawable.ic_default_doctor)
        }
    }

    private fun calcularCalificacionReal(doctorId: String) {
        FirebaseFirestore.getInstance()
            .collection("calificaciones")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("calificado", true)
            .get()
            .addOnSuccessListener { calificacionesResult ->
                var totalCalificaciones = 0.0
                var cantidadCalificaciones = 0

                for (document in calificacionesResult) {
                    val calificacion = document.getDouble("calificacion") ?: 0.0
                    totalCalificaciones += calificacion
                    cantidadCalificaciones++
                }

                val calificacionPromedio = if (cantidadCalificaciones > 0) {
                    totalCalificaciones / cantidadCalificaciones
                } else {
                    0.0
                }

                runOnUiThread {
                    if (calificacionPromedio > 0) {
                        doctorRating.text = String.format("%.1f", calificacionPromedio)
                    } else {
                        doctorRating.text = "Sin calificaciones"
                    }
                }

                Log.d("RegistrarCita", "Calificación calculada para $doctorId: $calificacionPromedio ($cantidadCalificaciones calificaciones)")
            }
            .addOnFailureListener { exception ->
                Log.e("RegistrarCita", "Error al calcular calificación", exception)
                runOnUiThread {
                    doctorRating.text = "--"
                }
            }
    }
}