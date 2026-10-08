package com.mmushtaq04.buysell.util

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

object AppPinManager {
    private const val PREFS_NAME = "app_security_prefs"
    private const val KEY_PIN_HASH = "app_pin_hash"
    private const val KEY_PIN_SALT = "app_pin_salt"

    private fun getOrCreateSalt(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var salt = prefs.getString(KEY_PIN_SALT, null)
        if (salt.isNullOrBlank()) {
            salt = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((pin + salt).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = getOrCreateSalt(context)
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin, salt)).apply()
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = getOrCreateSalt(context)
        return storedHash == hashPin(inputPin, salt)
    }

    fun clearPin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()
    }
}
