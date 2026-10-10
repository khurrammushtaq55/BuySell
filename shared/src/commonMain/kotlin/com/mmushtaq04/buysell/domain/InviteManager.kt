package com.mmushtaq04.buysell.domain

import kotlin.random.Random

object InviteManager {
    // Non-ambiguous 8-character charset (A-Z, 2-9, excluding 0, O, 1, I)
    private const val ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateInviteCode(): String {
        val sb = StringBuilder(8)
        for (i in 0 until 8) {
            sb.append(ALLOWED_CHARS[Random.nextInt(ALLOWED_CHARS.length)])
        }
        return sb.toString()
    }

    fun isInviteValid(expiresAt: Long, isUsed: Boolean): Boolean {
        val now = currentTimeMillis()
        return !isUsed && now <= expiresAt
    }
}

expect fun currentTimeMillis(): Long
