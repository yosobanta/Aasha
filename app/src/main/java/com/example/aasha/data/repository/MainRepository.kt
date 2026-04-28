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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MainRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val vaccinationDao: VaccinationDao,
    private val appointmentDao: AppointmentDao,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    // Flows for real-time local data
    val appointments: Flow<List<Appointment>> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else appointmentDao.getAllAppointments(id)
    }

    fun getVaccinationsByPatient(patientId: String): Flow<List<Vaccination>> =
        sessionManager.workerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else vaccinationDao.getVaccinationsByPatient(patientId, id)
        }

    fun getVisitsByPatient(patientId: String): Flow<List<Visit>> =
        sessionManager.workerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else visitDao.getVisitsByPatient(patientId, id)
        }

    // Dynamic Counts
    val appointmentCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else appointmentDao.getAppointmentCount(id)
    }

    val visitCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else visitDao.getVisitCount(id)
    }

    val vaccinationCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else vaccinationDao.getVaccinationCount(id)
    }

    // --- Operations ---

    suspend fun saveAppointment(appointment: Appointment) {
        val workerId = sessionManager.workerId.first() ?: ""
        val updatedAppointment = appointment.copy(
            workerId = workerId,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        appointmentDao.insertAppointment(updatedAppointment)
        triggerOneTimeSync()
    }

    suspend fun saveVaccination(vaccination: Vaccination) {
        val workerId = sessionManager.workerId.first() ?: ""
        val updatedVaccination = vaccination.copy(
            workerId = workerId,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        vaccinationDao.insertVaccination(updatedVaccination)
        triggerOneTimeSync()
    }

    suspend fun saveVisit(visit: Visit) {
        val workerId = sessionManager.workerId.first() ?: ""
        val updatedVisit = visit.copy(
            workerId = workerId,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        visitDao.insertVisit(updatedVisit)
        triggerOneTimeSync()
    }

    // --- Sync Logic ---

    suspend fun syncLocalWithRemote() {
        val workerId = sessionManager.workerId.first() ?: return

        // Sync Appointments
        val pendingAppointments = appointmentDao.getAppointmentsBySyncStatus(SyncStatus.PENDING)
        pendingAppointments.forEach { appointment ->
            try {
                firestore.collection("users").document(workerId)
                    .collection("appointments").document(appointment.id)
                    .set(appointment.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.ERROR))
            }
        }

        // Sync Visits
        val pendingVisits = visitDao.getVisitsBySyncStatus(SyncStatus.PENDING)
        pendingVisits.forEach { visit ->
            try {
                firestore.collection("users").document(workerId)
                    .collection("visits").document(visit.id)
                    .set(visit.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                visitDao.updateVisit(visit.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                visitDao.updateVisit(visit.copy(syncStatus = SyncStatus.ERROR))
            }
        }

        // Sync Vaccinations
        val pendingVaccinations = vaccinationDao.getVaccinationsBySyncStatus(SyncStatus.PENDING)
        pendingVaccinations.forEach { vaccination ->
            try {
                firestore.collection("users").document(workerId)
                    .collection("vaccinations").document(vaccination.id)
                    .set(vaccination.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                vaccinationDao.updateVaccination(vaccination.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                vaccinationDao.updateVaccination(vaccination.copy(syncStatus = SyncStatus.ERROR))
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
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }
}
