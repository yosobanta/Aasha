package com.example.aasha.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val village: String = "",

    val phone: String? = null,
    val guardianName: String? = null,
    val isPregnant: Boolean? = null,

    val workerId: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),

    val isDeleted: Boolean = false,

    val syncStatus: SyncStatus = SyncStatus.PENDING
)