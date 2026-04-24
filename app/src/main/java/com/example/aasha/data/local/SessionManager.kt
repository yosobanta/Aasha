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
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_mpin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val WORKER_ID = stringPreferencesKey("worker_id")
        private val NAME = stringPreferencesKey("name")
        private val EMAIL = stringPreferencesKey("email")
        private val LOCALITY = stringPreferencesKey("locality")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LANGUAGE = stringPreferencesKey("language")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private const val MPIN_HASH_KEY = "user_mpin_hash"
    }

    val workerId: Flow<String?> = dataStore.data.map { it[WORKER_ID] }
    val name: Flow<String?> = dataStore.data.map { it[NAME] }
    val email: Flow<String?> = dataStore.data.map { it[EMAIL] }
    val locality: Flow<String?> = dataStore.data.map { it[LOCALITY] }
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    val lastSyncTime: Flow<Long> = dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }

    fun hasMpin(): Boolean {
        return encryptedPrefs.getString(MPIN_HASH_KEY, null) != null
    }

    fun verifyMpin(pin: String): Boolean {
        val savedHash = encryptedPrefs.getString(MPIN_HASH_KEY, null)
        return if (savedHash != null) {
            hashMpin(pin) == savedHash
        } else {
            false
        }
    }

    private fun hashMpin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun saveSession(workerId: String, name: String, email: String, locality: String) {
        dataStore.edit { preferences ->
            preferences[WORKER_ID] = workerId
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

    fun saveMpin(mpin: String) {
        val hashedMpin = hashMpin(mpin)
        encryptedPrefs.edit().putString(MPIN_HASH_KEY, hashedMpin).apply()
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
        encryptedPrefs.edit().clear().apply()
    }
}
