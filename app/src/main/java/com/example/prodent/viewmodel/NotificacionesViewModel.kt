package com.example.prodent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.NotificacionCita
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class NotificacionesViewModel : ViewModel() {

    private val _notificaciones = MutableLiveData<List<NotificacionCita>>(emptyList())
    val notificaciones: LiveData<List<NotificacionCita>> = _notificaciones

    private val db = FirebaseFirestore.getInstance()

    fun cargarNotificaciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("notificaciones")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { doc ->
                    NotificacionCita(
                        mensaje = doc.getString("mensaje") ?: "",
                        horaInicio = doc.getString("horaInicio") ?: "",
                        horaFin = doc.getString("horaFin") ?: ""
                    )
                }
                _notificaciones.value = lista
            }
            .addOnFailureListener {
                _notificaciones.value = emptyList()
            }
    }


    fun agregarNotificacion(nueva: NotificacionCita) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = mapOf(
            "uid" to uid,
            "mensaje" to nueva.mensaje,
            "horaInicio" to nueva.horaInicio,
            "horaFin" to nueva.horaFin,
            "timestamp" to System.currentTimeMillis(),
            "visto" to false
        )
        db.collection("notificaciones").add(data)
            .addOnSuccessListener {
                cargarNotificaciones()
            }
    }



    fun eliminarTodas() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("notificaciones")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    db.collection("notificaciones").document(doc.id).delete()
                }
                _notificaciones.value = emptyList()
            }
    }

}
