package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE workerId = :workerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllPatients(workerId: String): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE syncStatus = :status")
    suspend fun getPatientsBySyncStatus(status: SyncStatus): List<Patient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Update
    suspend fun updatePatient(patient: Patient)

    @Query("SELECT * FROM patients WHERE isDeleted = 0 AND workerId = :workerId AND name = :name AND village = :village AND age BETWEEN :minAge AND :maxAge LIMIT 1")
    suspend fun findDuplicate(workerId: String, name: String, village: String, minAge: Int, maxAge: Int): Patient?

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: String): Patient?

    @Query("SELECT COUNT(*) FROM patients WHERE workerId = :workerId AND syncStatus != 'SYNCED'")
    fun getPendingCount(workerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM patients WHERE workerId = :workerId AND isDeleted = 0")
    fun getPatientCount(workerId: String): Flow<Int>

    @Query("SELECT * FROM patients WHERE workerId = :workerId AND isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchPatients(workerId: String, query: String): Flow<List<Patient>>
}
