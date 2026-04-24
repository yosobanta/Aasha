package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE syncStatus = :status")
    suspend fun getPatientsBySyncStatus(status: SyncStatus): List<Patient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Update
    suspend fun updatePatient(patient: Patient)

    @Query("SELECT * FROM patients WHERE isDeleted = 0 AND name = :name AND village = :village AND age BETWEEN :minAge AND :maxAge LIMIT 1")
    suspend fun findDuplicate(name: String, village: String, minAge: Int, maxAge: Int): Patient?

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: String): Patient?

    @Query("SELECT COUNT(*) FROM patients WHERE syncStatus != 'SYNCED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM patients WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchPatients(query: String): Flow<List<Patient>>
}
