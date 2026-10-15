package com.mmushtaq04.buysell.util

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

object AppPinManager {
    private const val PREFS_NAME = "app_security_prefs"
    private const val KEY_PIN_HASH = "app_pin_hash"
    private const val KEY_PIN_SALT = "app_pin_salt"

    private fun getOrCreateSalt(context: Context): String {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        var salt = prefs?.getString(KEY_PIN_SALT, null)
        if (salt.isNullOrBlank()) {
            salt = UUID.randomUUID().toString()
            val editor = prefs?.edit()
            if (editor != null) {
                editor.putString(KEY_PIN_SALT, salt)
                editor.apply()
            }
        }
        return salt
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((pin + salt).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        return !prefs?.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun savePin(context: Context, pin: String) {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val salt = getOrCreateSalt(context)
        val editor = prefs?.edit()
        if (editor != null) {
            editor.putString(KEY_PIN_HASH, hashPin(pin, salt))
            editor.apply()
        }
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val storedHash = prefs?.getString(KEY_PIN_HASH, null) ?: return false
        val salt = getOrCreateSalt(context)
        return storedHash == hashPin(inputPin, salt)
    }

    fun clearPin(context: Context) {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val editor = prefs?.edit()
        if (editor != null) {
            editor.remove(KEY_PIN_HASH)
            editor.remove(KEY_PIN_SALT)
            editor.apply()
        }
    }
}
