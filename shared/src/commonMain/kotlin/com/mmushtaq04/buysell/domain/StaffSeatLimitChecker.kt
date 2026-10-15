package com.mmushtaq04.buysell.domain

object StaffSeatLimitChecker {

    fun canAddStaffSeat(
        currentStaffCount: Int,
        isExtraSeatsLocked: Boolean
    ): Boolean {
        if (!isExtraSeatsLocked) return true
        return currentStaffCount < FreeTierLimits.MAX_FREE_STAFF_SEATS
    }
}
