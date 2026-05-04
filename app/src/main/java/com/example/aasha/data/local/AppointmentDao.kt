package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.Appointment
import com.example.aasha.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE workerId = :workerId ORDER BY dateTime DESC")
    fun getAllAppointments(workerId: String): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE syncStatus = :status")
    suspend fun getAppointmentsBySyncStatus(status: SyncStatus): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Query("SELECT COUNT(*) FROM appointments WHERE workerId = :workerId")
    fun getAppointmentCount(workerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM appointments WHERE workerId = :workerId AND syncStatus != 'SYNCED'")
    fun getPendingCount(workerId: String): Flow<Int>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: String): Appointment?

    @Query("UPDATE appointments SET isCompleted = :isCompleted, lastUpdated = :lastUpdated, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateAppointmentCompletion(id: String, isCompleted: Boolean, lastUpdated: Long, syncStatus: SyncStatus)
}
