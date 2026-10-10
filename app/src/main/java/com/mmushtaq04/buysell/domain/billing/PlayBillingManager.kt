package com.mmushtaq04.buysell.domain.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayBillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val TAG = "PlayBillingManager"

    companion object {
        const val SKU_PRO_MONTHLY = "buysell_pro_monthly"
        const val SKU_PRO_ANNUAL = "buysell_pro_annual"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _purchasedToken = MutableStateFlow<String?>(null)
    val purchasedToken: StateFlow<String?> = _purchasedToken.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    init {
        startConnection()
    }

    fun startConnection(onConnected: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    Log.i(TAG, "✓ Google Play Billing connected successfully.")
                    queryAvailableProducts()
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingReady.value = false
                Log.w(TAG, "Billing service disconnected.")
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
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.i(TAG, "✓ Fetched ${map.size} product details from Google Play.")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.responseCode}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val details = _productDetailsMap.value[productId]
        if (details == null) {
            Log.w(TAG, "Product details not loaded for '$productId'")
            return
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun restorePurchases(onPurchaseFound: (Purchase) -> Unit, onNoPurchaseFound: () -> Unit = {}) {
        if (!_isBillingReady.value) {
            startConnection { restorePurchases(onPurchaseFound, onNoPurchaseFound) }
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                val validPurchase = purchases.find { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (validPurchase != null) {
                    _purchasedToken.value = validPurchase.purchaseToken
                    onPurchaseFound(validPurchase)
                    Log.i(TAG, "✓ Restored active purchase token: ${validPurchase.purchaseToken}")
                } else {
                    onNoPurchaseFound()
                }
            } else {
                onNoPurchaseFound()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _purchasedToken.value = purchase.purchaseToken
                    Log.i(TAG, "✓ Purchase completed! Token: ${purchase.purchaseToken}")
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User canceled billing flow.")
        } else {
            Log.e(TAG, "Purchase failed with code: ${billingResult.responseCode}")
        }
    }
}
