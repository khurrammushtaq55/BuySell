package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.data.repository.PremiumFeature
import com.mmushtaq04.buysell.domain.RemoteConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigParserTest {

    private val defaultLockedSet = setOf(
        PremiumFeature.DASHBOARD_ADVANCED,
        PremiumFeature.INVENTORY_INSIGHTS,
        PremiumFeature.REPORTS_EXPORT_PRO,
        PremiumFeature.MONTHLY_SUMMARY_REPORTS,
        PremiumFeature.SLEEPING_PARTNER_INVITE,
        PremiumFeature.EXTRA_STAFF_SEATS
    )

    @Test
    fun testNullOrMissingListKeepsLastKnownGoodCache() {
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = null,
            rawEnforcementRaw = true,
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertTrue(result.enforcementEnabled)
        assertEquals(defaultLockedSet, result.lockedFeatures)
    }

    @Test
    fun testEmptyListUnlocksEverything() {
        val rawList = emptyList<Any>()
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = rawList,
            rawEnforcementRaw = true,
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertTrue(result.enforcementEnabled)
        assertTrue(result.lockedFeatures.isEmpty())
    }

    @Test
    fun testKillSwitchFalseUnlocksEverything() {
        val rawList = listOf("dashboard_advanced", "reports_export_pro")
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = rawList,
            rawEnforcementRaw = false, // Kill switch set to false
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertFalse(result.enforcementEnabled)
        assertTrue(result.lockedFeatures.isEmpty())
    }

    @Test
    fun testMixedCaseAndUnknownKeysIgnored() {
        val rawList = listOf("DASHBOARD_ADVANCED", "Reports_Export_Pro", "unknown_future_feature_key")
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = rawList,
            rawEnforcementRaw = true,
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertTrue(result.enforcementEnabled)
        assertEquals(
            setOf(PremiumFeature.DASHBOARD_ADVANCED, PremiumFeature.REPORTS_EXPORT_PRO),
            result.lockedFeatures
        )
    }

    @Test
    fun testLegacyOwnerDashboardExpanded() {
        val rawList = listOf("owner_dashboard")
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = rawList,
            rawEnforcementRaw = true,
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertTrue(result.enforcementEnabled)
        assertEquals(
            setOf(PremiumFeature.DASHBOARD_ADVANCED, PremiumFeature.INVENTORY_INSIGHTS),
            result.lockedFeatures
        )
    }
}
