package com.smartpay.android.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object SecurityUtils {
    
    private const val HASH_ALGORITHM = "SHA-256"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SALT_LENGTH = 32
    
    fun hashPin(pin: String): String {
        val salt = generateSalt()
        val hashedPin = hashWithSalt(pin, salt)
        return "${salt.toHex()}:${hashedPin.toHex()}"
    }
    
    fun verifyPin(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        
        val salt = parts[0].fromHex()
        val hash = parts[1].fromHex()
        val computedHash = hashWithSalt(pin, salt)
        
        return hash.contentEquals(computedHash)
    }
    
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    private fun hashWithSalt(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }
    
    fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    fun isValidPin(pin: String): Boolean {
        return pin.length in 4..8 && pin.all { it.isDigit() }
    }
    
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    
    private fun String.fromHex(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    
    fun calculateHMAC(data: String, key: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(key.toByteArray(), HMAC_ALGORITHM)
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray()).toHex()
    }
    
    fun sanitizeLog(message: String): String {
        return message
            .replace(Regex("token[\"':]\\s*[\"']([^\"'\\s]+)[\"']"), "token:\"[REDACTED]\"")
            .replace(Regex("password[\"':]\\s*[\"']([^\"'\\s]+)[\"']"), "password:\"[REDACTED]\"")
            .replace(Regex("pin[\"':]\\s*[\"']([^\"'\\s]+)[\"']"), "pin:\"[REDACTED]\"")
            .replace(Regex("\\d{4,}"), "[REDACTED]")
    }
}