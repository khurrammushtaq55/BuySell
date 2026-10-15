package com.mmushtaq04.buysell.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.domain.Entitlement
import com.mmushtaq04.buysell.domain.LicenseData
import com.mmushtaq04.buysell.domain.LicensePolicy
import com.mmushtaq04.buysell.domain.RemoteConfigParser
import com.mmushtaq04.buysell.util.LicenseCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class PremiumFeature {
    DASHBOARD_ADVANCED,
    INVENTORY_INSIGHTS,
    REPORTS_EXPORT_PRO,
    MONTHLY_SUMMARY_REPORTS,
    SLEEPING_PARTNER_INVITE,
    EXTRA_STAFF_SEATS;

    fun toKey(): String = name.lowercase()

    companion object {
        fun fromKey(key: String): PremiumFeature? {
            val norm = key.lowercase().trim()
            return when (norm) {
                "dashboard_advanced" -> DASHBOARD_ADVANCED
                "inventory_insights" -> INVENTORY_INSIGHTS
                "reports_export_pro" -> REPORTS_EXPORT_PRO
                "monthly_summary_reports" -> MONTHLY_SUMMARY_REPORTS
                "sleeping_partner_invite" -> SLEEPING_PARTNER_INVITE
                "extra_staff_seats" -> EXTRA_STAFF_SEATS

                // Backward compatibility aliases for legacy remote config keys
                "owner_dashboard" -> DASHBOARD_ADVANCED
                "data_export" -> REPORTS_EXPORT_PRO
                "custom_currency" -> null
                else -> entries.find { it.name.lowercase() == norm }
            }
        }

        fun fromKeyList(key: String): List<PremiumFeature> {
            val norm = key.lowercase().trim()
            if (norm == "owner_dashboard") {
                return listOf(DASHBOARD_ADVANCED, INVENTORY_INSIGHTS)
            }
            val single = fromKey(norm)
            return if (single != null) listOf(single) else emptyList()
        }
    }
}

interface FeatureAccessRepository {
    val isPremiumUnlocked: StateFlow<Boolean>
    val entitlementState: StateFlow<Entitlement>
    val lockedFeatures: StateFlow<Set<PremiumFeature>>

    fun isFeatureLocked(feature: PremiumFeature): Boolean
    fun isFeatureLockedNow(feature: PremiumFeature): Boolean
    suspend fun syncEntitlementAndConfig(shopId: String)
    suspend fun redeemAdminPromoCode(code: String, shopId: String): Boolean
    suspend fun verifyPurchaseWithServer(purchaseToken: String, productId: String, shopId: String): Boolean
}

class FeatureAccessRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase
) : FeatureAccessRepository {

    private val TAG = "FeatureAccessRepo"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }
    private val licenseCache by lazy { LicenseCache(context) }

    private val initialCachedLicense = licenseCache.read()
    private val initialEntitlement = LicensePolicy.computeEntitlement(
        license = initialCachedLicense,
        nowMs = System.currentTimeMillis(),
        lastSeenTimeMs = licenseCache.lastSeenTimeMs
    )

    private val _entitlementState = MutableStateFlow(initialEntitlement)
    override val entitlementState: StateFlow<Entitlement> = _entitlementState.asStateFlow()

    private val _isPremiumUnlocked = MutableStateFlow(initialEntitlement.isUnlocked)
    override val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val initialLockedSet = licenseCache.readLockedFeatures().mapNotNull { PremiumFeature.fromKey(it) }.toSet()
    private val _lockedFeatures = MutableStateFlow(initialLockedSet)
    override val lockedFeatures: StateFlow<Set<PremiumFeature>> = _lockedFeatures.asStateFlow()

    override fun isFeatureLocked(feature: PremiumFeature): Boolean {
        if (_isPremiumUnlocked.value) return false
        if (!licenseCache.enforcementEnabled) return false
        return _lockedFeatures.value.contains(feature)
    }

    override fun isFeatureLockedNow(feature: PremiumFeature): Boolean {
        val cached = licenseCache.read()
        val computed = LicensePolicy.computeEntitlement(
            license = cached,
            nowMs = System.currentTimeMillis(),
            lastSeenTimeMs = licenseCache.lastSeenTimeMs
        )
        if (computed.isUnlocked) return false
        if (!licenseCache.enforcementEnabled) return false
        val savedKeys = licenseCache.readLockedFeatures()
        val currentSet = savedKeys.mapNotNull { PremiumFeature.fromKey(it) }.toSet()
        return currentSet.contains(feature)
    }

    override suspend fun syncEntitlementAndConfig(shopId: String): Unit = withContext(Dispatchers.IO) {
        runCatching {
            if (shopId.isNotBlank()) {
                // 1. Fetch server license document from shops/{shopId}/license/current
                val licenseDoc = firestore.collection("shops")
                    .document(shopId)
                    .collection("license")
                    .document("current")
                    .get()
                    .await()

                if (licenseDoc.exists()) {
                    val plan = licenseDoc.getString("plan") ?: ""
                    val status = licenseDoc.getString("status") ?: "EXPIRED"
                    val validUntil = licenseDoc.getLong("valid_until") ?: 0L
                    val source = licenseDoc.getString("source") ?: "NONE"
                    val serverTimeMs = licenseDoc.getLong("server_time_ms") ?: 0L

                    val freshLicense = LicenseData(
                        plan = plan,
                        status = status,
                        validUntilMs = validUntil,
                        source = source,
                        serverTimeMs = serverTimeMs,
                        lastFetchLocalMs = System.currentTimeMillis()
                    )
                    licenseCache.save(freshLicense)

                    if (com.mmushtaq04.buysell.BuildConfig.DEBUG) {
                        Log.i(TAG, "✓ Fetched server license for shop '$shopId': status=$status, validUntil=$validUntil")
                    } else {
                        Log.i(TAG, "✓ Fetched server license: status=$status")
                    }
                } else {
                    if (com.mmushtaq04.buysell.BuildConfig.DEBUG) {
                        Log.w(TAG, "No license document exists at shops/$shopId/license/current")
                    }
                }
            }

            // 2. Fetch remote config at most once every 4 hours or on explicit sync
            val nowMs = System.currentTimeMillis()
            if ((nowMs - licenseCache.lastConfigFetchMs) > 4 * 3600 * 1000L || shopId.isNotBlank()) {
                val configDoc = firestore.collection("app_config").document("premium_features").get().await()
                if (configDoc.exists()) {
                    val rawFeaturesList = configDoc.get("locked_features")
                    val rawEnforcement = configDoc.get("enforcement_enabled")

                    val parsedConfig = RemoteConfigParser.parseRemoteConfig(
                        rawFeaturesList = rawFeaturesList,
                        rawEnforcementRaw = rawEnforcement,
                        lastKnownGood = _lockedFeatures.value,
                        lastEnforcementEnabled = licenseCache.enforcementEnabled
                    )

                    _lockedFeatures.value = parsedConfig.lockedFeatures
                    licenseCache.saveLockedFeatures(
                        set = parsedConfig.lockedFeatures.map { it.toKey() }.toSet(),
                        enforcementEnabled = parsedConfig.enforcementEnabled
                    )
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "Sync error (using cached license & config): ${err.message}")
        }

        // 3. Always recompute entitlement from cache + current time + lastSeenTimeMs
        val cached = licenseCache.read()
        val nowMs = System.currentTimeMillis()
        val computed = LicensePolicy.computeEntitlement(
            license = cached,
            nowMs = nowMs,
            lastSeenTimeMs = licenseCache.lastSeenTimeMs
        )

        _entitlementState.value = computed
        _isPremiumUnlocked.value = computed.isUnlocked

        if (com.mmushtaq04.buysell.BuildConfig.DEBUG) {
            Log.i(TAG, "Recomputed entitlement: isUnlocked=${computed.isUnlocked}, reason=${computed.reason}")
        }
    }

    override suspend fun redeemAdminPromoCode(code: String, shopId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCode = code.trim().uppercase()
            if (cleanCode.isBlank() || shopId.isBlank()) return@runCatching false

            val payload = hashMapOf(
                "code" to cleanCode,
                "shopId" to shopId
            )

            val result = functions
                .getHttpsCallable("redeemPromoCode")
                .call(payload)
                .await()

            val resultMap = result.getData() as? Map<*, *>
            val isSuccess = resultMap?.get("success") as? Boolean ?: false

            if (isSuccess) {
                syncEntitlementAndConfig(shopId)
                if (com.mmushtaq04.buysell.BuildConfig.DEBUG) {
                    Log.i(TAG, "✓ Successfully redeemed promo code for shop '$shopId'")
                }
                return@runCatching _entitlementState.value.isUnlocked
            }
            false
        }.getOrElse { err ->
            Log.w(TAG, "Redeem promo code failed: ${err.message}")
            false
        }
    }

    override suspend fun verifyPurchaseWithServer(purchaseToken: String, productId: String, shopId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val data = hashMapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId,
                "shopId" to shopId,
                "packageName" to context.packageName
            )

            val result = functions
                .getHttpsCallable("verifyPlayPurchase")
                .call(data)
                .await()

            val resultMap = result.getData() as? Map<*, *>
            val isSuccess = resultMap?.get("success") as? Boolean ?: false

            if (isSuccess) {
                syncEntitlementAndConfig(shopId)
                if (com.mmushtaq04.buysell.BuildConfig.DEBUG) {
                    Log.i(TAG, "✓ Server purchase verification succeeded for shop '$shopId'")
                }
                return@runCatching _entitlementState.value.isUnlocked
            }
            false
        }.getOrElse { err ->
            Log.w(TAG, "Server purchase verification failed closed: ${err.message}")
            false
        }
    }
}
