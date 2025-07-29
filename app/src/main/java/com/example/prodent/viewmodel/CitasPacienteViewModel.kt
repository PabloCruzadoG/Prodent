package com.example.prodent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.Cita
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CitasPacienteViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _citasPendientes = MutableLiveData<List<Cita>>()
    val citasPendientes: LiveData<List<Cita>> = _citasPendientes

    private val _citasPasadas = MutableLiveData<List<Cita>>()
    val citasPasadas: LiveData<List<Cita>> = _citasPasadas

    private val _calificaciones = MutableLiveData<Map<String, Double>>()
    val calificaciones: LiveData<Map<String, Double>> = _calificaciones

    private val _citaSeleccionada = MutableLiveData<Cita?>()
    val citaSeleccionada: LiveData<Cita?> = _citaSeleccionada

    fun seleccionarCitaParaVerIndicaciones(cita: Cita?) {
        _citaSeleccionada.value = cita
    }


    fun cargarCitas() {
        val pacienteId = auth.currentUser?.uid ?: return
        db.collection("citas")
            .whereEqualTo("paciente", pacienteId)
            .get()
            .addOnSuccessListener { result ->
                val zonaPeru = TimeZone.getTimeZone("America/Lima")
                val ahora = Calendar.getInstance(zonaPeru).time

                // Formato que incluya la hora
                val formato = SimpleDateFormat("d/M/yyyy hh:mm a", Locale.getDefault())
                formato.timeZone = zonaPeru

                val pendientes = mutableListOf<Cita>()
                val pasadas = mutableListOf<Cita>()

                for (doc in result) {
                    val cita = doc.toObject(Cita::class.java)

                    // Separar la hora de inicio
                    val horaInicio = try {
                        cita.hora.split(" - ")[0].trim()
                    } catch (e: Exception) {
                        "12:00 AM" // Valor por defecto si algo falla
                    }

                    // Combinar fecha y hora
                    val fechaHora = try {
                        formato.parse("${cita.fecha} $horaInicio")
                    } catch (e: Exception) {
                        null
                    }

                    // Clasificar según fecha y hora
                    if (cita.estado == "atendida" || (fechaHora != null && fechaHora.before(ahora))) {
                        pasadas.add(cita)
                    } else {
                        pendientes.add(cita)
                    }
                }

                _citasPendientes.value = pendientes
                _citasPasadas.value = pasadas
                cargarCalificaciones(pacienteId)
            }
    }



    private fun cargarCalificaciones(pacienteId: String) {
        db.collection("calificaciones_doctor")
            .whereEqualTo("pacienteId", pacienteId)
            .get()
            .addOnSuccessListener { result ->
                val calificacionesMap = mutableMapOf<String, Double>()
                for (doc in result) {
                    val doctorId = doc.getString("doctorId") ?: continue
                    val calificacion = doc.getDouble("calificacion") ?: 0.0
                    calificacionesMap[doctorId] = calificacion
                }
                _calificaciones.value = calificacionesMap
            }
    }
}
