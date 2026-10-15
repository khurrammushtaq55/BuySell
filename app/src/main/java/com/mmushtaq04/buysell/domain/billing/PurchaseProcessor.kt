package com.mmushtaq04.buysell.domain.billing

import com.android.billingclient.api.Purchase
import com.mmushtaq04.buysell.data.repository.FeatureAccessRepository

sealed class PurchaseResult {
    object Success : PurchaseResult()
    object Pending : PurchaseResult()
    object VerificationFailed : PurchaseResult()
    object CoffeeConsumed : PurchaseResult()
    object Ignored : PurchaseResult()
}

object PurchaseProcessor {

    suspend fun processPurchase(
        purchase: Purchase,
        shopId: String,
        featureRepo: FeatureAccessRepository,
        onAcknowledge: suspend (token: String) -> Boolean,
        onConsumeCoffee: suspend (purchase: Purchase) -> Unit
    ): PurchaseResult {
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Check if coffee tip consumable
                if (purchase.products.contains(PlayBillingManager.SKU_BUY_COFFEE)) {
                    onConsumeCoffee(purchase)
                    PurchaseResult.CoffeeConsumed
                } else {
                    val productId = purchase.products.firstOrNull() ?: PlayBillingManager.SKU_PRO_MONTHLY
                    val isVerified = featureRepo.verifyPurchaseWithServer(
                        purchaseToken = purchase.purchaseToken,
                        productId = productId,
                        shopId = shopId
                    )

                    if (isVerified) {
                        if (!purchase.isAcknowledged) {
                            onAcknowledge(purchase.purchaseToken)
                        }
                        PurchaseResult.Success
                    } else {
                        PurchaseResult.VerificationFailed
                    }
                }
            }

            Purchase.PurchaseState.PENDING -> {
                PurchaseResult.Pending
            }

            else -> {
                PurchaseResult.Ignored
            }
        }
    }
}
