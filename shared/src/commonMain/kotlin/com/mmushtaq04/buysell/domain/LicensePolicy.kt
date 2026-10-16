package com.mmushtaq04.buysell.domain

data class LicenseData(
    val plan: String = "",
    val status: String = "EXPIRED",
    val validUntilMs: Long = 0L,
    val source: String = "NONE",
    val serverTimeMs: Long = 0L,
    val lastFetchLocalMs: Long = 0L
)

data class Entitlement(
    val isUnlocked: Boolean = false,
    val status: String = "EXPIRED",
    val validUntilMs: Long = 0L,
    val source: String = "NONE",
    val reason: String = "Initial state"
)

object LicensePolicy {
    const val GRACE_WINDOW_MS = 7L * 24 * 60 * 60 * 1000L // 7 days
    const val OFFLINE_TRUST_WINDOW_MS = 14L * 24 * 60 * 60 * 1000L // 14 days
    const val CLOCK_ROLLBACK_TOLERANCE_MS = 60L * 1000L // 1 minute

    fun computeEntitlement(
        license: LicenseData,
        nowMs: Long,
        lastSeenTimeMs: Long,
        elapsedSinceFetchMs: Long = 0L
    ): Entitlement {
        val isClockRolledBack = nowMs < (lastSeenTimeMs - CLOCK_ROLLBACK_TOLERANCE_MS)
        val effectiveNowMs = if (isClockRolledBack) lastSeenTimeMs else maxOf(nowMs, lastSeenTimeMs)

        if (license.validUntilMs <= 0L) {
            return Entitlement(
                isUnlocked = false,
                status = license.status,
                validUntilMs = 0L,
                source = license.source,
                reason = "Missing or zero validUntil"
            )
        }

        val offlineDurationMs = if (elapsedSinceFetchMs > 0L) {
            elapsedSinceFetchMs
        } else if (license.lastFetchLocalMs > 0L) {
            (effectiveNowMs - license.lastFetchLocalMs).coerceAtLeast(0L)
        } else {
            0L
        }

        if (offlineDurationMs > OFFLINE_TRUST_WINDOW_MS) {
            return Entitlement(
                isUnlocked = false,
                status = license.status,
                validUntilMs = license.validUntilMs,
                source = license.source,
                reason = "Offline trust window exceeded"
            )
        }

        val normalizedStatus = license.status.uppercase().trim()
        val isAllowedStatus = normalizedStatus in setOf("ACTIVE", "GRACE", "ON_HOLD", "CANCELED_ACTIVE")

        if (!isAllowedStatus) {
            return Entitlement(
                isUnlocked = false,
                status = normalizedStatus,
                validUntilMs = license.validUntilMs,
                source = license.source,
                reason = "License status '$normalizedStatus' does not grant entitlement"
            )
        }

        val effectiveValidUntilMs = if (normalizedStatus == "GRACE" || normalizedStatus == "ON_HOLD") {
            license.validUntilMs + GRACE_WINDOW_MS
        } else {
            license.validUntilMs
        }

        val isNotExpired = effectiveNowMs <= effectiveValidUntilMs

        return if (isNotExpired) {
            Entitlement(
                isUnlocked = true,
                status = normalizedStatus,
                validUntilMs = license.validUntilMs,
                source = license.source,
                reason = if (isClockRolledBack) "Valid license (clock rollback detected)" else "Valid license"
            )
        } else {
            Entitlement(
                isUnlocked = false,
                status = normalizedStatus,
                validUntilMs = license.validUntilMs,
                source = license.source,
                reason = "License expired"
            )
        }
    }
}
