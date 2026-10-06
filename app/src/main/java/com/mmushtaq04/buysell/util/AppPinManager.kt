package com.mmushtaq04.buysell.util

import android.content.Context
import java.security.MessageDigest

object AppPinManager {
    private const val PREFS_NAME = "app_security_prefs"
    private const val KEY_PIN_HASH = "app_pin_hash"

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return storedHash == hashPin(inputPin)
    }

    fun clearPin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }
}
