package com.example.aasha.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.aasha.data.local.*
import com.example.aasha.data.sync.SyncWorker
import com.example.aasha.data.worker.BoosterReminderWorker
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
import kotlinx.coroutines.flow.combine

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

    val pendingCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else {
            combine(
                appointmentDao.getPendingCount(id),
                visitDao.getPendingCount(id),
                vaccinationDao.getPendingCount(id)
            ) { a, v, vac -> a + v + vac }
        }
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
        
        if (updatedVaccination.requiresBooster && updatedVaccination.reminderTime != null) {
            scheduleBoosterReminder(updatedVaccination)
        }
        
        triggerOneTimeSync()
    }

    private fun scheduleBoosterReminder(vaccination: Vaccination) {
        val reminderTime = vaccination.reminderTime ?: return
        val delay = reminderTime - System.currentTimeMillis()
        
        if (delay > 0) {
            val data = Data.Builder()
                .putString("patientName", vaccination.patientName)
                .putString("vaccineName", vaccination.vaccineName)
                .build()

            val reminderRequest = OneTimeWorkRequestBuilder<BoosterReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "booster_reminder_${vaccination.id}",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
        }
    }

    suspend fun rescheduleAllReminders() {
        val currentTime = System.currentTimeMillis()
        val pendingBoosterVaccinations = vaccinationDao.getPendingBoosterVaccinations(currentTime)
        pendingBoosterVaccinations.forEach { vaccination ->
            scheduleBoosterReminder(vaccination)
        }
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
        Log.d("SYNC_DEBUG", "🚀 MainRepository: Starting Sync Cycle")
        val workerId = sessionManager.workerId.first() ?: run {
            Log.e("SYNC_DEBUG", "❌ MainRepository: WorkerId is null, sync aborted")
            return
        }

        // 1. PULL LOGIC
        try {
            fetchRemoteAppointments(workerId)
            fetchRemoteVisits(workerId)
            fetchRemoteVaccinations(workerId)
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "❌ MainRepository: Error during pull sync: ${e.message}")
        }

        // 2. PUSH LOGIC
        // Sync Appointments
        val pendingAppointments = appointmentDao.getAppointmentsBySyncStatus(SyncStatus.PENDING)
        Log.d("SYNC_DEBUG", "📅 MainRepository: Found ${pendingAppointments.size} pending appointments")
        pendingAppointments.forEach { appointment ->
            try {
                Log.d("SYNC_DEBUG", "🔄 MainRepository: Syncing appointment ${appointment.id}")
                firestore.collection("users").document(workerId)
                    .collection("appointments").document(appointment.id)
                    .set(appointment.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.SYNCED))
                Log.d("SYNC_DEBUG", "✅ MainRepository: Sync success for appointment ${appointment.id}")
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "❌ MainRepository: Sync failed for appointment ${appointment.id}: ${e.message}")
                appointmentDao.updateAppointment(appointment.copy(syncStatus = SyncStatus.ERROR))
            }
        }

        // Sync Visits
        val pendingVisits = visitDao.getVisitsBySyncStatus(SyncStatus.PENDING)
        Log.d("SYNC_DEBUG", "🏥 MainRepository: Found ${pendingVisits.size} pending visits")
        pendingVisits.forEach { visit ->
            try {
                Log.d("SYNC_DEBUG", "🔄 MainRepository: Syncing visit ${visit.id}")
                firestore.collection("users").document(workerId)
                    .collection("visits").document(visit.id)
                    .set(visit.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                visitDao.updateVisit(visit.copy(syncStatus = SyncStatus.SYNCED))
                Log.d("SYNC_DEBUG", "✅ MainRepository: Sync success for visit ${visit.id}")
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "❌ MainRepository: Sync failed for visit ${visit.id}: ${e.message}")
                visitDao.updateVisit(visit.copy(syncStatus = SyncStatus.ERROR))
            }
        }

        // Sync Vaccinations
        val pendingVaccinations = vaccinationDao.getVaccinationsBySyncStatus(SyncStatus.PENDING)
        Log.d("SYNC_DEBUG", "💉 MainRepository: Found ${pendingVaccinations.size} pending vaccinations")
        pendingVaccinations.forEach { vaccination ->
            try {
                Log.d("SYNC_DEBUG", "🔄 MainRepository: Syncing vaccination ${vaccination.id}")
                firestore.collection("users").document(workerId)
                    .collection("vaccinations").document(vaccination.id)
                    .set(vaccination.copy(syncStatus = SyncStatus.SYNCED))
                    .await()
                vaccinationDao.updateVaccination(vaccination.copy(syncStatus = SyncStatus.SYNCED))
                Log.d("SYNC_DEBUG", "✅ MainRepository: Sync success for vaccination ${vaccination.id}")
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "❌ MainRepository: Sync failed for vaccination ${vaccination.id}: ${e.message}")
                vaccinationDao.updateVaccination(vaccination.copy(syncStatus = SyncStatus.ERROR))
            }
        }
        Log.d("SYNC_DEBUG", "🏁 MainRepository: Sync Cycle Finished")
    }
    private suspend fun fetchRemoteAppointments(workerId: String) {
        val lastSyncTime = sessionManager.lastSyncTime.first()
        val snapshot = firestore.collection("users").document(workerId)
            .collection("appointments")
            .whereGreaterThan("lastUpdated", lastSyncTime)
            .get().await()

        for (doc in snapshot.documents) {
            val remote = doc.toObject(Appointment::class.java) ?: continue
            val local = appointmentDao.getAppointmentById(remote.id)
            if (local == null || remote.lastUpdated > local.lastUpdated) {
                appointmentDao.insertAppointment(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }

    private suspend fun fetchRemoteVisits(workerId: String) {
        val lastSyncTime = sessionManager.lastSyncTime.first()
        val snapshot = firestore.collection("users").document(workerId)
            .collection("visits")
            .whereGreaterThan("lastUpdated", lastSyncTime)
            .get().await()

        for (doc in snapshot.documents) {
            val remote = doc.toObject(Visit::class.java) ?: continue
            val local = visitDao.getVisitById(remote.id)
            if (local == null || remote.lastUpdated > local.lastUpdated) {
                visitDao.insertVisit(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }

    private suspend fun fetchRemoteVaccinations(workerId: String) {
        val lastSyncTime = sessionManager.lastSyncTime.first()
        val snapshot = firestore.collection("users").document(workerId)
            .collection("vaccinations")
            .whereGreaterThan("lastUpdated", lastSyncTime)
            .get().await()

        for (doc in snapshot.documents) {
            val remote = doc.toObject(Vaccination::class.java) ?: continue
            val local = vaccinationDao.getVaccinationById(remote.id)
            if (local == null || remote.lastUpdated > local.lastUpdated) {
                vaccinationDao.insertVaccination(remote.copy(syncStatus = SyncStatus.SYNCED))
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
