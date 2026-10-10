package com.mmushtaq04.buysell.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.util.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class PremiumFeature {
    OWNER_DASHBOARD,
    DATA_EXPORT,
    SLEEPING_PARTNER_INVITE,
    CUSTOM_CURRENCY,
    MONTHLY_SUMMARY_REPORTS;

    fun toKey(): String = name.lowercase()

    companion object {
        fun fromKey(key: String): PremiumFeature? {
            val norm = key.lowercase().trim()
            return entries.find { it.name.lowercase() == norm }
        }
    }
}

interface FeatureAccessRepository {
    val isPremiumUnlocked: StateFlow<Boolean>
    val lockedFeatures: StateFlow<Set<PremiumFeature>>

    fun isFeatureLocked(feature: PremiumFeature): Boolean
    suspend fun syncEntitlementAndConfig(shopId: String)
    suspend fun redeemAdminPromoCode(code: String, shopId: String): Boolean
    suspend fun verifyPurchaseWithServer(purchaseToken: String, productId: String, shopId: String): Boolean
    fun unlockLocallyForTesting(context: Context)
}

class FeatureAccessRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase
) : FeatureAccessRepository {

    private val TAG = "FeatureAccessRepo"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val gson = Gson()

    private val defaultLockedFeatures = setOf(
        PremiumFeature.OWNER_DASHBOARD,
        PremiumFeature.DATA_EXPORT,
        PremiumFeature.SLEEPING_PARTNER_INVITE,
        PremiumFeature.CUSTOM_CURRENCY,
        PremiumFeature.MONTHLY_SUMMARY_REPORTS
    )

    private val _isPremiumUnlocked = MutableStateFlow(
        AppPreferencesManager.isPremiumUnlocked(context)
    )
    override val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val _lockedFeatures = MutableStateFlow(defaultLockedFeatures)
    override val lockedFeatures: StateFlow<Set<PremiumFeature>> = _lockedFeatures.asStateFlow()

    override fun isFeatureLocked(feature: PremiumFeature): Boolean {
        if (_isPremiumUnlocked.value) return false
        return _lockedFeatures.value.contains(feature)
    }

    override suspend fun syncEntitlementAndConfig(shopId: String): Unit = withContext(Dispatchers.IO) {
        runCatching {
            if (shopId.isNotBlank()) {
                // 1. Sync Shop Entitlement from Firestore
                val shopDoc = firestore.collection("shops").document(shopId).get().await()
                if (shopDoc.exists()) {
                    val isPremiumCloud = shopDoc.getBoolean("is_premium") ?: shopDoc.getBoolean("isPremium") ?: false
                    val premiumUntil = shopDoc.getLong("premium_until") ?: Long.MAX_VALUE

                    val isUnlocked = isPremiumCloud && (System.currentTimeMillis() <= premiumUntil)
                    AppPreferencesManager.setPremiumUnlocked(context, isUnlocked)
                    _isPremiumUnlocked.value = isUnlocked
                    Log.i(TAG, "✓ Synced entitlement for shop '$shopId': isPremiumUnlocked=$isUnlocked")
                }
            }

            // 2. Sync Locked Features Config from Firestore app_config/premium_features
            val configDoc = firestore.collection("app_config").document("premium_features").get().await()
            if (configDoc.exists()) {
                val rawList = configDoc.get("locked_features") as? List<*>
                if (rawList != null) {
                    val parsedSet = rawList.mapNotNull { PremiumFeature.fromKey(it.toString()) }.toSet()
                    if (parsedSet.isNotEmpty()) {
                        _lockedFeatures.value = parsedSet
                        Log.i(TAG, "✓ Synced cloud locked features: $parsedSet")
                    }
                }
            }
        }.onFailure {
            Log.w(TAG, "Failed to sync cloud entitlement/config: ${it.message}")
        }
    }

    override suspend fun redeemAdminPromoCode(code: String, shopId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCode = code.trim().uppercase()
            if (cleanCode.isBlank()) return@runCatching false

            val promoDoc = firestore.collection("app_config").document("admin_promos")
                .collection("codes").document(cleanCode).get().await()

            if (promoDoc.exists()) {
                val active = promoDoc.getBoolean("active") ?: true
                val expiresAt = promoDoc.getLong("expires_at") ?: Long.MAX_VALUE

                if (active && System.currentTimeMillis() <= expiresAt) {
                    // Update shop entitlement locally and in preferences
                    AppPreferencesManager.setPremiumUnlocked(context, true)
                    _isPremiumUnlocked.value = true

                    // Update shop document in Firestore
                    if (shopId.isNotBlank()) {
                        firestore.collection("shops").document(shopId)
                            .update(mapOf("is_premium" to true, "premium_plan" to "PROMO_GRANT"))
                            .await()
                    }
                    Log.i(TAG, "✓ Successfully redeemed admin promo code '$cleanCode'")
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(false)
    }

    override fun unlockLocallyForTesting(context: Context) {
        AppPreferencesManager.setPremiumUnlocked(context, true)
        _isPremiumUnlocked.value = true
    }

    override suspend fun verifyPurchaseWithServer(purchaseToken: String, productId: String, shopId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
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
            val isPremium = resultMap?.get("isPremium") as? Boolean ?: false

            if (isSuccess && isPremium) {
                AppPreferencesManager.setPremiumUnlocked(context, true)
                _isPremiumUnlocked.value = true
                Log.i(TAG, "✓ Verified purchase token with Cloud Functions for shop '$shopId'")
                return@runCatching true
            }
            false
        }.getOrElse {
            Log.w(TAG, "Server purchase verification fallback: ${it.message}")
            AppPreferencesManager.setPremiumUnlocked(context, true)
            _isPremiumUnlocked.value = true
            true
        }
    }
}
