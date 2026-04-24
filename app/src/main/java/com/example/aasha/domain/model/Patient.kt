package com.example.aasha.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String = "",

    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val village: String = "",

    val phone: String? = null,
    val guardianName: String? = null,
    val isPregnant: Boolean? = null,

    val workerId: String = "",

    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L,

    val isDeleted: Boolean = false,

    val syncStatus: SyncStatus = SyncStatus.PENDING
)