package com.example.prodent.view

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.prodent.R
import com.example.prodent.model.Doctor
import com.example.prodent.utils.decodeBase64ToBitmap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class DoctorAdapter(
    private val doctores: List<Doctor>,
    private val onDoctorSelected: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctores[position]
        holder.bind(doctor, onDoctorSelected)
    }

    override fun getItemCount(): Int = doctores.size

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgDoctor: CircleImageView = itemView.findViewById(R.id.imgDoctor)
        private val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val tvDoctorSpecialty: TextView = itemView.findViewById(R.id.tvDoctorSpecialty)
        private val tvRatingNumber: TextView = itemView.findViewById(R.id.tvRatingNumber)
        private val tvReviewCount: TextView = itemView.findViewById(R.id.tvReviewCount)
        private val btnSelectDoctor: FloatingActionButton = itemView.findViewById(R.id.btnSelectDoctor)

        fun bind(doctor: Doctor, onDoctorSelected: (Doctor) -> Unit) {
            // Mostrar especialidad
            tvDoctorSpecialty.text = doctor.especialidad.ifEmpty { "Especialidad no disponible" }

            // ✅ BOTÓN SELECCIONAR DOCTOR (flecha azul)
            btnSelectDoctor.setOnClickListener {
                onDoctorSelected(doctor)
            }

            // ✅ AGREGAR CLICK EN LA IMAGEN DEL DOCTOR
            imgDoctor.setOnClickListener {
                Log.d("DoctorAdapter", "🔥 CLICK EN FOTO DEL DOCTOR!")
                Log.d("DoctorAdapter", "Doctor: ${doctor.nombre} ${doctor.apellido}")

                try {
                    // Crear el Bottom Sheet del perfil
                    val bottomSheet = PerfilDoctorBottomSheetFragment.newInstance(doctor)

                    // Obtener FragmentManager desde el contexto
                    val fragmentManager = when (val context = itemView.context) {
                        is androidx.fragment.app.FragmentActivity -> context.supportFragmentManager
                        else -> {
                            Log.e("DoctorAdapter", "❌ El contexto no es FragmentActivity")
                            return@setOnClickListener
                        }
                    }

                    // Mostrar el Bottom Sheet
                    bottomSheet.show(fragmentManager, "PerfilDoctorBottomSheet")
                    Log.d("DoctorAdapter", "✅ Bottom Sheet de perfil mostrado correctamente")

                } catch (e: Exception) {
                    Log.e("DoctorAdapter", "❌ Error al mostrar Bottom Sheet de perfil", e)
                    Toast.makeText(itemView.context, "Error al abrir perfil", Toast.LENGTH_SHORT).show()
                }
            }

            // Cargar datos del usuario (nombre y foto)
            if (!doctor.usuarioId.isNullOrEmpty() && doctor.usuarioId != "usuarios") {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(doctor.usuarioId)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val nombre = doc.getString("nombre") ?: doctor.nombre
                            val apellido = doc.getString("apellido") ?: doctor.apellido
                            tvDoctorName.text = "Dr. $nombre $apellido"

                            // Cargar foto circular con Glide
                            val fotoBase64 = doc.getString("fotoBase64")
                            if (!fotoBase64.isNullOrEmpty()) {
                                val bitmap = decodeBase64ToBitmap(fotoBase64)
                                bitmap?.let {
                                    Glide.with(itemView.context)
                                        .load(it)
                                        .apply(RequestOptions.circleCropTransform())
                                        .placeholder(R.drawable.ic_default_doctor)
                                        .error(R.drawable.ic_default_doctor)
                                        .into(imgDoctor)
                                }
                            } else {
                                imgDoctor.setImageResource(R.drawable.ic_default_doctor)
                            }

                            // ✅ DESPUÉS DE CARGAR USUARIO, CALCULAR CALIFICACIONES
                            calcularYMostrarCalificaciones(doctor.id, nombre, apellido)

                        } else {
                            tvDoctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
                            imgDoctor.setImageResource(R.drawable.ic_default_doctor)
                            calcularYMostrarCalificaciones(doctor.id, doctor.nombre, doctor.apellido)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("DoctorAdapter", "Error al cargar usuario: ${doctor.usuarioId}", exception)
                        tvDoctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
                        imgDoctor.setImageResource(R.drawable.ic_default_doctor)
                        calcularYMostrarCalificaciones(doctor.id, doctor.nombre, doctor.apellido)
                    }
            } else {
                tvDoctorName.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
                imgDoctor.setImageResource(R.drawable.ic_default_doctor)
                calcularYMostrarCalificaciones(doctor.id, doctor.nombre, doctor.apellido)
            }
        }

        // ✅ FUNCIÓN MEJORADA: Calcular calificaciones Y agregar click en reseñas
        private fun calcularYMostrarCalificaciones(doctorId: String, nombre: String, apellido: String) {
            FirebaseFirestore.getInstance()
                .collection("calificaciones")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("calificado", true)
                .get()
                .addOnSuccessListener { result ->
                    var totalCalificaciones = 0.0
                    var cantidadCalificaciones = 0

                    for (document in result) {
                        val calificacion = document.getDouble("calificacion") ?: 0.0
                        totalCalificaciones += calificacion
                        cantidadCalificaciones++
                    }

                    val calificacionPromedio = if (cantidadCalificaciones > 0) {
                        totalCalificaciones / cantidadCalificaciones
                    } else {
                        0.0
                    }

                    // ✅ MOSTRAR CALIFICACIÓN Y CANTIDAD DE RESEÑAS
                    if (cantidadCalificaciones > 0) {
                        tvRatingNumber.text = String.format("%.1f", calificacionPromedio)
                        tvReviewCount.text = if (cantidadCalificaciones == 1) {
                            "1 reseña"
                        } else {
                            "$cantidadCalificaciones reseñas"
                        }
                    } else {
                        tvRatingNumber.text = "Sin calificar"
                        tvReviewCount.text = "0 reseñas"
                    }

                    // ✅ CAMBIAR ESTA PARTE EN calcularYMostrarCalificaciones():
                    tvReviewCount.setOnClickListener {
                        val context = itemView.context

                        Log.d("DoctorAdapter", "🔥 CLICK EN RESEÑAS DETECTADO!")
                        Log.d("DoctorAdapter", "Doctor ID: $doctorId")
                        Log.d("DoctorAdapter", "Doctor Nombre: Dr. $nombre $apellido")

                        try {
                            // ✅ CAMBIAR A BOTTOM SHEET EN LUGAR DE INTENT
                            val bottomSheet = ResenasBottomSheetFragment.newInstance(
                                doctorId = doctorId,
                                doctorNombre = "Dr. $nombre $apellido"
                            )

                            val fragmentManager = when (context) {
                                is androidx.fragment.app.FragmentActivity -> context.supportFragmentManager
                                else -> {
                                    Log.e("DoctorAdapter", "Contexto no es FragmentActivity")
                                    return@setOnClickListener
                                }
                            }

                            bottomSheet.show(fragmentManager, "ResenasBottomSheet")
                            Log.d("DoctorAdapter", "✅ Bottom Sheet mostrado correctamente")

                        } catch (e: Exception) {
                            Log.e("DoctorAdapter", "❌ Error al mostrar Bottom Sheet", e)
                            Toast.makeText(context, "Error al abrir reseñas: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    tvRatingNumber.setOnClickListener {
                        val context = itemView.context
                        try {
                            val bottomSheet = ResenasBottomSheetFragment.newInstance(
                                doctorId = doctorId,
                                doctorNombre = "Dr. $nombre $apellido"
                            )

                            val fragmentManager = when (context) {
                                is androidx.fragment.app.FragmentActivity -> context.supportFragmentManager
                                else -> return@setOnClickListener
                            }

                            bottomSheet.show(fragmentManager, "ResenasBottomSheet")

                        } catch (e: Exception) {
                            Log.e("DoctorAdapter", "Error al mostrar Bottom Sheet", e)
                        }
                    }

                    // ✅ TAMBIÉN HACER CLICKEABLE EL RATING (opcional)
                    tvRatingNumber.setOnClickListener {
                        val context = itemView.context
                        val intent = Intent(context, ResenasBottomSheetFragment::class.java).apply {
                            putExtra("doctorId", doctorId)
                            putExtra("doctorNombre", "Dr. $nombre $apellido")
                        }
                        context.startActivity(intent)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DoctorAdapter", "Error al calcular calificaciones para doctor: $doctorId", exception)
                    tvRatingNumber.text = "Error"
                    tvReviewCount.text = "0 reseñas"
                }
        }
    }
}