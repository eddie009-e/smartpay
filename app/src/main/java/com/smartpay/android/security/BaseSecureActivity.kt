package com.smartpay.android.security

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

abstract class BaseSecureActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var deviceSecurityChecker: DeviceSecurityChecker
    private lateinit var secureStorage: SecureStorage
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializeSecurity()
        applySecurityMeasures()
        checkDeviceSecurity()
    }
    
    private fun initializeSecurity() {
        sessionManager = SessionManager.getInstance(this)
        deviceSecurityChecker = DeviceSecurityChecker(this)
        secureStorage = SecureStorage.getInstance(this)
    }
    
    private fun applySecurityMeasures() {
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
    }
    
    private fun checkDeviceSecurity() {
        if (!deviceSecurityChecker.isSafeToRun()) {
            val threats = deviceSecurityChecker.getCriticalThreats()
            handleSecurityThreat(threats.firstOrNull()?.description ?: "Device compromised")
            return
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        checkSessionValidity()
        sessionManager.extendSession()
        
        lifecycleScope.launch {
            sessionManager.sessionState.collect { state ->
                when (state) {
                    SessionManager.SessionState.EXPIRED -> handleSessionExpired()
                    SessionManager.SessionState.LOCKED -> handleSessionLocked()
                    else -> {} // Active or Warning states are handled by SessionManager
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        secureStorage.updateLastActivity()
    }
    
    private fun checkSessionValidity() {
        if (!sessionManager.isSessionValid()) {
            redirectToLogin()
        }
    }
    
    private fun handleSessionExpired() {
        redirectToLogin()
    }
    
    private fun handleSessionLocked() {
        redirectToPin()
    }
    
    private fun handleSecurityThreat(description: String) {
        android.util.Log.w("BaseSecureActivity", "Security threat: $description")
        finishAffinity()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, getLoginActivityClass()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    private fun redirectToPin() {
        val intent = Intent(this, getPinActivityClass()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    protected abstract fun getLoginActivityClass(): Class<out Activity>
    protected abstract fun getPinActivityClass(): Class<out Activity>
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionManager.extendSession()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        if (hasFocus) {
            checkDeviceSecurity()
        }
    }
    
    protected fun isSessionValid(): Boolean {
        return sessionManager.isSessionValid()
    }
    
    protected fun getRemainingSessionTime(): Long {
        return sessionManager.getRemainingSessionTime()
    }
    
    protected fun logout() {
        sessionManager.endSession()
        redirectToLogin()
    }
}