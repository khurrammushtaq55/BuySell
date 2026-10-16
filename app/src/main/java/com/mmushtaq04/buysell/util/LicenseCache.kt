package com.mmushtaq04.buysell.util

import android.content.Context
import android.os.SystemClock
import com.mmushtaq04.buysell.domain.LicenseData

class LicenseCache(context: Context) {
    private val prefs = context.getSharedPreferences("buysell_license_cache", Context.MODE_PRIVATE)
    private val oldAppPrefs = context.getSharedPreferences("buysell_app_prefs", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_LOCKED_FEATURES = setOf(
            "dashboard_advanced",
            "inventory_insights",
            "reports_export_pro",
            "monthly_summary_reports",
            "sleeping_partner_invite",
            "extra_staff_seats"
        )
    }

    init {
        // One-time cleanup of legacy is_premium_unlocked key
        if (oldAppPrefs.contains("is_premium_unlocked")) {
            val editor = oldAppPrefs.edit()
            editor.remove("is_premium_unlocked")
            editor.apply()
        }
    }

    var cachedShopId: String
        get() = prefs.getString("cached_shop_id", "") ?: ""
        set(value) {
            prefs.edit().putString("cached_shop_id", value).apply()
        }

    var lastSeenTimeMs: Long
        get() = prefs.getLong("last_seen_time_ms", 0L)
        set(value) {
            val editor = prefs.edit()
            val maxVal = maxOf(value, lastSeenTimeMs)
            editor.putLong("last_seen_time_ms", maxVal)
            editor.apply()
        }

    var lastFetchElapsedRealtimeMs: Long
        get() = prefs.getLong("last_fetch_elapsed_realtime_ms", 0L)
        set(value) {
            prefs.edit().putLong("last_fetch_elapsed_realtime_ms", value).apply()
        }

    var lastConfigFetchMs: Long
        get() = prefs.getLong("last_config_fetch_ms", 0L)
        set(value) {
            val editor = prefs.edit()
            editor.putLong("last_config_fetch_ms", value)
            editor.apply()
        }

    var enforcementEnabled: Boolean
        get() = prefs.getBoolean("enforcement_enabled", true)
        set(value) {
            val editor = prefs.edit()
            editor.putBoolean("enforcement_enabled", value)
            editor.apply()
        }

    fun updateLastSeenTime(currentNowMs: Long = System.currentTimeMillis()) {
        lastSeenTimeMs = maxOf(currentNowMs, lastSeenTimeMs)
    }

    fun getElapsedSinceFetchMs(): Long {
        val startElapsed = lastFetchElapsedRealtimeMs
        if (startElapsed <= 0L) return 0L
        val currentElapsed = SystemClock.elapsedRealtime()
        return (currentElapsed - startElapsed).coerceAtLeast(0L)
    }

    fun read(currentShopId: String = ""): LicenseData {
        val storedShopId = cachedShopId
        if (currentShopId.isNotBlank() && storedShopId.isNotBlank() && storedShopId != currentShopId) {
            return LicenseData(status = "EXPIRED", validUntilMs = 0L)
        }
        return LicenseData(
            plan = prefs.getString("plan", "") ?: "",
            status = prefs.getString("status", "EXPIRED") ?: "EXPIRED",
            validUntilMs = prefs.getLong("valid_until_ms", 0L),
            source = prefs.getString("source", "NONE") ?: "NONE",
            serverTimeMs = prefs.getLong("server_time_ms", 0L),
            lastFetchLocalMs = prefs.getLong("last_fetch_local_ms", 0L)
        )
    }

    fun save(license: LicenseData, shopId: String = "") {
        val editor = prefs.edit()
        if (shopId.isNotBlank()) {
            editor.putString("cached_shop_id", shopId)
        }
        editor.putString("plan", license.plan)
        editor.putString("status", license.status)
        editor.putLong("valid_until_ms", license.validUntilMs)
        editor.putString("source", license.source)
        editor.putLong("server_time_ms", license.serverTimeMs)
        editor.putLong("last_fetch_local_ms", license.lastFetchLocalMs)
        editor.putLong("last_fetch_elapsed_realtime_ms", SystemClock.elapsedRealtime())
        editor.apply()

        val candidateTime = maxOf(license.serverTimeMs, license.lastFetchLocalMs, System.currentTimeMillis())
        lastSeenTimeMs = candidateTime
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    fun readLockedFeatures(): Set<String> {
        return prefs.getStringSet("locked_features_set", DEFAULT_LOCKED_FEATURES) ?: DEFAULT_LOCKED_FEATURES
    }

    fun saveLockedFeatures(set: Set<String>, enforcementEnabled: Boolean) {
        val editor = prefs.edit()
        editor.putStringSet("locked_features_set", set)
        editor.putBoolean("enforcement_enabled", enforcementEnabled)
        editor.putLong("last_config_fetch_ms", System.currentTimeMillis())
        editor.apply()
    }
}
