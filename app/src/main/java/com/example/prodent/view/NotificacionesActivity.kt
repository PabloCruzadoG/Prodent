package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.databinding.ActivityNotificacionesBinding
import com.example.prodent.viewmodel.NotificacionesViewModel
import com.example.prodent.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificacionesBinding
    private val viewModel: NotificacionesViewModel by viewModels()
    private var rolUsuario: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar notificaciones desde Firestore
        viewModel.cargarNotificaciones()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("notificaciones")
                .whereEqualTo("uid", uid)
                .whereEqualTo("visto", false)
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result.documents) {
                        doc.reference.update("visto", true)
                    }
                }
        }

        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    rolUsuario = doc.getString("rol") ?: ""
                }
        }

        // Observar lista y generar vistas dinámicamente
        viewModel.notificaciones.observe(this) { lista ->
            binding.layoutNotificaciones.removeAllViews()

            for (noti in lista) {
                val view = LayoutInflater.from(this)
                    .inflate(R.layout.item_notificacion, binding.layoutNotificaciones, false)

                view.findViewById<TextView>(R.id.tvMensaje).text = noti.mensaje
                view.findViewById<TextView>(R.id.tvHorario).text = "${noti.horaInicio} - ${noti.horaFin}"

                binding.layoutNotificaciones.addView(view)
            }
        }


        // Opcional: manejar navegación en barra inferior
        binding.bottomNavigationView.selectedItemId = R.id.nav_notifications
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
                R.id.nav_notifications -> true
                R.id.nav_configuracion -> {
                    startActivity(Intent(this, ConfiguracionActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        // Ocultar el badge al entrar
        binding.bottomNavigationView.removeBadge(R.id.nav_notifications)

    }
}