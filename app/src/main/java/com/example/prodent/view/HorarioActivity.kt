package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.R
import com.example.prodent.databinding.ActivityHorarioBinding
import com.example.prodent.databinding.AgregarHorarioBinding
import com.example.prodent.viewmodel.HorarioViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HorarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHorarioBinding
    private val viewModel: HorarioViewModel by viewModels()
    private var diaSeleccionado: String = ""
    private var doctorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHorarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.bottomNavigationView.selectedItemId = R.id.nav_calendar


        val hoy = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"))
        diaSeleccionado = formatearFecha(hoy)
        actualizarResumen(diaSeleccionado)
        viewModel.cargarHorariosPorFecha(doctorId, diaSeleccionado)

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
                set(year, month, dayOfMonth)
            }

            diaSeleccionado = formatearFecha(calendar)
            actualizarResumen(diaSeleccionado)
            viewModel.cargarHorariosPorFecha(doctorId, diaSeleccionado)
        }

        viewModel.horarios.observe(this) { lista ->
            binding.layoutHorarios.removeAllViews()
            binding.tvResumenDia.text =
                "Día seleccionado: $diaSeleccionado\nHorarios: ${lista.size} citas agendadas"

            lista.forEach { horario ->
                val chip = layoutInflater.inflate(R.layout.chip_horario, binding.layoutHorarios, false)
                chip.findViewById<TextView>(R.id.textHorario).text =
                    "${horario.horaInicio} - ${horario.horaFin}"
                binding.layoutHorarios.addView(chip)
            }
        }

        binding.btnAgregarHorario.setOnClickListener {
            mostrarDialogoAgregarHorario()
        }

        binding.btnCancelarAtencion.setOnClickListener {
            mostrarDialogoConfirmacionCancelacion()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeDoctorActivity::class.java))
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

    private fun mostrarDialogoAgregarHorario() {
        val dialogBinding = AgregarHorarioBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnGuardarHorario.setOnClickListener {
            val horaInicio = "${dialogBinding.etHoraInicio.text}:${dialogBinding.etMinInicio.text} ${dialogBinding.etAmPmInicio.text}"
            val horaFin = "${dialogBinding.etHoraFin.text}:${dialogBinding.etMinFin.text} ${dialogBinding.etAmPmFin.text}"

            viewModel.agregarHorario(doctorId, diaSeleccionado, horaInicio, horaFin)
            dialog.dismiss()
        }

        dialogBinding.btnCancelarDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogoConfirmacionCancelacion() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar atención")
            .setMessage("¿Estás seguro de eliminar todos los horarios para el $diaSeleccionado?")
            .setPositiveButton("Sí") { dialog, _ ->
                viewModel.cancelarHorariosDelDia(doctorId, diaSeleccionado)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun formatearFecha(cal: Calendar): String {
        val dia = cal.get(Calendar.DAY_OF_MONTH)
        val mes = cal.get(Calendar.MONTH) + 1
        val anio = cal.get(Calendar.YEAR)
        return "$dia/$mes/$anio"
    }

    private fun actualizarResumen(fecha: String) {
        binding.tvResumenDia.text = "Día seleccionado: $fecha\nHorarios: 0 citas agendadas"
    }
}