package com.mmushtaq04.buysell.domain

import com.mmushtaq04.buysell.data.local.dao.PartyDao
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.enums.PartyTypeHint

object PartyResolver {

    suspend fun findOrCreate(
        partyDao: PartyDao,
        shopId: String,
        rawName: String,
        rawPhone: String?,
        rawCnic: String?,
        typeHint: PartyTypeHint,
        recordedBy: String,
        nowMs: Long,
        generateId: () -> String
    ): PartyEntity {
        val normalizedPhone = PartyNormalizer.normalizePhone(rawPhone)
        val normalizedCnic = PartyNormalizer.normalizeCnic(rawCnic)
        val activeParties = partyDao.getAllParties(shopId)

        var matchedParty: PartyEntity? = if (normalizedCnic.isNotBlank()) {
            activeParties.firstOrNull { candidate ->
                candidate.cnic?.let { PartyNormalizer.normalizeCnic(it) } == normalizedCnic
            }
        } else null

        if (matchedParty == null && normalizedPhone.isNotBlank()) {
            matchedParty = activeParties.firstOrNull { candidate ->
                candidate.phone?.let { PartyNormalizer.normalizePhone(it) } == normalizedPhone
            }
        }

        if (matchedParty != null) {
            val updatedName = matchedParty.name.ifBlank { rawName.ifBlank { "Party" } }
            val updatedPhone = matchedParty.phone?.ifBlank { rawPhone?.ifBlank { null } } ?: rawPhone?.ifBlank { null }
            val updatedCnic = matchedParty.cnic?.ifBlank { rawCnic?.ifBlank { null } } ?: rawCnic?.ifBlank { null }

            val resolved = matchedParty.copy(
                name = updatedName,
                phone = updatedPhone,
                cnic = updatedCnic,
                updatedAt = nowMs,
                updatedBy = recordedBy
            )
            partyDao.insertParty(resolved)
            return resolved
        }

        val newParty = PartyEntity(
            id = generateId(),
            shopId = shopId,
            name = rawName.ifBlank { "Party" },
            phone = rawPhone?.ifBlank { null },
            cnic = rawCnic?.ifBlank { null },
            typeHint = typeHint,
            createdAt = nowMs,
            updatedAt = nowMs,
            createdBy = recordedBy,
            updatedBy = recordedBy
        )
        partyDao.insertParty(newParty)
        return newParty
    }
}
