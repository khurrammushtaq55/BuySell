package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.data.repository.PremiumFeature
import com.mmushtaq04.buysell.domain.RemoteConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigParserTest {

    private val defaultLockedSet = setOf(
        PremiumFeature.OWNER_DASHBOARD,
        PremiumFeature.DATA_EXPORT,
        PremiumFeature.SLEEPING_PARTNER_INVITE,
        PremiumFeature.CUSTOM_CURRENCY,
        PremiumFeature.MONTHLY_SUMMARY_REPORTS
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
        val rawList = listOf("owner_dashboard", "data_export")
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
        val rawList = listOf("OWNER_DASHBOARD", "Data_Export", "unknown_future_feature_key")
        val result = RemoteConfigParser.parseRemoteConfig(
            rawFeaturesList = rawList,
            rawEnforcementRaw = true,
            lastKnownGood = defaultLockedSet,
            lastEnforcementEnabled = true
        )

        assertTrue(result.enforcementEnabled)
        assertEquals(
            setOf(PremiumFeature.OWNER_DASHBOARD, PremiumFeature.DATA_EXPORT),
            result.lockedFeatures
        )
    }
}
