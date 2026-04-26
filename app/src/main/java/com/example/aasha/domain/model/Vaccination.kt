package com.example.aasha.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "vaccinations")
data class Vaccination(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val patientId: String = "",
    val patientName: String = "",
    val vaccineName: String = "",
    val doseNumber: String = "",
    val dateAdministered: Long = System.currentTimeMillis(),
    val remarks: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
