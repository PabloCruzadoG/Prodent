package com.example.prodent.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.prodent.R
import com.example.prodent.databinding.BottomSheetPerfilDoctorBinding
import com.example.prodent.model.Doctor
import com.example.prodent.utils.decodeBase64ToBitmap
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class PerfilDoctorBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPerfilDoctorBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var doctor: Doctor

    companion object {
        fun newInstance(doctor: Doctor): PerfilDoctorBottomSheetFragment {
            val fragment = PerfilDoctorBottomSheetFragment()
            val args = Bundle().apply {
                putString("doctorId", doctor.id)
                putString("doctorUsuarioId", doctor.usuarioId)
                putString("doctorNombre", doctor.nombre)
                putString("doctorApellido", doctor.apellido)
                putString("doctorEspecialidad", doctor.especialidad)
                putDouble("doctorCalificacion", doctor.calificacionPromedio)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPerfilDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val doctorId = arguments?.getString("doctorId") ?: ""
        val doctorUsuarioId = arguments?.getString("doctorUsuarioId") ?: ""
        val doctorNombre = arguments?.getString("doctorNombre") ?: ""
        val doctorApellido = arguments?.getString("doctorApellido") ?: ""
        val doctorEspecialidad = arguments?.getString("doctorEspecialidad") ?: ""

        doctor = Doctor(
            id = doctorId,
            usuarioId = doctorUsuarioId,
            nombre = doctorNombre,
            apellido = doctorApellido,
            especialidad = doctorEspecialidad,
            calificacionPromedio = 0.0
        )

        setupPerfilDoctor()
        cargarDatosUsuario()
        calcularEstadisticasReales()
    }

    private fun setupPerfilDoctor() {
        binding.tvNombreDoctor.text = "Dr. ${doctor.nombre} ${doctor.apellido}"
        binding.tvEspecialidadDoctor.text = doctor.especialidad.ifEmpty { "Especialidad no especificada" }

        binding.tvCalificacionDoctor.text = "Cargando..."
        binding.tvPacientesDoctor.text = "Cargando..."
        binding.tvEmailDoctor.text = "Cargando..."
    }

    private fun calcularEstadisticasReales() {
        firestore.collection("calificaciones")
            .whereEqualTo("doctorId", doctor.usuarioId)
            .whereEqualTo("calificado", true)
            .get()
            .addOnSuccessListener { calificacionesResult ->
                var totalCalificaciones = 0.0
                var cantidadCalificaciones = 0

                for (document in calificacionesResult) {
                    val calificacion = document.getDouble("calificacion") ?: 0.0
                    totalCalificaciones += calificacion
                    cantidadCalificaciones++
                }

                val calificacionPromedio = if (cantidadCalificaciones > 0) {
                    totalCalificaciones / cantidadCalificaciones
                } else {
                    0.0
                }

                if (calificacionPromedio > 0) {
                    binding.tvCalificacionDoctor.text = String.format("%.1f", calificacionPromedio)
                } else {
                    binding.tvCalificacionDoctor.text = "--"
                }

                binding.tvPacientesDoctor.text = if (cantidadCalificaciones > 0) "$cantidadCalificaciones" else "0"
            }
            .addOnFailureListener {
                binding.tvCalificacionDoctor.text = "--"
                binding.tvPacientesDoctor.text = "--"
            }
    }

    private fun cargarDatosUsuario() {
        if (doctor.usuarioId.isNotEmpty()) {
            firestore.collection("usuarios")
                .document(doctor.usuarioId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val correo = doc.getString("correo")
                        val fotoBase64 = doc.getString("fotoBase64")

                        // Cargar imagen
                        if (!fotoBase64.isNullOrEmpty()) {
                            val bitmap = decodeBase64ToBitmap(fotoBase64)
                            bitmap?.let {
                                Glide.with(this)
                                    .load(it)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_default_doctor)
                                    .error(R.drawable.ic_default_doctor)
                                    .into(binding.imgDoctorPerfil)
                            }
                        } else {
                            binding.imgDoctorPerfil.setImageResource(R.drawable.ic_default_doctor)
                        }

                        val email = correo ?: "No disponible"
                        binding.tvEmailDoctor.text = email
                    } else {
                        setearDatosPorDefecto()
                    }
                }
                .addOnFailureListener {
                    setearDatosPorDefecto()
                }
        } else {
            setearDatosPorDefecto()
        }
    }

    private fun setearDatosPorDefecto() {
        binding.imgDoctorPerfil.setImageResource(R.drawable.ic_default_doctor)
        binding.tvEmailDoctor.text = "No disponible"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}