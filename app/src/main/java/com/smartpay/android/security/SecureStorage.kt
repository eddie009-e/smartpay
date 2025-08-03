package com.smartpay.android.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SecureStorage? = null
        
        fun getInstance(context: Context): SecureStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val PREFS_NAME = "smartpay_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_PIN = "user_pin"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_SESSION_TIMEOUT = "session_timeout"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveAuthToken(token: String) {
        encryptedPrefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        updateLastActivity()
    }
    
    fun getAuthToken(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun saveUserPin(pin: String) {
        val hashedPin = SecurityUtils.hashPin(pin)
        encryptedPrefs.edit().putString(KEY_USER_PIN, hashedPin).apply()
    }
    
    fun verifyUserPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_USER_PIN, null) ?: return false
        return SecurityUtils.verifyPin(pin, storedHash)
    }
    
    fun updateLastActivity() {
        encryptedPrefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }
    
    fun getLastActivity(): Long {
        return encryptedPrefs.getLong(KEY_LAST_ACTIVITY, 0)
    }
    
    fun setSessionTimeout(timeoutMs: Long) {
        encryptedPrefs.edit().putLong(KEY_SESSION_TIMEOUT, timeoutMs).apply()
    }
    
    fun getSessionTimeout(): Long {
        return encryptedPrefs.getLong(KEY_SESSION_TIMEOUT, 5 * 60 * 1000L) // Default 5 minutes
    }
    
    fun isSessionExpired(): Boolean {
        val lastActivity = getLastActivity()
        val timeout = getSessionTimeout()
        return System.currentTimeMillis() - lastActivity > timeout
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    fun clearAllData() {
        encryptedPrefs.edit().clear().apply()
    }
    
    fun clearSession() {
        encryptedPrefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_LAST_ACTIVITY)
            .apply()
    }
}