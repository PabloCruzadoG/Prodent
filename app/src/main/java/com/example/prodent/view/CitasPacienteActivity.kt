package com.example.prodent.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.prodent.R
import com.example.prodent.databinding.ActivityCitaspacienteBinding
import com.example.prodent.model.CalificacionDoctor
import com.example.prodent.model.Cita
import com.example.prodent.model.NotificacionCita
import com.example.prodent.viewmodel.CitasPacienteViewModel
import com.example.prodent.viewmodel.NotificacionesViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CitasPacienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitaspacienteBinding
    private val viewModel: CitasPacienteViewModel by viewModels()
    private val notificacionesViewModel: NotificacionesViewModel by viewModels()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitaspacienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.selectedItemId = R.id.nav_calendar

        setupTabs()
        setupReservaCitaButton()
        observeViewModel()
        viewModel.cargarCitas() // cargar desde Firebase

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomePacienteActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
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
        firestore.collection("notificaciones")
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

    private fun setupTabs() {
        showCitasPendientes()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showCitasPendientes()
                    1 -> showCitasPasadas()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showCitasPendientes() {
        viewModel.citasPendientes.value?.let { renderCitasPendientes(it) }
    }

    private fun showCitasPasadas() {
        renderCitasPasadas(
            viewModel.citasPasadas.value ?: emptyList(),
            viewModel.calificaciones.value ?: emptyMap()
        )
    }

    private fun observeViewModel() {
        viewModel.citasPendientes.observe(this) {
            if (binding.tabLayout.selectedTabPosition == 0) {
                renderCitasPendientes(it)
            }
        }
        viewModel.citasPasadas.observe(this) { citas ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                renderCitasPasadas(citas, viewModel.calificaciones.value ?: emptyMap())
            }
        }

        viewModel.citaSeleccionada.observe(this) { cita ->
            if (cita != null) {
                mostrarIndicacionesDialog(cita)
                viewModel.seleccionarCitaParaVerIndicaciones(null)
            }
        }
    }

    private fun renderCitasPendientes(citas: List<Cita>) {
        binding.containerCitas.removeAllViews()

        if (citas.isEmpty()) {
            agregarMensaje("No tienes citas pendientes")
            return
        }

        citas.forEach { cita ->
            val view = layoutInflater.inflate(R.layout.item_cita_paciente_pendiente, binding.containerCitas, false)

            val tvFechaCita = view.findViewById<TextView>(R.id.tvFechaCita)
            val tvHoraCita = view.findViewById<TextView>(R.id.tvHoraCita)
            val tvDoctorCita = view.findViewById<TextView>(R.id.tvDoctorCita)
            val btnCancelar = view.findViewById<Button>(R.id.btnCancelar)

            tvFechaCita.text = cita.fecha
            tvHoraCita.text = cita.hora

            // Cargar nombre del doctor
            firestore.collection("usuarios")
                .document(cita.doctorId)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: "Nombre no disponible"
                    val apellido = doc.getString("apellido") ?: ""
                    tvDoctorCita.text = "Dr. $nombre $apellido"
                }
                .addOnFailureListener {
                    tvDoctorCita.text = "Doctor no disponible"
                }

            btnCancelar.setOnClickListener {
                cancelarCita(cita)
            }

            binding.containerCitas.addView(view)
        }
    }

    private fun renderCitasPasadas(citas: List<Cita>, calificaciones: Map<String, Double>) {
        binding.containerCitas.removeAllViews()

        if (citas.isEmpty()) {
            agregarMensaje("No tienes citas pasadas")
            return
        }

        citas.forEach { cita ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_cita_paciente_pasada, binding.containerCitas, false)

            view.findViewById<TextView>(R.id.tvFechaCita).text = cita.fecha
            view.findViewById<TextView>(R.id.tvHoraCita).text = cita.hora

            // Cargar nombre del doctor
            firestore.collection("usuarios")
                .document(cita.doctorId)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: "Doctor"
                    val apellido = doc.getString("apellido") ?: ""
                    view.findViewById<TextView>(R.id.tvDoctorCita).text = "Dr. $nombre $apellido"
                }
                .addOnFailureListener {
                    view.findViewById<TextView>(R.id.tvDoctorCita).text = "Doctor"
                }

            // ✅ CONFIGURAR BOTÓN VER INDICACIONES
            view.findViewById<Button>(R.id.btnVerIndicaciones)?.setOnClickListener {
                mostrarIndicacionesDialog(cita)
            }

            // ✅ VERIFICAR SI YA EXISTE CALIFICACIÓN
            verificarYMostrarCalificacion(view, cita)

            binding.containerCitas.addView(view)
        }
    }

    // ✅ NUEVA FUNCIÓN: Verificar si ya existe calificación
    private fun verificarYMostrarCalificacion(view: View, cita: Cita) {
        val pacienteId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        android.util.Log.d("CitasPaciente", "🔍 Verificando calificación para doctor: ${cita.doctorId}")

        firestore.collection("calificaciones")
            .whereEqualTo("doctorId", cita.doctorId)
            .whereEqualTo("pacienteId", pacienteId)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // Ya existe calificación
                    val calificacion = result.documents[0].getDouble("calificacion") ?: 0.0
                    android.util.Log.d("CitasPaciente", "✅ Calificación encontrada: $calificacion")
                    mostrarCalificacionExistente(view, calificacion)
                } else {
                    // No existe calificación, mostrar botón
                    android.util.Log.d("CitasPaciente", "❌ Sin calificación, mostrando botón")
                    mostrarBotonCalificar(view, cita)
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("CitasPaciente", "Error al verificar calificación", exception)
                mostrarBotonCalificar(view, cita)
            }
    }

    // ✅ NUEVA FUNCIÓN: Mostrar calificación existente
    private fun mostrarCalificacionExistente(view: View, calificacion: Double) {
        // Ocultar botón de calificar
        view.findViewById<Button>(R.id.btnCalificar)?.visibility = View.GONE

        // Mostrar la calificación en el área de información
        val llCalificacion = view.findViewById<LinearLayout>(R.id.llCalificacion)
        val tvCalificacion = view.findViewById<TextView>(R.id.tvCalificacion)

        llCalificacion?.visibility = View.VISIBLE
        tvCalificacion?.text = String.format("%.1f ⭐", calificacion)

        // ✅ SOLO LOG UNA VEZ, NO EN BUCLE:
        // android.util.Log.d("CitasPaciente", "Mostrando calificación existente: $calificacion")
    }

    // ✅ NUEVA FUNCIÓN: Mostrar botón calificar
    private fun mostrarBotonCalificar(view: View, cita: Cita) {
        // Mostrar botón de calificar
        val btnCalificar = view.findViewById<Button>(R.id.btnCalificar)
        btnCalificar?.visibility = View.VISIBLE

        // Ocultar calificación
        view.findViewById<LinearLayout>(R.id.llCalificacion)?.visibility = View.GONE

        // Configurar click del botón
        btnCalificar?.setOnClickListener {
            android.util.Log.d("CitasPaciente", "Abriendo modal para calificar doctor: ${cita.doctorId}")
            showCalificarModal(view, cita.doctorId)
        }

        android.util.Log.d("CitasPaciente", "Mostrando botón calificar para doctor: ${cita.doctorId}")
    }

    // ✅ FUNCIÓN CORREGIDA: Modal de calificación
    private fun showCalificarModal(view: View, doctorId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Calificar al Doctor")

        val modalView = layoutInflater.inflate(R.layout.modal_calificar, null)
        builder.setView(modalView)

        val ratingBar = modalView.findViewById<RatingBar>(R.id.ratingBar)
        val comentarioEditText = modalView.findViewById<EditText>(R.id.etComentario)
        val btnEnviarCalificacion = modalView.findViewById<Button>(R.id.btnEnviarCalificacion)

        val dialog = builder.create()
        dialog.show()

        btnEnviarCalificacion.setOnClickListener {
            val calificacion = ratingBar.rating.toDouble()

            if (calificacion == 0.0) {
                Toast.makeText(this, "Por favor selecciona una calificación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comentario = comentarioEditText.text.toString().trim()
            val pacienteId = FirebaseAuth.getInstance().currentUser?.uid
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            if (doctorId.isNotEmpty() && pacienteId != null) {
                val calificacionDoctor = CalificacionDoctor(
                    doctorId = doctorId,
                    pacienteId = pacienteId,
                    calificacion = calificacion,
                    comentario = comentario,
                    fecha = fechaActual,
                    calificado = true
                )

                firestore.collection("calificaciones")
                    .add(calificacionDoctor)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()

                        // ✅ SOLO ACTUALIZAR LA VISTA ESPECÍFICA, NO RECARGAR TODO
                        mostrarCalificacionExistente(view, calificacion)

                        dialog.dismiss()

                        // ✅ ELIMINAR ESTA LÍNEA QUE CAUSA EL BUCLE:
                        // viewModel.cargarCitas() // ← NO HACER ESTO
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error al guardar la calificación", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("CalificacionError", "Error", exception)
                    }
            } else {
                Toast.makeText(this, "Error al obtener los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarCita(cita: Cita) {
        firestore.collection("citas")
            .whereEqualTo("paciente", cita.paciente)
            .whereEqualTo("fecha", cita.fecha)
            .whereEqualTo("hora", cita.hora)
            .whereEqualTo("doctorId", cita.doctorId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    firestore.collection("citas").document(document.id).delete()
                }

                // Crear notificaciones
                val horaSplit = cita.hora.split(" - ")
                val notificacion = NotificacionCita(
                    mensaje = "Tu cita del ${cita.fecha} ha sido cancelada",
                    horaInicio = horaSplit[0],
                    horaFin = horaSplit[1]
                )
                notificacionesViewModel.agregarNotificacion(notificacion)

                val notificacionDoctor = mapOf(
                    "uid" to cita.doctorId,
                    "mensaje" to "Una cita del ${cita.fecha} ha sido cancelada por el paciente",
                    "horaInicio" to horaSplit[0],
                    "horaFin" to horaSplit[1],
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("notificaciones").add(notificacionDoctor)

                Toast.makeText(this, "Cita cancelada", Toast.LENGTH_SHORT).show()
                viewModel.cargarCitas()
            }
    }

    private fun agregarMensaje(mensaje: String) {
        val textView = TextView(this).apply {
            text = mensaje
            textSize = 16f
            setPadding(16, 32, 16, 0)
        }
        binding.containerCitas.addView(textView)
    }

    private fun setupReservaCitaButton() {
        binding.btnReservarCita.setOnClickListener {
            startActivity(Intent(this, RegistrarCitaActivity::class.java))
        }
    }

    private fun mostrarIndicacionesDialog(cita: Cita) {
        val mensaje = """
        Medicamentos:
        ${cita.indicacionesMedicamentos.ifBlank { "Sin indicaciones" }}

        Cuidados:
        ${cita.indicacionesCuidados.ifBlank { "Sin indicaciones" }}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Indicaciones médicas")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .create()
            .show()
    }
}