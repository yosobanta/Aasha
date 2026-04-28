package com.example.aasha.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionManager: com.example.aasha.data.local.SessionManager
) {
    suspend fun signUp(email: String, password: String): AuthResult {
        return auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password).await()
    }

    fun verifyAdminPassword(input: String, workerId: String): Boolean {
        return sessionManager.verifyPassword(input, workerId)
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    fun deleteCurrentUser() {
        FirebaseAuth.getInstance().currentUser?.delete()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
