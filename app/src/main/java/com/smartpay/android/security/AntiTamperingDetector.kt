package com.smartpay.android.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Debug
import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess

class AntiTamperingDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "AntiTamperingDetector"
        private const val EXPECTED_SIGNATURE_HASH = "YOUR_EXPECTED_SIGNATURE_HASH"
        private val FRIDA_DETECTION_STRINGS = listOf(
            "frida",
            "xposed",
            "substrate",
            "cycript"
        )
    }
    
    fun performAntiTamperingChecks(): Boolean {
        val checks = listOf(
            ::checkAppSignature,
            ::checkDebuggingDetection,
            ::checkHookingFrameworks,
            ::checkEmulatorEnvironment,
            ::checkSuspiciousFiles,
            ::checkMemoryPatching
        )
        
        return checks.all { it() }
    }
    
    private fun checkAppSignature(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isNullOrEmpty()) return false
            
            val signature = signatures[0]
            val signatureHash = getSignatureHash(signature)
            
            signatureHash == EXPECTED_SIGNATURE_HASH
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getSignatureHash(signature: Signature): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(signature.toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    private fun checkDebuggingDetection(): Boolean {
        if (Debug.isDebuggerConnected()) {
            handleTamperingDetected("Debugger detected")
            return false
        }
        
        if (Debug.waitingForDebugger()) {
            handleTamperingDetected("Waiting for debugger")
            return false
        }
        
        return true
    }
    
    private fun checkHookingFrameworks(): Boolean {
        val suspiciousPackages = listOf(
            "de.robv.android.xposed.installer",
            "de.robv.android.xposed",
            "com.saurik.substrate",
            "com.zachspong.fridainjector",
            "re.frida.server"
        )
        
        val packageManager = context.packageManager
        
        for (packageName in suspiciousPackages) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                handleTamperingDetected("Hooking framework detected: $packageName")
                return false
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found, which is good
            }
        }
        
        return checkFridaDetection()
    }
    
    private fun checkFridaDetection(): Boolean {
        val maps = File("/proc/self/maps")
        if (maps.exists()) {
            try {
                val mapsContent = maps.readText()
                for (detectionString in FRIDA_DETECTION_STRINGS) {
                    if (mapsContent.contains(detectionString, ignoreCase = true)) {
                        handleTamperingDetected("Frida-related content detected in memory maps")
                        return false
                    }
                }
            } catch (e: Exception) {
                // Unable to read maps file
            }
        }
        
        return true
    }
    
    private fun checkEmulatorEnvironment(): Boolean {
        val emulatorProperties = listOf(
            "ro.kernel.qemu",
            "ro.hardware",
            "ro.product.device"
        )
        
        for (property in emulatorProperties) {
            val value = getSystemProperty(property)
            if (value.contains("goldfish", ignoreCase = true) || 
                value.contains("emulator", ignoreCase = true)) {
                handleTamperingDetected("Emulator environment detected")
                return false
            }
        }
        
        return true
    }
    
    private fun checkSuspiciousFiles(): Boolean {
        val suspiciousFiles = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        
        for (filePath in suspiciousFiles) {
            if (File(filePath).exists()) {
                handleTamperingDetected("Suspicious file found: $filePath")
                return false
            }
        }
        
        return true
    }
    
    private fun checkMemoryPatching(): Boolean {
        val memorySegments = File("/proc/self/maps")
        if (memorySegments.exists()) {
            try {
                val content = memorySegments.readText()
                if (content.contains("rw-p") && content.contains("deleted")) {
                    handleTamperingDetected("Memory patching detected")
                    return false
                }
            } catch (e: Exception) {
                // Unable to check memory segments
            }
        }
        
        return true
    }
    
    private fun getSystemProperty(property: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $property")
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun handleTamperingDetected(reason: String) {
        android.util.Log.w(TAG, "Tampering detected: $reason")
        
        Thread {
            Thread.sleep(1000)
            exitProcess(0)
        }.start()
    }
    
    fun startContinuousMonitoring() {
        Thread {
            while (true) {
                if (!performAntiTamperingChecks()) {
                    break
                }
                Thread.sleep(5000) // Check every 5 seconds
            }
        }.start()
    }
}