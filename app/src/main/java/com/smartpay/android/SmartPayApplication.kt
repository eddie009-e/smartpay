package com.smartpay.android

import android.app.Application
import android.util.Log
import com.smartpay.android.crash.CrashHandler
import com.smartpay.android.crash.CrashRetryWorker
import com.smartpay.android.security.*
import com.smartpay.data.network.ApiClient
import kotlin.system.exitProcess

class SmartPayApplication : Application() {
    
    companion object {
        private const val TAG = "SmartPayApplication"
    }
    
    private lateinit var deviceSecurityChecker: DeviceSecurityChecker
    private lateinit var antiTamperingDetector: AntiTamperingDetector
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash handler first to catch any initialization crashes
        initializeCrashHandler()
        
        initializeSecurity()
        initializeComponents()
    }
    
    private fun initializeSecurity() {
        try {
            deviceSecurityChecker = DeviceSecurityChecker(this)
            antiTamperingDetector = AntiTamperingDetector(this)
            
            val securityThreats = deviceSecurityChecker.performSecurityCheck()
            val criticalThreats = securityThreats.filter { 
                it.severity == DeviceSecurityChecker.Severity.CRITICAL 
            }
            
            if (criticalThreats.isNotEmpty()) {
                handleCriticalSecurityThreats(criticalThreats)
                return
            }
            
            if (!antiTamperingDetector.performAntiTamperingChecks()) {
                handleTamperingDetected()
                return
            }
            
            antiTamperingDetector.startContinuousMonitoring()
            
            Log.i(TAG, "Security initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Security initialization failed", e)
            handleSecurityInitializationFailure()
        }
    }
    
    private fun initializeCrashHandler() {
        try {
            // Initialize the global crash handler
            CrashHandler.initialize(this)
            
            // Schedule periodic retry work for pending crashes
            CrashRetryWorker.schedulePeriodicWork(this)
            
            Log.i(TAG, "Crash handler initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize crash handler", e)
        }
    }
    
    private fun initializeComponents() {
        try {
            ApiClient.initialize(this)
            sessionManager = SessionManager.getInstance(this)
            
            Log.i(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Component initialization failed", e)
        }
    }
    
    private fun handleCriticalSecurityThreats(threats: List<DeviceSecurityChecker.SecurityThreat>) {
        val threatMessages = threats.joinToString("\n") { "• ${it.description}" }
        
        Log.w(TAG, "Critical security threats detected:\n$threatMessages")
        
        showSecurityAlert(
            "Security Alert",
            "This app cannot run on compromised devices for your security:\n\n$threatMessages",
            onDismiss = { exitApp() }
        )
    }
    
    private fun handleTamperingDetected() {
        Log.w(TAG, "Application tampering detected")
        
        showSecurityAlert(
            "Security Alert",
            "Application integrity has been compromised. For your security, the app will now close.",
            onDismiss = { exitApp() }
        )
    }
    
    private fun handleSecurityInitializationFailure() {
        Log.e(TAG, "Failed to initialize security components")
        
        showSecurityAlert(
            "Initialization Error",
            "Failed to initialize security components. The app cannot continue.",
            onDismiss = { exitApp() }
        )
    }
    
    private fun showSecurityAlert(title: String, message: String, onDismiss: () -> Unit) {
        Thread {
            Thread.sleep(1000)
            onDismiss()
        }.start()
    }
    
    private fun exitApp() {
        try {
            finishAffinity()
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing activities", e)
        }
        
        exitProcess(0)
    }
    
    private fun finishAffinity() {
        // This would normally finish all activities, but since we're in Application class,
        // we'll rely on exitProcess for now
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "Application terminated")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.i(TAG, "Memory trim requested: level $level")
    }
}