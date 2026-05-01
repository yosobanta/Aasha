package com.example.aasha.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.data.repository.PatientRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val patientRepository: PatientRepository,
    private val mainRepository: MainRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SYNC_DEBUG", "👷 SyncWorker: Background job started")

        return try {
            // Sync patients
            Log.d("SYNC_DEBUG", "👷 SyncWorker: Initiating Patient Sync...")
            patientRepository.syncLocalWithRemote()

            // Sync appointments, visits, etc.
            Log.d("SYNC_DEBUG", "👷 SyncWorker: Initiating Main Data Sync (Visits/Vaccinations/Appointments)...")
            mainRepository.syncLocalWithRemote()

            Log.d("SYNC_DEBUG", "✅ SyncWorker: ALL SYNC OPERATIONS COMPLETED SUCCESS")
            Result.success()
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "❌ SyncWorker: CRITICAL FAILURE: ${e.message}", e)
            Result.retry()
        }
    }
}
