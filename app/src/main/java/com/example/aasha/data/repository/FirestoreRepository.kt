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
            firestore.collection("users").document(aashaId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveUser(user: User) {
        val userRef = firestore.collection("users").document(user.aashaId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (snapshot.exists()) {
                throw Exception("AashaID already taken")
            }
            transaction.set(userRef, user)
        }.await()
    }

    suspend fun getUserByAashaId(aashaId: String): User? {
        return try {
            firestore.collection("users")
                .document(aashaId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
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
