package com.smartpay.android.crash

import android.content.Context
import android.os.Build
import com.smartpay.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Global crash handler that intercepts all unhandled exceptions
 * and sends them to the backend AI Security Agent for analysis
 */
class CrashHandler private constructor(
    private val context: Context,
    private val errorReporter: ErrorReporter
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val crashScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Collect crash data
            val crashData = collectCrashData(throwable)
            
            // Try to send crash report
            crashScope.launch {
                try {
                    errorReporter.sendCrashReport(crashData)
                } catch (e: Exception) {
                    // Save for retry if sending fails
                    errorReporter.savePendingCrash(crashData)
                }
            }
            
            // Give some time for the crash report to be sent
            Thread.sleep(2000)
            
        } catch (e: Exception) {
            // Ignore any errors in crash reporting
        } finally {
            // Call the default handler to show crash dialog or terminate
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun collectCrashData(throwable: Throwable): CrashData {
        return CrashData(
            source = "android",
            error = "${throwable.javaClass.name}: ${throwable.message ?: "No message"}",
            stackTrace = getStackTraceString(throwable),
            device = getDeviceInfo(),
            sdk = Build.VERSION.SDK_INT,
            appVersion = BuildConfig.VERSION_NAME,
            timestamp = getISOTimestamp()
        )
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun getISOTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }

    companion object {
        @Volatile
        private var INSTANCE: CrashHandler? = null

        /**
         * Initialize the crash handler. Should be called once in Application.onCreate()
         */
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        val errorReporter = ErrorReporter(context)
                        INSTANCE = CrashHandler(context.applicationContext, errorReporter)
                        
                        // Also retry any pending crashes
                        errorReporter.retryPendingCrashes()
                    }
                }
            }
        }

        /**
         * Get the singleton instance
         */
        fun getInstance(): CrashHandler? = INSTANCE
    }
}

/**
 * Data class representing crash information
 */
data class CrashData(
    val source: String,
    val error: String,
    val stackTrace: String,
    val device: String,
    val sdk: Int,
    val appVersion: String,
    val timestamp: String
)