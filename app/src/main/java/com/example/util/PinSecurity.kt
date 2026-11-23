package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurity {
    private val random = SecureRandom()

    fun generateSaltBase64(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    fun hashPin(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashed)
    }

    fun verifyPin(enteredPin: String, expectedHashHex: String, saltHex: String): Boolean {
        if (expectedHashHex.isEmpty() || saltHex.isEmpty()) return false
        val actual = hexToBytes(hashPin(enteredPin, saltHex))
        val expected = hexToBytes(expectedHashHex)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        if (clean.length % 2 != 0) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            val hi = clean[i * 2].digitToIntOrNull(16) ?: 0
            val lo = clean[i * 2 + 1].digitToIntOrNull(16) ?: 0
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    fun isBiometricHardwareSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                (pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                    pm.hasSystemFeature(PackageManager.FEATURE_IRIS)))
    }
}
