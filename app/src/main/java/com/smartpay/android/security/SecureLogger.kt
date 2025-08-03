package com.smartpay.android.security

import android.util.Log
import com.smartpay.android.BuildConfig

object SecureLogger {
    
    private const val MAX_LOG_LENGTH = 4000
    private const val REDACTED_PLACEHOLDER = "[REDACTED]"
    
    private val sensitivePatterns = listOf(
        Regex("(token|password|pin|key|secret)[\"'\\s:=]+[\"']?([^\"'\\s,}]+)", RegexOption.IGNORE_CASE),
        Regex("\\d{4,}", RegexOption.IGNORE_CASE), // Card/account numbers
        Regex("(\\+?\\d{1,3}[\\s.-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}", RegexOption.IGNORE_CASE), // Phone numbers
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", RegexOption.IGNORE_CASE) // Email addresses
    )
    
    fun v(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val sanitized = sanitizeMessage(msg)
            if (tr != null) {
                Log.v(tag, sanitized, tr)
            } else {
                Log.v(tag, sanitized)
            }
        }
    }
    
    fun d(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val sanitized = sanitizeMessage(msg)
            if (tr != null) {
                Log.d(tag, sanitized, tr)
            } else {
                Log.d(tag, sanitized)
            }
        }
    }
    
    fun i(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val sanitized = sanitizeMessage(msg)
            if (tr != null) {
                Log.i(tag, sanitized, tr)
            } else {
                Log.i(tag, sanitized)
            }
        }
    }
    
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        val sanitized = sanitizeMessage(msg)
        if (tr != null) {
            Log.w(tag, sanitized, tr)
        } else {
            Log.w(tag, sanitized)
        }
    }
    
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val sanitized = sanitizeMessage(msg)
        if (tr != null) {
            Log.e(tag, sanitized, createSanitizedException(tr))
        } else {
            Log.e(tag, sanitized)
        }
    }
    
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        sensitivePatterns.forEach { pattern ->
            sanitized = pattern.replace(sanitized) { matchResult ->
                val groups = matchResult.groupValues
                if (groups.size > 1) {
                    groups[0].replace(groups.last(), REDACTED_PLACEHOLDER)
                } else {
                    REDACTED_PLACEHOLDER
                }
            }
        }
        
        return if (sanitized.length > MAX_LOG_LENGTH) {
            sanitized.substring(0, MAX_LOG_LENGTH) + "... [TRUNCATED]"
        } else {
            sanitized
        }
    }
    
    private fun createSanitizedException(original: Throwable): Throwable {
        return if (BuildConfig.DEBUG) {
            original
        } else {
            Exception("Exception occurred: ${original::class.java.simpleName}")
        }
    }
    
    fun logSecurityEvent(event: String, details: Map<String, Any> = emptyMap()) {
        val sanitizedDetails = details.mapValues { (_, value) ->
            when (value) {
                is String -> sanitizeMessage(value)
                else -> value.toString()
            }
        }
        
        val message = "SECURITY_EVENT: $event - $sanitizedDetails"
        Log.w("SecurityLogger", message)
    }
    
    fun logNetworkRequest(url: String, method: String, responseCode: Int? = null) {
        if (BuildConfig.DEBUG) {
            val sanitizedUrl = sanitizeMessage(url)
            val message = "NETWORK: $method $sanitizedUrl" + 
                         (responseCode?.let { " -> $it" } ?: "")
            Log.d("NetworkLogger", message)
        }
    }
    
    fun logUserAction(action: String, userId: String? = null) {
        val sanitizedUserId = userId?.let { "User:${it.take(4)}***" } ?: "Anonymous"
        val message = "USER_ACTION: $action by $sanitizedUserId"
        Log.i("UserActionLogger", message)
    }
}