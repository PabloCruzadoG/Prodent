package com.example.prodent.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prodent.model.Cita
import com.example.prodent.model.Doctor
import com.example.prodent.model.Horario
import com.google.firebase.firestore.FirebaseFirestore

class CitaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // LiveData que expone la lista de citas pendientes
    private val _citasPendientes = MutableLiveData<List<Cita>>()
    val citasPendientes: LiveData<List<Cita>> get() = _citasPendientes

    // LiveData para saber si la cita fue guardada con éxito
    private val _citaGuardada = MutableLiveData<Boolean>()
    val citaGuardada: LiveData<Boolean> get() = _citaGuardada

    // LiveData para los doctores
    private val _doctores = MutableLiveData<List<Doctor>>()
    val doctores: LiveData<List<Doctor>> get() = _doctores

    private val _horarios = MutableLiveData<List<Horario>>()
    val horarios: LiveData<List<Horario>> get() = _horarios

    // LiveData para citas pendientes de pacientes
    private val _citasPendientesPacientes = MutableLiveData<List<Cita>>()
    val citasPendientesPacientes: LiveData<List<Cita>> get() = _citasPendientesPacientes

    // ✅ FUNCIÓN CORREGIDA: Cargar doctores desde ambas colecciones
    fun cargarDoctores() {
        Log.d("CitaViewModel", "Iniciando carga de doctores...")

        // Primero obtenemos usuarios que son doctores
        db.collection("usuarios")
            .whereEqualTo("rol", "Doctor")
            .get()
            .addOnSuccessListener { usuariosResult ->
                Log.d("CitaViewModel", "Usuarios doctores encontrados: ${usuariosResult.size()}")

                val doctoresList = mutableListOf<Doctor>()
                var usuariosProcesados = 0
                val totalUsuarios = usuariosResult.size()

                if (totalUsuarios == 0) {
                    Log.w("CitaViewModel", "No se encontraron usuarios con rol Doctor")
                    _doctores.value = emptyList()
                    return@addOnSuccessListener
                }

                // Para cada usuario doctor, buscar su documento en la colección "doctores"
                for (usuarioDoc in usuariosResult) {
                    val usuarioId = usuarioDoc.id
                    val nombre = usuarioDoc.getString("nombre") ?: ""
                    val apellido = usuarioDoc.getString("apellido") ?: ""

                    Log.d("CitaViewModel", "Procesando usuario doctor: $usuarioId - $nombre $apellido")

                    // Buscar en la colección "doctores" por usuarioId
                    db.collection("doctores")
                        .whereEqualTo("usuarioId", usuarioId)
                        .get()
                        .addOnSuccessListener { doctoresResult ->
                            usuariosProcesados++

                            if (!doctoresResult.isEmpty) {
                                val doctorDoc = doctoresResult.documents[0]
                                val especialidad = doctorDoc.getString("especialidad") ?: "Especialidad no especificada"
                                val calificacionPromedio = doctorDoc.getDouble("calificacionPromedio") ?: 0.0

                                Log.d("CitaViewModel", "Doctor encontrado: $usuarioId - especialidad: $especialidad")

                                val doctor = Doctor(
                                    id = usuarioId, // Usar el ID del usuario
                                    usuarioId = usuarioId,
                                    especialidad = especialidad,
                                    nombre = nombre,
                                    apellido = apellido,
                                    calificacionPromedio = calificacionPromedio
                                )

                                doctoresList.add(doctor)
                                Log.d("CitaViewModel", "Doctor agregado a la lista: $doctor")
                            } else {
                                Log.w("CitaViewModel", "No se encontró documento en 'doctores' para usuario: $usuarioId")

                                // Crear doctor con datos básicos aunque no tenga documento en "doctores"
                                val doctor = Doctor(
                                    id = usuarioId,
                                    usuarioId = usuarioId,
                                    especialidad = "Especialidad no especificada",
                                    nombre = nombre,
                                    apellido = apellido,
                                    calificacionPromedio = 0.0
                                )

                                doctoresList.add(doctor)
                                Log.d("CitaViewModel", "Doctor agregado con datos básicos: $doctor")
                            }

                            // Si hemos procesado todos los usuarios, actualizar LiveData
                            if (usuariosProcesados == totalUsuarios) {
                                Log.d("CitaViewModel", "Carga completada. Total doctores: ${doctoresList.size}")
                                _doctores.value = doctoresList
                            }
                        }
                        .addOnFailureListener { exception ->
                            usuariosProcesados++
                            Log.e("CitaViewModel", "Error al buscar doctor para usuario: $usuarioId", exception)

                            // Crear doctor con datos básicos aunque falle la búsqueda
                            val doctor = Doctor(
                                id = usuarioId,
                                usuarioId = usuarioId,
                                especialidad = "Error al cargar especialidad",
                                nombre = nombre,
                                apellido = apellido,
                                calificacionPromedio = 0.0
                            )

                            doctoresList.add(doctor)

                            // Si hemos procesado todos los usuarios, actualizar LiveData
                            if (usuariosProcesados == totalUsuarios) {
                                Log.d("CitaViewModel", "Carga completada con errores. Total doctores: ${doctoresList.size}")
                                _doctores.value = doctoresList
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CitaViewModel", "Error al cargar usuarios doctores", exception)
                _doctores.value = emptyList()
            }
    }

    // Cargar citas por fecha
    fun cargarCitasPorFecha(doctorId: String, fecha: String) {
        Log.d("CitaViewModel", "Cargando citas para doctor: $doctorId, fecha: $fecha")

        db.collection("citas")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("estado", "pendiente")
            .whereEqualTo("fecha", fecha)
            .get()
            .addOnSuccessListener { result ->
                val citas = result.map { it.toObject(Cita::class.java) }
                Log.d("CitaViewModel", "Citas encontradas: ${citas.size}")
                _citasPendientes.value = citas
            }
            .addOnFailureListener { exception ->
                Log.e("CitaViewModel", "Error al cargar citas por fecha", exception)
                _citasPendientes.value = emptyList()
            }
    }

    // Obtener citas de un paciente
    fun obtenerCitasPaciente(uid: String) {
        Log.d("CitaViewModel", "Obteniendo citas para paciente: $uid")

        db.collection("citas")
            .whereEqualTo("paciente", uid) // ✅ Corregido: usar "paciente" en lugar de "pacienteId"
            .whereEqualTo("estado", "pendiente")
            .get()
            .addOnSuccessListener { result ->
                val citas = result.map { it.toObject(Cita::class.java) }
                Log.d("CitaViewModel", "Citas del paciente encontradas: ${citas.size}")
                _citasPendientesPacientes.value = citas
            }
            .addOnFailureListener { exception ->
                Log.e("CitaViewModel", "Error al obtener citas del paciente", exception)
                _citasPendientesPacientes.value = emptyList()
            }
    }

    // Guardar una nueva cita
    fun guardarCita(cita: Cita) {
        Log.d("CitaViewModel", "Guardando nueva cita: $cita")

        db.collection("citas")
            .add(cita)
            .addOnSuccessListener { documentReference ->
                Log.d("CitaViewModel", "Cita guardada exitosamente con ID: ${documentReference.id}")
                _citaGuardada.value = true
            }
            .addOnFailureListener { exception ->
                Log.e("CitaViewModel", "Error al guardar cita", exception)
                _citaGuardada.value = false
            }
    }

    // Cargar horarios disponibles para un doctor y una fecha
    fun cargarHorarios(doctorId: String, fecha: String) {
        Log.d("CitaViewModel", "Cargando horarios para doctor: $doctorId, fecha: $fecha")

        val horariosRef = db.collection("horarios")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("fecha", fecha)

        val citasRef = db.collection("citas")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("fecha", fecha)

        horariosRef.get().addOnSuccessListener { horariosDocs ->
            val horariosDisponibles = horariosDocs.map { it.toObject(Horario::class.java) }.toMutableList()
            Log.d("CitaViewModel", "Horarios disponibles encontrados: ${horariosDisponibles.size}")


            citasRef.get().addOnSuccessListener { citasDocs ->
                val horasReservadas = citasDocs.mapNotNull { it.getString("hora") }
                Log.d("CitaViewModel", "Horas ya reservadas: $horasReservadas")

                // Filtrar horarios que no están reservados
                val filtrados = horariosDisponibles.filter { horario ->
                    val horaCompleta = "${horario.horaInicio} - ${horario.horaFin}"
                    !horasReservadas.contains(horaCompleta)
                }

                Log.d("CitaViewModel", "Horarios filtrados disponibles: ${filtrados.size}")
                _horarios.value = filtrados
            }.addOnFailureListener { exception ->
                Log.e("CitaViewModel", "Error al cargar citas para filtrar horarios", exception)
                _horarios.value = horariosDisponibles // Si no se pudo obtener citas, mostrar todos
            }
        }.addOnFailureListener { exception ->
            Log.e("CitaViewModel", "Error al cargar horarios", exception)
            _horarios.value = emptyList()
        }
    }

    // ✅ FUNCIÓN ADICIONAL: Limpiar datos cuando se destruye el ViewModel
    override fun onCleared() {
        super.onCleared()
        Log.d("CitaViewModel", "ViewModel destruido - limpiando recursos")
    }
}