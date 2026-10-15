package com.mmushtaq04.buysell.domain

import com.mmushtaq04.buysell.data.repository.PremiumFeature

data class ParsedConfig(
    val lockedFeatures: Set<PremiumFeature>,
    val enforcementEnabled: Boolean
)

object RemoteConfigParser {

    /**
     * Pure function to parse remote config doc fields.
     * @param rawFeaturesList Raw list object from "locked_features" field (null if field missing)
     * @param rawEnforcementRaw Raw object from "enforcement_enabled" field (null if field missing)
     * @param lastKnownGood Last known good set of PremiumFeature
     * @param lastEnforcementEnabled Last known enforcement_enabled flag
     */
    fun parseRemoteConfig(
        rawFeaturesList: Any?,
        rawEnforcementRaw: Any?,
        lastKnownGood: Set<PremiumFeature>,
        lastEnforcementEnabled: Boolean = true
    ): ParsedConfig {
        val enforcementEnabled = when (rawEnforcementRaw) {
            is Boolean -> rawEnforcementRaw
            is String -> rawEnforcementRaw.toBooleanStrictOrNull() ?: lastEnforcementEnabled
            else -> lastEnforcementEnabled
        }

        if (!enforcementEnabled) {
            return ParsedConfig(
                lockedFeatures = emptySet(),
                enforcementEnabled = false
            )
        }

        val parsedSet = when (rawFeaturesList) {
            is List<*> -> {
                rawFeaturesList.flatMap { item ->
                    item?.toString()?.let { PremiumFeature.fromKeyList(it) } ?: emptyList()
                }.toSet()
            }
            else -> {
                // Field missing or invalid -> Keep last-known-good
                lastKnownGood
            }
        }

        return ParsedConfig(
            lockedFeatures = parsedSet,
            enforcementEnabled = true
        )
    }
}
