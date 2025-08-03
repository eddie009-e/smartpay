package com.smartpay.android.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import com.scottyab.rootbeer.RootBeer
import java.io.File

class DeviceSecurityChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceSecurityChecker"
    }
    
    private val rootBeer = RootBeer(context)
    
    data class SecurityThreat(
        val type: ThreatType,
        val description: String,
        val severity: Severity
    )
    
    enum class ThreatType {
        ROOT_DETECTED,
        EMULATOR_DETECTED,
        DEVELOPER_OPTIONS_ENABLED,
        DEBUGGER_ATTACHED,
        HOOK_DETECTED,
        TAMPERED_APP
    }
    
    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    fun performSecurityCheck(): List<SecurityThreat> {
        val threats = mutableListOf<SecurityThreat>()
        
        if (isRooted()) {
            threats.add(SecurityThreat(
                ThreatType.ROOT_DETECTED,
                "Device appears to be rooted",
                Severity.CRITICAL
            ))
        }
        
        if (isEmulator()) {
            threats.add(SecurityThreat(
                ThreatType.EMULATOR_DETECTED,
                "App is running on an emulator",
                Severity.HIGH
            ))
        }
        
        if (isDeveloperOptionsEnabled()) {
            threats.add(SecurityThreat(
                ThreatType.DEVELOPER_OPTIONS_ENABLED,
                "Developer options are enabled",
                Severity.MEDIUM
            ))
        }
        
        if (isDebuggerAttached()) {
            threats.add(SecurityThreat(
                ThreatType.DEBUGGER_ATTACHED,
                "Debugger is attached to the app",
                Severity.CRITICAL
            ))
        }
        
        if (isAppTampered()) {
            threats.add(SecurityThreat(
                ThreatType.TAMPERED_APP,
                "App appears to be tampered with",
                Severity.CRITICAL
            ))
        }
        
        return threats
    }
    
    private fun isRooted(): Boolean {
        return rootBeer.isRooted || checkManualRootSigns()
    }
    
    private fun checkManualRootSigns(): Boolean {
        val rootFiles = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        return rootFiles.any { File(it).exists() }
    }
    
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("vbox86"))
    }
    
    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() || 
               android.os.Debug.waitingForDebugger()
    }
    
    private fun isAppTampered(): Boolean {
        return try {
            val appInfo = context.applicationInfo
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && 
            !BuildConfig.DEBUG
        } catch (e: Exception) {
            true
        }
    }
    
    fun isSafeToRun(): Boolean {
        val threats = performSecurityCheck()
        return threats.none { it.severity == Severity.CRITICAL }
    }
    
    fun getCriticalThreats(): List<SecurityThreat> {
        return performSecurityCheck().filter { it.severity == Severity.CRITICAL }
    }
}