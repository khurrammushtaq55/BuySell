package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.data.repository.PremiumFeature
import com.mmushtaq04.buysell.domain.FreeTierLimits
import com.mmushtaq04.buysell.domain.StaffSeatLimitChecker
import com.mmushtaq04.buysell.util.LicenseCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTierLimitsTest {

    @Test
    fun testFromKeyBackwardCompatibility() {
        assertEquals(PremiumFeature.DASHBOARD_ADVANCED, PremiumFeature.fromKey("dashboard_advanced"))
        assertEquals(PremiumFeature.INVENTORY_INSIGHTS, PremiumFeature.fromKey("inventory_insights"))
        assertEquals(PremiumFeature.REPORTS_EXPORT_PRO, PremiumFeature.fromKey("reports_export_pro"))
        assertEquals(PremiumFeature.MONTHLY_SUMMARY_REPORTS, PremiumFeature.fromKey("monthly_summary_reports"))
        assertEquals(PremiumFeature.SLEEPING_PARTNER_INVITE, PremiumFeature.fromKey("sleeping_partner_invite"))
        assertEquals(PremiumFeature.EXTRA_STAFF_SEATS, PremiumFeature.fromKey("extra_staff_seats"))

        // Aliases for legacy remote keys
        assertEquals(PremiumFeature.DASHBOARD_ADVANCED, PremiumFeature.fromKey("owner_dashboard"))
        assertEquals(PremiumFeature.REPORTS_EXPORT_PRO, PremiumFeature.fromKey("data_export"))
        assertNull(PremiumFeature.fromKey("custom_currency"))
    }

    @Test
    fun testFromKeyListLegacyExpansion() {
        val legacyExpanded = PremiumFeature.fromKeyList("owner_dashboard")
        assertEquals(2, legacyExpanded.size)
        assertTrue(legacyExpanded.contains(PremiumFeature.DASHBOARD_ADVANCED))
        assertTrue(legacyExpanded.contains(PremiumFeature.INVENTORY_INSIGHTS))
    }

    @Test
    fun testDefaultLockedSetContainsAllSixKeys() {
        val defaultKeys = LicenseCache.DEFAULT_LOCKED_FEATURES
        assertEquals(6, defaultKeys.size)
        assertTrue(defaultKeys.contains("dashboard_advanced"))
        assertTrue(defaultKeys.contains("inventory_insights"))
        assertTrue(defaultKeys.contains("reports_export_pro"))
        assertTrue(defaultKeys.contains("monthly_summary_reports"))
        assertTrue(defaultKeys.contains("sleeping_partner_invite"))
        assertTrue(defaultKeys.contains("extra_staff_seats"))
    }

    @Test
    fun testStaffSeatLimitChecker() {
        // Free tier limit is 1
        assertEquals(1, FreeTierLimits.MAX_FREE_STAFF_SEATS)

        // When locked (Free Tier):
        // 0 staff members -> can add staff seat
        assertTrue(StaffSeatLimitChecker.canAddStaffSeat(currentStaffCount = 0, isExtraSeatsLocked = true))
        // 1 staff member -> blocked from adding 2nd staff seat
        assertFalse(StaffSeatLimitChecker.canAddStaffSeat(currentStaffCount = 1, isExtraSeatsLocked = true))
        // 2 staff members -> blocked
        assertFalse(StaffSeatLimitChecker.canAddStaffSeat(currentStaffCount = 2, isExtraSeatsLocked = true))

        // When unlocked (Premium Tier):
        // Any count allowed
        assertTrue(StaffSeatLimitChecker.canAddStaffSeat(currentStaffCount = 1, isExtraSeatsLocked = false))
        assertTrue(StaffSeatLimitChecker.canAddStaffSeat(currentStaffCount = 5, isExtraSeatsLocked = false))
    }
}
