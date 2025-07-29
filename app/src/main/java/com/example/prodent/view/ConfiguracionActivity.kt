package com.example.prodent.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.R
import com.example.prodent.databinding.ActivityConfiguracionBinding
import com.example.prodent.viewmodel.ConfiguracionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private val viewModel = ConfiguracionViewModel()
    private var rolUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.selectedItemId = R.id.nav_configuracion

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                rolUsuario = doc.getString("rol") ?: ""

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
                            true
                        }

                        else -> false
                    }
                }
            }

        binding.tvEditarPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        binding.tvCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

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
}
