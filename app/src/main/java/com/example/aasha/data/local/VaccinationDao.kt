package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.SyncStatus
import com.example.aasha.domain.model.Vaccination
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccinationDao {
    @Query("SELECT * FROM vaccinations WHERE patientId = :patientId")
    fun getVaccinationsByPatient(patientId: String): Flow<List<Vaccination>>

    @Query("SELECT * FROM vaccinations WHERE syncStatus = :status")
    suspend fun getVaccinationsBySyncStatus(status: SyncStatus): List<Vaccination>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(vaccination: Vaccination)

    @Update
    suspend fun updateVaccination(vaccination: Vaccination)
}
