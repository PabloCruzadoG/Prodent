package com.example.prodent.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.Horario
import com.google.firebase.firestore.FirebaseFirestore

class HorarioViewModel : ViewModel() {

    val horarios = MutableLiveData<List<Horario>>()
    private val db = FirebaseFirestore.getInstance()

    fun agregarHorario(doctorId: String, fecha: String, horaInicio: String, horaFin: String) {
        val nuevoDoc = db.collection("horarios").document()
        val horario = Horario(
            id = nuevoDoc.id,
            doctorId = doctorId,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin
        )
        nuevoDoc.set(horario)
            .addOnSuccessListener {
                cargarHorariosPorFecha(doctorId, fecha)
            }
    }

    fun cargarHorariosPorFecha(doctorId: String, fecha: String) {
        db.collection("horarios")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("fecha", fecha)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { it.toObject(Horario::class.java) }
                horarios.value = lista
            }
    }

    fun cancelarHorariosDelDia(doctorId: String, fecha: String) {
        db.collection("horarios")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("fecha", fecha)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                result.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    horarios.value = listOf()
                }
            }
    }
}
