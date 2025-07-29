package com.example.prodent.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.CalificacionDoctor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalificacionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _calificacionGuardada = MutableLiveData<Boolean>()
    val calificacionGuardada: LiveData<Boolean> = _calificacionGuardada

    private val _puedeCalificar = MutableLiveData<Boolean>()
    val puedeCalificar: LiveData<Boolean> = _puedeCalificar

    // ✅ Verificar si un paciente puede calificar a un doctor
    fun verificarPuedeCalificar(pacienteId: String, doctorId: String) {
        // Buscar citas completadas que no han sido calificadas
        db.collection("citas")
            .whereEqualTo("paciente", pacienteId)
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("estado", "completada")
            .get()
            .addOnSuccessListener { citasResult ->
                var puedeCalificarDoctor = false

                for (citaDoc in citasResult) {
                    val citaId = citaDoc.id

                    // Verificar si ya existe calificación para esta cita
                    db.collection("calificaciones")
                        .whereEqualTo("pacienteId", pacienteId)
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("citaId", citaId) // ✅ Asociar calificación con cita específica
                        .get()
                        .addOnSuccessListener { calificacionesResult ->
                            if (calificacionesResult.isEmpty) {
                                puedeCalificarDoctor = true
                            }
                            _puedeCalificar.value = puedeCalificarDoctor
                        }
                }
            }
    }

    // ✅ Guardar calificación
    fun guardarCalificacion(
        doctorId: String,
        pacienteId: String,
        citaId: String,
        calificacion: Double,
        comentario: String
    ) {
        val calificacionData = CalificacionDoctor(
            doctorId = doctorId,
            pacienteId = pacienteId,
            calificacion = calificacion,
            comentario = comentario,
            fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            calificado = true
        )

        db.collection("calificaciones")
            .add(calificacionData)
            .addOnSuccessListener { documentReference ->
                Log.d("CalificacionViewModel", "Calificación guardada: ${documentReference.id}")

                // Actualizar la cita para marcar que ya fue calificada
                db.collection("citas")
                    .document(citaId)
                    .update(
                        "calificacionId", documentReference.id,
                        "puedeCalificar", false
                    )

                _calificacionGuardada.value = true
            }
            .addOnFailureListener { exception ->
                Log.e("CalificacionViewModel", "Error al guardar calificación", exception)
                _calificacionGuardada.value = false
            }
    }

    // ✅ Obtener reseñas de un doctor
    fun obtenerReseñasDoctor(doctorId: String): LiveData<List<CalificacionDoctor>> {
        val _reseñas = MutableLiveData<List<CalificacionDoctor>>()

        db.collection("calificaciones")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("calificado", true)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val reseñas = result.map { it.toObject(CalificacionDoctor::class.java) }
                _reseñas.value = reseñas
            }
            .addOnFailureListener {
                _reseñas.value = emptyList()
            }

        return _reseñas
    }
}