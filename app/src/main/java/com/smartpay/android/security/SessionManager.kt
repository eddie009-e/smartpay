package com.smartpay.android.security

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager private constructor(private val context: Context) : Application.ActivityLifecycleCallbacks {
    
    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private const val WARNING_TIMEOUT_MS = 4 * 60 * 1000L // 4 minutes (1 minute warning)
    }
    
    private val secureStorage = SecureStorage.getInstance(context)
    private val handler = Handler(Looper.getMainLooper())
    private var sessionTimeoutRunnable: Runnable? = null
    private var warningTimeoutRunnable: Runnable? = null
    private var isAppInForeground = false
    private var activeActivities = 0
    
    private val _sessionState = MutableStateFlow(SessionState.ACTIVE)
    val sessionState: StateFlow<SessionState> = _sessionState
    
    enum class SessionState {
        ACTIVE,
        WARNING,
        EXPIRED,
        LOCKED
    }
    
    init {
        (context as? Application)?.registerActivityLifecycleCallbacks(this)
    }
    
    fun startSession() {
        _sessionState.value = SessionState.ACTIVE
        secureStorage.updateLastActivity()
        resetSessionTimeout()
    }
    
    fun extendSession() {
        if (_sessionState.value == SessionState.ACTIVE) {
            secureStorage.updateLastActivity()
            resetSessionTimeout()
        }
    }
    
    fun endSession() {
        _sessionState.value = SessionState.EXPIRED
        clearSessionTimeouts()
        secureStorage.clearSession()
    }
    
    fun lockSession() {
        _sessionState.value = SessionState.LOCKED
        clearSessionTimeouts()
    }
    
    private fun resetSessionTimeout() {
        clearSessionTimeouts()
        
        if (isAppInForeground) {
            warningTimeoutRunnable = Runnable {
                _sessionState.value = SessionState.WARNING
                showSessionWarning()
            }
            handler.postDelayed(warningTimeoutRunnable!!, WARNING_TIMEOUT_MS)
            
            sessionTimeoutRunnable = Runnable {
                expireSession()
            }
            handler.postDelayed(sessionTimeoutRunnable!!, SESSION_TIMEOUT_MS)
        }
    }
    
    private fun clearSessionTimeouts() {
        sessionTimeoutRunnable?.let { handler.removeCallbacks(it) }
        warningTimeoutRunnable?.let { handler.removeCallbacks(it) }
        sessionTimeoutRunnable = null
        warningTimeoutRunnable = null
    }
    
    private fun expireSession() {
        _sessionState.value = SessionState.EXPIRED
        secureStorage.clearSession()
        redirectToLogin()
    }
    
    private fun showSessionWarning() {
        // Implementation would show a dialog or notification warning about session expiry
        // For now, we'll just log it
        android.util.Log.i("SessionManager", "Session will expire in 1 minute")
    }
    
    private fun redirectToLogin() {
        try {
            val intent = Intent().apply {
                setClassName(context, "com.smartpay.LoginActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to redirect to login", e)
        }
    }
    
    fun isSessionValid(): Boolean {
        return when (_sessionState.value) {
            SessionState.ACTIVE, SessionState.WARNING -> {
                !secureStorage.isSessionExpired()
            }
            else -> false
        }
    }
    
    fun getRemainingSessionTime(): Long {
        val lastActivity = secureStorage.getLastActivity()
        val timeout = secureStorage.getSessionTimeout()
        val elapsed = System.currentTimeMillis() - lastActivity
        return maxOf(0, timeout - elapsed)
    }
    
    // ActivityLifecycleCallbacks implementation
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    
    override fun onActivityStarted(activity: Activity) {
        activeActivities++
        if (!isAppInForeground) {
            isAppInForeground = true
            onAppForeground()
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        if (isSessionValid()) {
            extendSession()
        }
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {
        activeActivities--
        if (activeActivities <= 0) {
            isAppInForeground = false
            onAppBackground()
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {}
    
    private fun onAppForeground() {
        if (secureStorage.isSessionExpired()) {
            expireSession()
        } else if (_sessionState.value == SessionState.ACTIVE) {
            resetSessionTimeout()
        }
    }
    
    private fun onAppBackground() {
        clearSessionTimeouts()
        secureStorage.updateLastActivity()
    }
}