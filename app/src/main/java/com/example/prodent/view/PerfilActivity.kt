package com.example.prodent.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prodent.databinding.ActivityPerfilBinding
import com.example.prodent.viewmodel.PerfilViewModel
import com.example.prodent.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel = PerfilViewModel()

    private var rolUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.selectedItemId = R.id.nav_configuracion

        viewModel.obtenerDatosUsuario { usuario ->
            binding.etNombre.setText(usuario.nombre)
            binding.etCorreo.setText(usuario.correo)
            rolUsuario = usuario.rol

            if (usuario.rol == "Paciente") {
                binding.etTelefono.setText(usuario.telefono)
                binding.etTelefono.visibility = View.VISIBLE
                binding.tvTelefonoLabel.visibility = View.VISIBLE
            } else {
                binding.etTelefono.visibility = View.GONE
                binding.tvTelefonoLabel.visibility = View.GONE
            }

            if (!usuario.fotoBase64.isNullOrEmpty()) {
                val bytes = android.util.Base64.decode(usuario.fotoBase64, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivFotoPerfil.setImageBitmap(bitmap)
            }

        }



        binding.btnGuardar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val correo = binding.etCorreo.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()

            if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rolUsuario == "Paciente" && !isPhoneValid(telefono)) {
                Toast.makeText(this, "Teléfono inválido (debe comenzar con 9 y tener 9 dígitos)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.actualizarDatosPerfil(
                nombre = nombre,
                correo = correo,
                telefono = telefono,
            ) { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        }


        binding.ivFotoPerfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, 1001)
        }

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
                    startActivity(Intent(this, ConfiguracionActivity::class.java))
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

    private fun convertirImagenABase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data
            uri?.let {
                binding.ivFotoPerfil.setImageURI(it)

                val base64 = convertirImagenABase64(it)
                if (!base64.isNullOrEmpty()) {
                    viewModel.actualizarFotoPerfil(base64) { mensaje ->
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun isPhoneValid(phone: String): Boolean {
        return phone.matches(Regex("^9\\d{8}$"))
    }

}
