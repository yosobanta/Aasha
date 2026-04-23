package com.example.aasha.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val aashaId: String = "",
    val email: String = "",
    val locality: String = "",
    val createdAt: Timestamp? = null
)
