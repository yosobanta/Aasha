package com.example.aasha.data.repository

import com.example.aasha.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun isAashaIdTaken(aashaId: String): Boolean {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("aashaId", aashaId)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveUser(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun getUserByAashaId(aashaId: String): User? {
        val query = firestore.collection("users")
            .whereEqualTo("aashaId", aashaId)
            .get()
            .await()
        return if (!query.isEmpty) {
            query.documents[0].toObject(User::class.java)
        } else {
            null
        }
    }

    suspend fun getUserById(uid: String): User? {
        return try {
            firestore.collection("users")
                .document(uid)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
