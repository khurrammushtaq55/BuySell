package com.mmushtaq04.buysell.domain

import com.mmushtaq04.buysell.data.local.entity.InviteEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object InviteManager {
    // Non-ambiguous 8-character charset (A-Z, 2-9, excluding 0, O, 1, I)
    private const val ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generateInviteCode(): String {
        val sb = StringBuilder(8)
        for (i in 0 until 8) {
            sb.append(ALLOWED_CHARS[random.nextInt(ALLOWED_CHARS.length)])
        }
        return sb.toString()
    }

    fun createInvite(
        shopId: String,
        role: Role,
        createdByUserId: String
    ): InviteEntity {
        val now = System.currentTimeMillis()
        val expiresAt = now + TimeUnit.DAYS.toMillis(7)

        return InviteEntity(
            code = generateInviteCode(),
            shopId = shopId,
            role = role,
            createdByUserId = createdByUserId,
            createdAt = now,
            expiresAt = expiresAt
        )
    }
}
