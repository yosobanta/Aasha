package com.example.aasha.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "secure_mpin_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "EncryptedSharedPreferences creation failed", e)
            context.getSharedPreferences("secure_mpin_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private val WORKER_ID = stringPreferencesKey("worker_id")
        private val LAST_WORKER_ID = stringPreferencesKey("last_worker_id")
        private val NAME = stringPreferencesKey("name")
        private val EMAIL = stringPreferencesKey("email")
        private val LOCALITY = stringPreferencesKey("locality")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LANGUAGE = stringPreferencesKey("language")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val NOTIFICATION_TIME = stringPreferencesKey("notification_time")

        private const val MPIN_HASH_PREFIX = "mpin_hash_"
        private const val PASSWORD_HASH_PREFIX = "password_hash_"
    }

    val workerId: Flow<String?> = dataStore.data.map { it[WORKER_ID] }
    val lastWorkerId: Flow<String?> = dataStore.data.map { it[LAST_WORKER_ID] }
    val name: Flow<String?> = dataStore.data.map { it[NAME] }
    val email: Flow<String?> = dataStore.data.map { it[EMAIL] }
    val locality: Flow<String?> = dataStore.data.map { it[LOCALITY] }
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    val lastSyncTime: Flow<Long> = dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }
    val notificationTime: Flow<String> = dataStore.data.map { it[NOTIFICATION_TIME] ?: "09:00" }

    fun hasMpin(workerId: String): Boolean {
        return encryptedPrefs.getString(MPIN_HASH_PREFIX + workerId, null) != null
    }

    fun verifyMpin(pin: String, workerId: String): Boolean {
        val savedHash = encryptedPrefs.getString(MPIN_HASH_PREFIX + workerId, null)
        return if (savedHash != null) {
            hashString(pin) == savedHash
        } else {
            false
        }
    }

    fun verifyPassword(password: String, workerId: String): Boolean {
        val savedHash = encryptedPrefs.getString(PASSWORD_HASH_PREFIX + workerId, null)
        return if (savedHash != null) {
            hashString(password) == savedHash
        } else {
            false
        }
    }

    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun saveSession(workerId: String, name: String, email: String, locality: String) {
        dataStore.edit { preferences ->
            preferences[WORKER_ID] = workerId
            preferences[LAST_WORKER_ID] = workerId
            preferences[NAME] = name
            preferences[EMAIL] = email
            preferences[LOCALITY] = locality
            preferences[IS_LOGGED_IN] = true
        }
    }

    suspend fun saveLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    suspend fun updateLastSyncTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = time
        }
    }

    suspend fun saveNotificationTime(time: String) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIME] = time
        }
    }

    fun saveMpin(mpin: String, workerId: String) {
        val hashedMpin = hashString(mpin)
        encryptedPrefs.edit().putString(MPIN_HASH_PREFIX + workerId, hashedMpin).apply()
    }

    fun savePasswordHash(password: String, workerId: String) {
        val hash = hashString(password)
        encryptedPrefs.edit().putString(PASSWORD_HASH_PREFIX + workerId, hash).apply()
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = loggedIn
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
        // Note: We keep WORKER_ID, NAME, EMAIL, LOCALITY, LAST_WORKER_ID 
        // and encrypted MPINs/Passwords for offline login support
    }
    }
