package com.example.prodent.view

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Observer
import com.example.prodent.R
import com.example.prodent.databinding.ActivityHomedoctorBinding
import com.example.prodent.databinding.ActivityHomepacienteBinding
import com.example.prodent.model.Cita
import com.example.prodent.viewmodel.CitaViewModel
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask
import kotlin.getValue

class HomePacienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepacienteBinding
    private lateinit var sharedPref: SharedPreferences
    private val viewModel: CitaViewModel by viewModels()
    private lateinit var consejoTextView: TextView
    private val consejosSalud = listOf(
        "Cambia tu cepillo dental cada 3 meses",
        "Cepíllate los dientes al menos 2 veces al día",
        "Usa hilo dental diariamente",
        "Visita al dentista cada 6 meses para una limpieza",
        "Reduce el consumo de azúcares para prevenir caries",
        "Usa enjuague bucal para complementar tu higiene",
        "No olvides cepillar también tu lengua",
        "Masajea tus encías suavemente para mejorar la circulación"
    )
    private var consejoTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepacienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        consejoTextView = binding.tvConsejoSalud
        binding.bottomNavigationView.selectedItemId = R.id.nav_home

        binding.mapaLayout.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
            true
        }
        // Navegación inferior
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }

                R.id.nav_calendar -> {
                    startActivity(Intent(this, CitasPacienteActivity::class.java))
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

        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val isFirstLogin = sharedPref.getBoolean("first_login_${uid}", true)

        if (isFirstLogin) {
            mostrarGuia()

            editor.putBoolean("first_login_${uid}", false)
            editor.apply()
        }
        mostrarConsejoAleatorio()
        iniciarTimerConsejos()

    }

    private fun mostrarConsejoAleatorio() {
        val consejoAleatorio = consejosSalud.random()
        consejoTextView.text = consejoAleatorio
    }

    private fun iniciarTimerConsejos() {
        consejoTimer = Timer()
        consejoTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    mostrarConsejoAleatorio()
                }
            }
        }, 10000, 10000) // 10 segundos
    }

    override fun onDestroy() {
        super.onDestroy()
        consejoTimer?.cancel()
    }

    private var secuenciaGuia: TapTargetSequence? = null
    private fun mostrarGuia() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val homeItem = bottomNav.findViewById<View>(R.id.nav_home)
        val calendarioItem = bottomNav.findViewById<View>(R.id.nav_calendar)
        val notificacionItem = bottomNav.findViewById<View>(R.id.nav_notifications)
        val configuracionItem = bottomNav.findViewById<View>(R.id.nav_configuracion)

        secuenciaGuia = TapTargetSequence(this)
            .targets(
                TapTarget.forView(homeItem, "Inicio", "Vista principal y proximos recordatorios")
                    .cancelable(true).dimColor(android.R.color.black) ,
                TapTarget.forView(calendarioItem, "Citas", "Consulta tus citas pasadas y futuras")
                    .cancelable(true).dimColor(android.R.color.black) ,
                TapTarget.forView(notificacionItem, "Notificaciones", "Revisa alertas importantes")
                    .cancelable(true).dimColor(android.R.color.black) ,
                TapTarget.forView(configuracionItem, "Ajustes", "Configura tu perfil y preferencias")
                    .cancelable(true).dimColor(android.R.color.black)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                }

                override fun onSequenceStep(tapTarget: TapTarget, targetClicked: Boolean) {}

                override fun onSequenceCanceled(tapTarget: TapTarget) {
                }
            })

        secuenciaGuia?.start()

    }
}


