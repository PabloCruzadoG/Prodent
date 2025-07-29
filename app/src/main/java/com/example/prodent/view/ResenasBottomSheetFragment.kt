package com.example.prodent.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.prodent.R
import com.example.prodent.databinding.ActivityResenasDoctorBinding
import com.example.prodent.model.ReseñaConPaciente
import com.example.prodent.utils.decodeBase64ToBitmap
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class ResenasBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: ActivityResenasDoctorBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var doctorId: String
    private lateinit var doctorNombre: String

    companion object {
        fun newInstance(doctorId: String, doctorNombre: String): ResenasBottomSheetFragment {
            val fragment = ResenasBottomSheetFragment()
            val args = Bundle().apply {
                putString("doctorId", doctorId)
                putString("doctorNombre", doctorNombre)
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
        _binding = ActivityResenasDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("ResenasBottomSheet", "🚀 ResenasBottomSheetFragment iniciado")

        doctorId = arguments?.getString("doctorId") ?: ""
        doctorNombre = arguments?.getString("doctorNombre") ?: "Doctor"

        Log.d("ResenasBottomSheet", "📝 Datos recibidos:")
        Log.d("ResenasBottomSheet", "   Doctor ID: $doctorId")
        Log.d("ResenasBottomSheet", "   Doctor Nombre: $doctorNombre")

        if (doctorId.isEmpty()) {
            Log.e("ResenasBottomSheet", "❌ ERROR: doctorId está vacío!")
            dismiss()
            return
        }


        cargarResenasDoctor()
    }

    private fun cargarResenasDoctor() {
        mostrarCargando()

        Log.d("ResenasBottomSheet", "Cargando reseñas para doctor: $doctorId")

        firestore.collection("calificaciones")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("calificado", true)
            .get()
            .addOnSuccessListener { calificacionesResult ->
                Log.d("ResenasBottomSheet", "Calificaciones encontradas: ${calificacionesResult.size()}")

                if (calificacionesResult.isEmpty) {
                    mostrarSinResenas()
                    return@addOnSuccessListener
                }

                val resenas = mutableListOf<ReseñaConPaciente>()
                var procesadas = 0
                val total = calificacionesResult.size()

                for (calificacionDoc in calificacionesResult) {
                    val pacienteId = calificacionDoc.getString("pacienteId") ?: ""
                    val calificacion = calificacionDoc.getDouble("calificacion") ?: 0.0
                    val comentario = calificacionDoc.getString("comentario") ?: ""
                    val fecha = calificacionDoc.getString("fecha") ?: ""

                    Log.d("ResenasBottomSheet", "Procesando calificación - Paciente: $pacienteId, Calificación: $calificacion")

                    if (pacienteId.isNotEmpty()) {
                        firestore.collection("usuarios")
                            .document(pacienteId)
                            .get()
                            .addOnSuccessListener { pacienteDoc ->
                                procesadas++

                                if (pacienteDoc.exists()) {
                                    val nombrePaciente = pacienteDoc.getString("nombre") ?: "Paciente"
                                    val apellidoPaciente = pacienteDoc.getString("apellido") ?: ""
                                    val fotoPaciente = pacienteDoc.getString("fotoBase64") ?: ""

                                    val resena = ReseñaConPaciente(
                                        calificacion = calificacion,
                                        comentario = comentario,
                                        fecha = fecha,
                                        nombrePaciente = nombrePaciente,
                                        apellidoPaciente = apellidoPaciente,
                                        fotoPaciente = fotoPaciente
                                    )

                                    resenas.add(resena)
                                    Log.d("ResenasBottomSheet", "Reseña agregada: $nombrePaciente $apellidoPaciente - $calificacion")
                                }

                                if (procesadas == total) {
                                    mostrarResenas(resenas)
                                }
                            }
                            .addOnFailureListener { exception ->
                                procesadas++
                                Log.e("ResenasBottomSheet", "Error al cargar paciente: $pacienteId", exception)
                                if (procesadas == total) {
                                    mostrarResenas(resenas)
                                }
                            }
                    } else {
                        procesadas++
                        if (procesadas == total) {
                            mostrarResenas(resenas)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ResenasBottomSheet", "Error al cargar calificaciones", exception)
                mostrarSinResenas()
            }
    }

    private fun mostrarCargando() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollViewResenas.visibility = View.GONE
        binding.layoutSinResenas.visibility = View.GONE
    }

    private fun mostrarResenas(resenas: List<ReseñaConPaciente>) {
        binding.progressBar.visibility = View.GONE

        if (resenas.isEmpty()) {
            mostrarSinResenas()
            return
        }

        Log.d("ResenasBottomSheet", "Mostrando ${resenas.size} reseñas")

        val promedioCalificacion = resenas.map { it.calificacion }.average()
        val totalResenas = resenas.size

        binding.tvPromedioCalificacion.text = String.format("%.1f ⭐", promedioCalificacion)
        binding.tvTotalResenas.text = if (totalResenas == 1) "1 reseña" else "$totalResenas reseñas"

        val resenasOrdenadas = resenas.sortedWith(
            compareByDescending<ReseñaConPaciente> { it.calificacion }
                .thenByDescending { it.fecha }
        )

        binding.containerResenas.removeAllViews()

        resenasOrdenadas.forEach { resena ->
            agregarResenaALista(resena)
        }

        binding.scrollViewResenas.visibility = View.VISIBLE
        binding.layoutSinResenas.visibility = View.GONE
    }

    private fun agregarResenaALista(resena: ReseñaConPaciente) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_resena, binding.containerResenas, false)

        val imgPaciente = view.findViewById<ImageView>(R.id.imgPaciente)
        val tvNombrePaciente = view.findViewById<TextView>(R.id.tvNombrePaciente)
        val tvCalificacion = view.findViewById<TextView>(R.id.tvCalificacion)
        val tvComentario = view.findViewById<TextView>(R.id.tvComentario)
        val tvFecha = view.findViewById<TextView>(R.id.tvFecha)

        tvNombrePaciente.text = "${resena.nombrePaciente} ${resena.apellidoPaciente}"
        tvCalificacion.text = String.format("%.1f", resena.calificacion)
        tvFecha.text = resena.fecha

        if (resena.comentario.isNotEmpty()) {
            tvComentario.text = resena.comentario
            tvComentario.visibility = View.VISIBLE
        } else {
            tvComentario.text = "El paciente no dejó comentarios adicionales."
            tvComentario.visibility = View.VISIBLE
            tvComentario.alpha = 0.7f
        }

        if (resena.fotoPaciente.isNotEmpty()) {
            val bitmap = decodeBase64ToBitmap(resena.fotoPaciente)
            bitmap?.let {
                Glide.with(this)
                    .load(it)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_default_doctor)
                    .error(R.drawable.ic_default_doctor)
                    .into(imgPaciente)
            }
        } else {
            imgPaciente.setImageResource(R.drawable.ic_default_doctor)
        }

        configurarEstrellas(view, resena.calificacion)
        binding.containerResenas.addView(view)
    }

    private fun configurarEstrellas(view: View, calificacion: Double) {
        val estrellas = listOf(
            view.findViewById<ImageView>(R.id.estrella1),
            view.findViewById<ImageView>(R.id.estrella2),
            view.findViewById<ImageView>(R.id.estrella3),
            view.findViewById<ImageView>(R.id.estrella4),
            view.findViewById<ImageView>(R.id.estrella5)
        )

        for (i in estrellas.indices) {
            if (i < calificacion.toInt()) {
                estrellas[i].setImageResource(R.drawable.ic_star_filled)
            } else {
                estrellas[i].setImageResource(R.drawable.ic_star_empty)
            }
        }
    }

    private fun mostrarSinResenas() {
        binding.progressBar.visibility = View.GONE
        binding.scrollViewResenas.visibility = View.GONE
        binding.layoutSinResenas.visibility = View.VISIBLE
        binding.tvPromedioCalificacion.text = "Sin calificar"
        binding.tvTotalResenas.text = "0 reseñas"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}