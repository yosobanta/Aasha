package com.example.aasha.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.aasha.data.local.PatientDao
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.sync.SyncWorker
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.SyncStatus
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
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    val patients: Flow<List<Patient>> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else patientDao.getAllPatients(id)
    }

    val pendingCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else patientDao.getPendingCount(id)
    }

    val patientCount: Flow<Int> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(0) else patientDao.getPatientCount(id)
    }

    val lastSyncTime: Flow<Long> = sessionManager.lastSyncTime

    fun getPatientsFlow(): Flow<List<Patient>> = patients

    fun searchPatients(query: String): Flow<List<Patient>> = sessionManager.workerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else patientDao.searchPatients(id, query)
    }

    suspend fun getPatientById(id: String): Patient? = patientDao.getPatientById(id)

    suspend fun isDuplicate(name: String, village: String, age: Int): Patient? {
        val workerId = sessionManager.workerId.first() ?: ""
        return patientDao.findDuplicate(workerId, name, village, age - 2, age + 2)
    }

    suspend fun addPatient(patient: Patient) {
        val workerId = sessionManager.workerId.first() ?: ""
        val newPatient = patient.copy(
            workerId = workerId,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        patientDao.insertPatient(newPatient)
        triggerSync()
    }

    suspend fun updatePatient(patient: Patient) {
        val updatedPatient = patient.copy(
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        patientDao.updatePatient(updatedPatient)
        triggerSync()
    }

    suspend fun deletePatient(patientId: String) {
        val patient = patientDao.getPatientById(patientId) ?: return
        val deletedPatient = patient.copy(
            isDeleted = true,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        patientDao.updatePatient(deletedPatient)
        triggerSync()
    }

    suspend fun syncLocalWithRemote() {
        Log.d("SYNC_DEBUG", "🔥🔥 syncLocalWithRemote STARTED")
        val workerId = sessionManager.workerId.first()
        if (workerId.isNullOrEmpty()) {
            Log.e("SYNC_DEBUG", "❌ WorkerId is null or empty, skipping sync")
            return
        }

        try {
            // 1. PULL LOGIC (Firebase -> Local)
            Log.d("SYNC_DEBUG", "📥 Starting PULL logic")
            fetchRemotePatients(workerId)

            // 2. PUSH LOGIC (Local -> Firebase)
            Log.d("SYNC_DEBUG", "📤 Starting PUSH logic")
            val pendingPatients = patientDao.getPatientsBySyncStatus(SyncStatus.PENDING)
            Log.d("SYNC_DEBUG", "Pending patients count: ${pendingPatients.size}")

            pendingPatients.forEach { patient ->
                try {
                    Log.d("SYNC_DEBUG", "Processing patient: ${patient.id} (${patient.name})")
                    val docRef = firestore.collection("users").document(workerId).collection("patients").document(patient.id)
                    val remoteDoc = docRef.get().await()
                    
                    if (!remoteDoc.exists()) {
                        Log.d("SYNC_DEBUG", "Remote doc missing, creating for: ${patient.id}")
                        docRef.set(patient.copy(syncStatus = SyncStatus.SYNCED)).await()
                        patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        val remoteLastUpdated = remoteDoc.getLong("lastUpdated") ?: 0L
                        Log.d("SYNC_DEBUG", "Comparing timestamps: Local(${patient.lastUpdated}) vs Remote($remoteLastUpdated)")
                        
                        if (patient.lastUpdated >= remoteLastUpdated) {
                            Log.d("SYNC_DEBUG", "Local is newer/same, pushing to remote: ${patient.id}")
                            docRef.set(patient.copy(syncStatus = SyncStatus.SYNCED), SetOptions.merge()).await()
                            patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                        } else {
                            Log.d("SYNC_DEBUG", "Remote is newer, pulling to local: ${patient.id}")
                            val remotePatient = remoteDoc.toObject(Patient::class.java)
                            if (remotePatient != null) {
                                patientDao.insertPatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SYNC_DEBUG", "❌ Error syncing patient ${patient.id}: ${e.message}")
                    patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.ERROR))
                }
            }
            
            sessionManager.updateLastSyncTime(System.currentTimeMillis())
            Log.d("SYNC_DEBUG", "✅ syncLocalWithRemote FINISHED SUCCESS")
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "❌ syncLocalWithRemote FAILED: ${e.message}")
            throw e
        }
    }

    suspend fun fetchRemotePatients(workerId: String) {
        val lastSyncTimeValue = sessionManager.lastSyncTime.first()
        Log.d("SYNC_DEBUG", "Fetching remote patients since: $lastSyncTimeValue")
        
        try {
            val querySnapshot = firestore.collection("users").document(workerId).collection("patients")
                .whereGreaterThan("lastUpdated", lastSyncTimeValue)
                .get()
                .await()

            Log.d("SYNC_DEBUG", "Found ${querySnapshot.size()} updated patients in Firestore")

            for (doc in querySnapshot.documents) {
                val remotePatient = doc.toObject(Patient::class.java) ?: continue
                Log.d("SYNC_DEBUG", "Processing remote patient: ${remotePatient.id}")
                val localPatient = patientDao.getPatientById(remotePatient.id)

                if (localPatient == null) {
                    Log.d("SYNC_DEBUG", "New remote patient, inserting: ${remotePatient.id}")
                    patientDao.insertPatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                } else {
                    if (remotePatient.lastUpdated > localPatient.lastUpdated) {
                        Log.d("SYNC_DEBUG", "Remote patient is newer, updating local: ${remotePatient.id}")
                        patientDao.updatePatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        Log.d("SYNC_DEBUG", "Local patient is newer or same, skipping pull for: ${remotePatient.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "❌ Error fetching remote patients: ${e.message}")
            throw e
        }
    }

    fun triggerSync() {
        Log.d("SYNC_DEBUG", "🚀 triggerSync called (Manual)")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
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
            "patient_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun enqueuePeriodicSync() {
        Log.d("SYNC_DEBUG", "🕒 enqueuePeriodicSync called")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
