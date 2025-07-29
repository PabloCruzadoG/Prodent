package com.example.prodent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class IndicacionesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _estadoGuardado = MutableLiveData<Boolean>()
    val estadoGuardado: LiveData<Boolean> = _estadoGuardado

    fun guardarIndicaciones(
        pacienteId: String,
        fecha: String,
        hora: String,
        doctorId: String,
        medicamentos: String,
        cuidados: String
    ) {
        db.collection("citas")
            .whereEqualTo("paciente", pacienteId)
            .whereEqualTo("fecha", fecha)
            .whereEqualTo("hora", hora)
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { result ->
                val documento = result.documents.firstOrNull()
                if (documento != null) {
                    db.collection("citas").document(documento.id)
                        .update(
                            mapOf(
                                "indicacionesMedicamentos" to medicamentos,
                                "indicacionesCuidados" to cuidados,
                                "estado" to "atendida"
                            )
                        )
                        .addOnSuccessListener {
                            _estadoGuardado.value = true
                        }
                        .addOnFailureListener {
                            _estadoGuardado.value = false
                        }
                } else {
                    _estadoGuardado.value = false
                }
            }
            .addOnFailureListener {
                _estadoGuardado.value = false
            }
    }
}
