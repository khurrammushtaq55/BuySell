package com.mmushtaq04.buysell.domain.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.repository.FeatureAccessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class PlayBillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)

    private val TAG = "PlayBillingManager"

    companion object {
        const val SKU_PRO_MONTHLY = "buysell_pro_monthly"
        const val SKU_PRO_ANNUAL = "buysell_pro_annual"
        const val SKU_BUY_COFFEE = "buysell_coffee_tip"

        @Volatile
        private var INSTANCE: PlayBillingManager? = null

        fun getInstance(context: Context): PlayBillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayBillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var featureRepo: FeatureAccessRepository? = null
    private var activeShopId: String = ""
    private var activeUserUid: String = ""

    private var retryDelayMs = 1000L

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _billingEventMessage = MutableStateFlow<String?>(null)
    val billingEventMessage: StateFlow<String?> = _billingEventMessage.asStateFlow()

    init {
        startConnection()
    }

    private var hasAutoRecoveredOnStart = false

    fun initializeSession(repo: FeatureAccessRepository, shopId: String, userUid: String) {
        this.featureRepo = repo
        this.activeShopId = shopId
        this.activeUserUid = userUid

        if (_isBillingReady.value && shopId.isNotBlank() && !hasAutoRecoveredOnStart) {
            hasAutoRecoveredOnStart = true
            Log.i(TAG, "Triggering automatic purchase recovery on app startup for shop '$shopId'")
            restorePurchases(shopId, repo)
        }
    }

    fun clearEventMessage() {
        _billingEventMessage.value = null
    }

    fun startConnection(onConnected: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    retryDelayMs = 1000L
                    Log.i(TAG, "✓ Google Play Billing connected successfully.")
                    queryAvailableProducts()

                    if (activeShopId.isNotBlank() && featureRepo != null && !hasAutoRecoveredOnStart) {
                        hasAutoRecoveredOnStart = true
                        Log.i(TAG, "Triggering automatic purchase recovery on billing connection for shop '$activeShopId'")
                        restorePurchases(activeShopId, featureRepo!!)
                    }

                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingReady.value = false
                Log.w(TAG, "Billing service disconnected. Retrying in ${retryDelayMs}ms...")
                scope.launch {
                    delay(retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(16000L)
                    startConnection()
                }
            }
        })
    }

    fun queryAvailableProducts() {
        if (!_isBillingReady.value) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_PRO_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BUY_COFFEE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = productDetailsResult.productDetailsList ?: emptyList()
                val map = list.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.i(TAG, "✓ Fetched ${map.size} product details from Google Play.")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.responseCode}")
            }
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        shopId: String,
        userUid: String
    ) {
        val details = _productDetailsMap.value[productId]
        if (details == null) {
            _billingEventMessage.value = applicationContext.getString(R.string.billing_btn_loading)
            Log.w(TAG, "Product details not loaded for '$productId'")
            return
        }

        val isSub = details.productType == BillingClient.ProductType.SUBS
        val offerToken = if (isSub) details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: "" else ""

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (offerToken.isNotEmpty()) {
            productParamsBuilder.setOfferToken(offerToken)
        }

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))

        // Bind obfuscated account ID & profile ID (truncated to Google Play's 64-char limit)
        if (shopId.isNotBlank()) {
            billingFlowParamsBuilder.setObfuscatedAccountId(shopId.take(64))
        }
        if (userUid.isNotBlank()) {
            billingFlowParamsBuilder.setObfuscatedProfileId(userUid.take(64))
        }

        val result = billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.responseCode}")
            _billingEventMessage.value = applicationContext.getString(R.string.billing_error_generic)
        }
    }

    suspend fun acknowledgePurchase(purchaseToken: String): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            val success = billingResult.responseCode == BillingClient.BillingResponseCode.OK
            if (success) {
                Log.i(TAG, "✓ Purchase acknowledged successfully.")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.responseCode}")
            }
            if (cont.isActive) {
                cont.resume(success)
            }
        }
    }

    fun consumePurchase(purchase: Purchase, onConsumed: () -> Unit) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { _, _ ->
            onConsumed()
        }
    }

    fun restorePurchases(
        shopId: String,
        repo: FeatureAccessRepository,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!_isBillingReady.value) {
            startConnection { restorePurchases(shopId, repo, onComplete) }
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                scope.launch {
                    var restoredCount = 0
                    for (purchase in purchases) {
                        val res = PurchaseProcessor.processPurchase(
                            purchase = purchase,
                            shopId = shopId,
                            featureRepo = repo,
                            onAcknowledge = { acknowledgePurchase(it) },
                            onConsumeCoffee = { consumePurchase(it) {} }
                        )
                        if (res is PurchaseResult.Success) {
                            restoredCount++
                        }
                    }
                    onComplete(restoredCount > 0)
                }
            } else {
                onComplete(false)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty() && featureRepo != null && activeShopId.isNotBlank()) {
                    scope.launch {
                        for (purchase in purchases) {
                            val result = PurchaseProcessor.processPurchase(
                                purchase = purchase,
                                shopId = activeShopId,
                                featureRepo = featureRepo!!,
                                onAcknowledge = { acknowledgePurchase(it) },
                                onConsumeCoffee = { consumePurchase(it) {} }
                            )

                            when (result) {
                                is PurchaseResult.Pending -> {
                                    _billingEventMessage.value = applicationContext.getString(R.string.billing_payment_pending)
                                }
                                is PurchaseResult.VerificationFailed -> {
                                    _billingEventMessage.value = applicationContext.getString(R.string.billing_error_generic)
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled billing flow.")
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.i(TAG, "Item already owned. Triggering restore...")
                if (featureRepo != null && activeShopId.isNotBlank()) {
                    restorePurchases(activeShopId, featureRepo!!)
                }
            }

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                Log.w(TAG, "Billing network error: ${billingResult.responseCode}")
                _billingEventMessage.value = applicationContext.getString(R.string.billing_error_network)
            }

            else -> {
                Log.e(TAG, "Purchase failed with code: ${billingResult.responseCode}")
                _billingEventMessage.value = applicationContext.getString(R.string.billing_error_generic)
            }
        }
    }
}
