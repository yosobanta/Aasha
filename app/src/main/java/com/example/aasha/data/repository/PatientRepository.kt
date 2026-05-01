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
        Log.d("SYNC_DEBUG", "🚀 Starting Sync Cycle [Local <-> Remote]")
        val workerId = sessionManager.workerId.first()
        if (workerId.isNullOrEmpty()) {
            Log.e("SYNC_DEBUG", "❌ WorkerId is null or empty, sync aborted")
            return
        }

        try {
            // 1. PULL LOGIC (Firebase -> Local)
            Log.d("SYNC_DEBUG", "📥 PULL: Fetching updates from Firestore for worker: $workerId")
            fetchRemotePatients(workerId)

            // 2. PUSH LOGIC (Local -> Firebase)
            Log.d("SYNC_DEBUG", "📤 PUSH: Checking for PENDING local changes")
            val pendingPatients = patientDao.getPatientsBySyncStatus(SyncStatus.PENDING)
            Log.d("SYNC_DEBUG", "📊 Found ${pendingPatients.size} patients pending sync")

            var successCount = 0
            var errorCount = 0

            pendingPatients.forEach { patient ->
                try {
                    Log.d("SYNC_DEBUG", "🔄 Syncing patient: ${patient.id} | Name: ${patient.name}")
                    val docRef = firestore.collection("users").document(workerId).collection("patients").document(patient.id)

                    if (patient.isDeleted) {
                        Log.d("SYNC_DEBUG", "🗑️ Patient ${patient.id} is deleted locally. Deleting from remote...")
                        docRef.delete().await()
                        patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                        Log.d("SYNC_DEBUG", "✅ Successfully deleted remote document for ${patient.id}")
                    } else {
                        val remoteDoc = docRef.get().await()
                        if (!remoteDoc.exists()) {
                            Log.d("SYNC_DEBUG", "🆕 Patient ${patient.id} does not exist on remote. Creating...")
                            docRef.set(patient.copy(syncStatus = SyncStatus.SYNCED)).await()
                            patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                            Log.d("SYNC_DEBUG", "✅ Successfully created remote document for ${patient.id}")
                        } else {
                            val remoteLastUpdated = remoteDoc.getLong("lastUpdated") ?: 0L
                            Log.d("SYNC_DEBUG", "⚖️ Conflict Check [${patient.id}]: Local(${patient.lastUpdated}) vs Remote($remoteLastUpdated)")

                            if (patient.lastUpdated >= remoteLastUpdated) {
                                Log.d("SYNC_DEBUG", "🔼 Local is newer. Pushing update for ${patient.id}")
                                docRef.set(patient.copy(syncStatus = SyncStatus.SYNCED), SetOptions.merge()).await()
                                patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.SYNCED))
                                Log.d("SYNC_DEBUG", "✅ Successfully updated remote document for ${patient.id}")
                            } else {
                                Log.d("SYNC_DEBUG", "🔽 Remote is newer. Pulling update for ${patient.id}")
                                val remotePatient = remoteDoc.toObject(Patient::class.java)
                                if (remotePatient != null) {
                                    patientDao.insertPatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                                    Log.d("SYNC_DEBUG", "✅ Successfully updated local record for ${patient.id}")
                                }
                            }
                        }
                    }
                    successCount++
                } catch (e: Exception) {
                    Log.e("SYNC_DEBUG", "❌ Error syncing patient ${patient.id}: ${e.message}")
                    patientDao.updatePatient(patient.copy(syncStatus = SyncStatus.ERROR))
                    errorCount++
                }
            }

            sessionManager.updateLastSyncTime(System.currentTimeMillis())
            Log.d("SYNC_DEBUG", "🏁 Sync Cycle Finished. Success: $successCount, Errors: $errorCount")
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "💥 Critical failure in syncLocalWithRemote: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchRemotePatients(workerId: String) {
        val lastSyncTimeValue = sessionManager.lastSyncTime.first()
        Log.d("SYNC_DEBUG", "🔍 Querying Firestore for patients updated after: $lastSyncTimeValue")

        try {
            val querySnapshot = firestore.collection("users").document(workerId).collection("patients")
                .whereGreaterThan("lastUpdated", lastSyncTimeValue)
                .get()
                .await()

            Log.d("SYNC_DEBUG", "📥 Firestore returned ${querySnapshot.size()} documents")

            for (doc in querySnapshot.documents) {
                val remotePatient = doc.toObject(Patient::class.java) ?: continue
                Log.d("SYNC_DEBUG", "💾 Processing fetched patient: ${remotePatient.id}")
                val localPatient = patientDao.getPatientById(remotePatient.id)

                if (localPatient == null) {
                    Log.d("SYNC_DEBUG", "➕ New remote patient found. Inserting locally: ${remotePatient.id}")
                    patientDao.insertPatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                } else {
                    if (remotePatient.lastUpdated > localPatient.lastUpdated) {
                        Log.d("SYNC_DEBUG", "🆙 Remote update found for existing patient. Updating locally: ${remotePatient.id}")
                        patientDao.updatePatient(remotePatient.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        Log.d("SYNC_DEBUG", "⏭️ Local record is already up-to-date for ${remotePatient.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "❌ Error during fetchRemotePatients: ${e.message}")
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