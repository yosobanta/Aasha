package com.example.aasha.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val patientName: String,
    val dateTime: Long,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
