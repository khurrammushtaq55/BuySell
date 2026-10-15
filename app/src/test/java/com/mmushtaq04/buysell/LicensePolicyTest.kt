package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.domain.LicenseData
import com.mmushtaq04.buysell.domain.LicensePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensePolicyTest {

    private val baseNowMs = 1_700_000_000_000L

    @Test
    fun testActiveLicenseGrantsEntitlement() {
        val license = LicenseData(
            plan = "PRO_ANNUAL",
            status = "ACTIVE",
            validUntilMs = baseNowMs + 86400000L * 30,
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertTrue(result.isUnlocked)
        assertEquals("ACTIVE", result.status)
        assertEquals("PLAY", result.source)
    }

    @Test
    fun testExpiredLicenseDeniesEntitlement() {
        val license = LicenseData(
            plan = "PRO_ANNUAL",
            status = "ACTIVE",
            validUntilMs = baseNowMs - 1000L,
            source = "PLAY",
            serverTimeMs = baseNowMs - 86400000L,
            lastFetchLocalMs = baseNowMs - 86400000L
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertFalse(result.isUnlocked)
        assertEquals("License expired", result.reason)
    }

    @Test
    fun testMissingOrZeroValidUntilDeniesEntitlement() {
        val license = LicenseData(
            plan = "PRO_ANNUAL",
            status = "ACTIVE",
            validUntilMs = 0L,
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertFalse(result.isUnlocked)
        assertEquals("Missing or zero validUntil", result.reason)
    }

    @Test
    fun testGracePeriodLicenseGrantsEntitlementWithinGraceWindow() {
        // Expiry was 3 days ago, but within 7-day grace window
        val license = LicenseData(
            plan = "PRO_MONTHLY",
            status = "GRACE",
            validUntilMs = baseNowMs - 3L * 86400000L,
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertTrue(result.isUnlocked)
        assertEquals("GRACE", result.status)
    }

    @Test
    fun testOnHoldLicenseDeniesEntitlementAfterGraceWindow() {
        // Expiry was 10 days ago (past 7-day grace window)
        val license = LicenseData(
            plan = "PRO_MONTHLY",
            status = "ON_HOLD",
            validUntilMs = baseNowMs - 10L * 86400000L,
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertFalse(result.isUnlocked)
    }

    @Test
    fun testCanceledActiveLicenseGrantsEntitlementUntilExpiry() {
        val license = LicenseData(
            plan = "PRO_MONTHLY",
            status = "CANCELED_ACTIVE",
            validUntilMs = baseNowMs + 5L * 86400000L,
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertTrue(result.isUnlocked)
        assertEquals("CANCELED_ACTIVE", result.status)
    }

    @Test
    fun testOfflineTrustWindowExceededDeniesEntitlement() {
        // Last fetch was 15 days ago (exceeding 14-day limit)
        val license = LicenseData(
            plan = "PRO_ANNUAL",
            status = "ACTIVE",
            validUntilMs = baseNowMs + 100L * 86400000L,
            source = "PLAY",
            serverTimeMs = baseNowMs - 15L * 86400000L,
            lastFetchLocalMs = baseNowMs - 15L * 86400000L
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertFalse(result.isUnlocked)
        assertEquals("Offline trust window exceeded", result.reason)
    }

    @Test
    fun testClockRollbackProtectedWithLastSeenTimeMs() {
        val lastSeenMs = baseNowMs + 10L * 86400000L // 10 days in future
        val rolledBackNowMs = baseNowMs // Device clock changed backward

        val license = LicenseData(
            plan = "PRO_ANNUAL",
            status = "ACTIVE",
            validUntilMs = baseNowMs + 5L * 86400000L, // Valid for 5 days from baseNow, but rolledBackNow is past validity
            source = "PLAY",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        // Effective now will be lastSeenMs (10 days in future), so license (valid for 5 days) is expired!
        val result = LicensePolicy.computeEntitlement(license, nowMs = rolledBackNowMs, lastSeenTimeMs = lastSeenMs)

        assertFalse(result.isUnlocked)
        assertEquals("License expired", result.reason)
    }

    @Test
    fun testPromoCodeGrantStatus() {
        val license = LicenseData(
            plan = "SPRING_PROMO",
            status = "ACTIVE",
            validUntilMs = baseNowMs + 30L * 86400000L,
            source = "PROMO",
            serverTimeMs = baseNowMs,
            lastFetchLocalMs = baseNowMs
        )
        val result = LicensePolicy.computeEntitlement(license, nowMs = baseNowMs, lastSeenTimeMs = baseNowMs)

        assertTrue(result.isUnlocked)
        assertEquals("PROMO", result.source)
    }
}
