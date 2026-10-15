package com.mmushtaq04.buysell

import com.android.billingclient.api.Purchase
import com.mmushtaq04.buysell.data.repository.FeatureAccessRepository
import com.mmushtaq04.buysell.domain.Entitlement
import com.mmushtaq04.buysell.domain.billing.PlayBillingManager
import com.mmushtaq04.buysell.domain.billing.PurchaseProcessor
import com.mmushtaq04.buysell.domain.billing.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseProcessorTest {

    private val shopId = "shop_test_123"

    private class FakePurchase(
        private val state: Int,
        private val skus: List<String>,
        private val token: String,
        private val acknowledged: Boolean
    ) : Purchase("", "{}") {
        override fun getPurchaseState(): Int = state
        override fun getProducts(): List<String> = skus
        override fun getPurchaseToken(): String = token
        override fun isAcknowledged(): Boolean = acknowledged
    }

    private class FakeFeatureRepo(private val shouldVerify: Boolean) : FeatureAccessRepository {
        override val isPremiumUnlocked: StateFlow<Boolean> = MutableStateFlow(shouldVerify)
        override val entitlementState: StateFlow<Entitlement> = MutableStateFlow(Entitlement(isUnlocked = shouldVerify))
        override val lockedFeatures: StateFlow<Set<com.mmushtaq04.buysell.data.repository.PremiumFeature>> = MutableStateFlow(emptySet())

        override fun isFeatureLocked(feature: com.mmushtaq04.buysell.data.repository.PremiumFeature): Boolean = !shouldVerify
        override fun isFeatureLockedNow(feature: com.mmushtaq04.buysell.data.repository.PremiumFeature): Boolean = !shouldVerify
        override suspend fun syncEntitlementAndConfig(shopId: String) {}
        override suspend fun redeemAdminPromoCode(code: String, shopId: String): Boolean = shouldVerify
        override suspend fun verifyPurchaseWithServer(purchaseToken: String, productId: String, shopId: String): Boolean = shouldVerify
    }

    @Test
    fun testPurchasedSubscriptionVerifiedAndAcknowledged() = runBlocking {
        val purchase = FakePurchase(
            state = Purchase.PurchaseState.PURCHASED,
            skus = listOf(PlayBillingManager.SKU_PRO_ANNUAL),
            token = "token_valid_abc",
            acknowledged = false
        )

        var acknowledgedToken: String? = null
        val fakeRepo = FakeFeatureRepo(shouldVerify = true)

        val result = PurchaseProcessor.processPurchase(
            purchase = purchase,
            shopId = shopId,
            featureRepo = fakeRepo,
            onAcknowledge = {
                acknowledgedToken = it
                true
            },
            onConsumeCoffee = {}
        )

        assertEquals(PurchaseResult.Success, result)
        assertEquals("token_valid_abc", acknowledgedToken)
    }

    @Test
    fun testPurchasedSubscriptionFailsServerVerificationNotAcknowledged() = runBlocking {
        val purchase = FakePurchase(
            state = Purchase.PurchaseState.PURCHASED,
            skus = listOf(PlayBillingManager.SKU_PRO_MONTHLY),
            token = "token_fake_xyz",
            acknowledged = false
        )

        var acknowledgeCalled = false
        val fakeRepo = FakeFeatureRepo(shouldVerify = false)

        val result = PurchaseProcessor.processPurchase(
            purchase = purchase,
            shopId = shopId,
            featureRepo = fakeRepo,
            onAcknowledge = {
                acknowledgeCalled = true
                true
            },
            onConsumeCoffee = {}
        )

        assertEquals(PurchaseResult.VerificationFailed, result)
        assertFalse(acknowledgeCalled)
    }

    @Test
    fun testPendingPurchaseReturnsPendingState() = runBlocking {
        val purchase = FakePurchase(
            state = Purchase.PurchaseState.PENDING,
            skus = listOf(PlayBillingManager.SKU_PRO_MONTHLY),
            token = "token_pending_123",
            acknowledged = false
        )

        val fakeRepo = FakeFeatureRepo(shouldVerify = true)

        val result = PurchaseProcessor.processPurchase(
            purchase = purchase,
            shopId = shopId,
            featureRepo = fakeRepo,
            onAcknowledge = { true },
            onConsumeCoffee = {}
        )

        assertEquals(PurchaseResult.Pending, result)
    }

    @Test
    fun testCoffeeTipPurchaseConsumed() = runBlocking {
        val purchase = FakePurchase(
            state = Purchase.PurchaseState.PURCHASED,
            skus = listOf(PlayBillingManager.SKU_BUY_COFFEE),
            token = "token_coffee_123",
            acknowledged = false
        )

        var coffeeConsumed = false
        val fakeRepo = FakeFeatureRepo(shouldVerify = true)

        val result = PurchaseProcessor.processPurchase(
            purchase = purchase,
            shopId = shopId,
            featureRepo = fakeRepo,
            onAcknowledge = { true },
            onConsumeCoffee = {
                coffeeConsumed = true
            }
        )

        assertEquals(PurchaseResult.CoffeeConsumed, result)
        assertTrue(coffeeConsumed)
    }
}
