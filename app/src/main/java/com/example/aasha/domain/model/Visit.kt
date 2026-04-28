package com.example.aasha.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val patientId: String = "",
    val patientName: String = "",
    val workerId: String = "",
    val visitDate: Long = System.currentTimeMillis(),
    val reason: String = "",
    val observations: String = "",
    val treatment: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
