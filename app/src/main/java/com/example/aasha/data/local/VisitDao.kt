package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.SyncStatus
import com.example.aasha.domain.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE patientId = :patientId AND workerId = :workerId")
    fun getVisitsByPatient(patientId: String, workerId: String): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE syncStatus = :status")
    suspend fun getVisitsBySyncStatus(status: SyncStatus): List<Visit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit)

    @Update
    suspend fun updateVisit(visit: Visit)

    @Query("SELECT COUNT(*) FROM visits WHERE workerId = :workerId")
    fun getVisitCount(workerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE workerId = :workerId AND syncStatus != 'SYNCED'")
    fun getPendingCount(workerId: String): Flow<Int>

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getVisitById(id: String): Visit?
}
