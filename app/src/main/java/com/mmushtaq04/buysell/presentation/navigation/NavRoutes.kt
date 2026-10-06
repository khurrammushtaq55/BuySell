package com.mmushtaq04.buysell.presentation.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object ShopSetup : NavRoutes("shop_setup")
    object Home : NavRoutes("home")
    object BuyWizard : NavRoutes("buy_wizard")
    object SellWizard : NavRoutes("sell_wizard")
    object ExchangeWizard : NavRoutes("exchange_wizard")
    object StockList : NavRoutes("stock_list")
    object DeviceDetail : NavRoutes("device_detail/{itemId}") {
        fun createRoute(itemId: String) = "device_detail/$itemId"
    }
    object PartyList : NavRoutes("party_list")
    object PartyLedger : NavRoutes("party_ledger/{partyId}") {
        fun createRoute(partyId: String) = "party_ledger/$partyId"
    }
    object Settings : NavRoutes("settings")
    object OwnerDashboard : NavRoutes("owner_dashboard")
    object Help : NavRoutes("help")
    object AppLock : NavRoutes("app_lock")
}
