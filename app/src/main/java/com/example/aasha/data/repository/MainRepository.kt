package com.example.aasha.data.repository

import android.content.Context
import androidx.work.*
import com.example.aasha.data.local.*
import com.example.aasha.data.sync.SyncWorker
import com.example.aasha.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val visitDao: VisitDao,
    private val vaccinationDao: VaccinationDao,
    private val appointmentDao: AppointmentDao,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    // Flows for real-time local data
    val patients: Flow<List<Patient>> = patientDao.getAllPatients()
    val appointments: Flow<List<Appointment>> = appointmentDao.getAllAppointments()

    fun getVaccinationsByPatient(patientId: String): Flow<List<Vaccination>> = 
        vaccinationDao.getVaccinationsByPatient(patientId)

    fun getVisitsByPatient(patientId: String): Flow<List<Visit>> = 
        visitDao.getVisitsByPatient(patientId)

    // --- Patient Operations ---

    suspend fun savePatient(patient: Patient) {
        val workerId = sessionManager.workerId.first() ?: ""
        val patientWithWorkerId = patient.copy(
            workerId = workerId,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        patientDao.insertPatient(patientWithWorkerId)
        triggerOneTimeSync()
    }

    suspend fun updatePatient(patient: Patient) {
        val updatedPatient = patient.copy(
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        patientDao.updatePatient(updatedPatient)
        triggerOneTimeSync()
    }

    suspend fun getPatientById(id: String): Patient? = patientDao.getPatientById(id)

    suspend fun isDuplicate(name: String, village: String, age: Int): Patient? {
        return patientDao.findDuplicate(name, village, age - 2, age + 2)
    }

    fun searchPatients(query: String): Flow<List<Patient>> = patientDao.searchPatients(query)

    // --- Appointment Operations ---

    suspend fun saveAppointment(appointment: Appointment) {
        val updatedAppointment = appointment.copy(
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        appointmentDao.insertAppointment(updatedAppointment)
        triggerOneTimeSync()
    }

    suspend fun saveVaccination(vaccination: Vaccination) {
        val updatedVaccination = vaccination.copy(
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        vaccinationDao.insertVaccination(updatedVaccination)
        triggerOneTimeSync()
    }

    suspend fun saveVisit(visit: Visit) {
        val updatedVisit = visit.copy(
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        visitDao.insertVisit(updatedVisit)
        triggerOneTimeSync()
    }

    // --- Sync Logic (Conflict Resolution: Last-Write-Wins) ---

    suspend fun syncLocalWithRemote() {
        val pendingPatients = patientDao.getPatientsBySyncStatus(SyncStatus.PENDING)
        pendingPatients.forEach { patient ->
            try {
                // Check remote version first
                val remoteDoc = firestore.collection("patients").document(patient.id).get().await()
                val remoteLastUpdated = remoteDoc.getLong("lastUpdated") ?: 0L
                
                if (patient.lastUpdated >= remoteLastUpdated) {
                    // Local is newer or same, push to remote
                    firestore.collection("patients").document(patient.id)
                        .set(patient.copy(syncStatus = SyncStatus.SYNCED), SetOptions.merge())
                        .await()
                    patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                } else {
                    // Remote is newer, pull to local
                    val remotePatient = remoteDoc.toObject(Patient::class.java)
                    if (remotePatient != null) {
                        patientDao.insertPatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            } catch (e: Exception) {
                patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.ERROR))
            }
        }

        // Similar logic for appointments, visits, vaccinations...
        val pendingAppointments = appointmentDao.getAppointmentsBySyncStatus(SyncStatus.PENDING)
        pendingAppointments.forEach { appointment ->
            try {
                firestore.collection("appointments").document(appointment.id)
                    .set(appointment.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.ERROR))
            }
        }
    }

    private fun triggerOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "one_time_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
