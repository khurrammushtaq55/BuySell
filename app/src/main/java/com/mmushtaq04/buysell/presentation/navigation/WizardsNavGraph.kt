package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.presentation.screens.buy.BuyViewModel
import com.mmushtaq04.buysell.presentation.screens.buy.BuyWizardScreen
import com.mmushtaq04.buysell.presentation.screens.exchange.ExchangeViewModel
import com.mmushtaq04.buysell.presentation.screens.exchange.ExchangeWizardScreen
import com.mmushtaq04.buysell.presentation.screens.sell.SellViewModel
import com.mmushtaq04.buysell.presentation.screens.sell.SellWizardScreen

fun NavGraphBuilder.wizardsNavGraph(
    navController: NavHostController,
    registeredUserName: String,
    enabledCategories: List<CategoryEntity>
) {
    composable(NavRoutes.BuyWizard.route) {
        val buyViewModel: BuyViewModel = viewModel()

        BuyWizardScreen(
            currentUserName = registeredUserName,
            enabledCategories = enabledCategories.map { it.name },
            onNavigateBack = { navController.popBackStack() },
            onSavePurchase = { cat, brand, model, imei, color, issue, price, name, phone, cnic, recordedBy, paid, method, details, promised ->
                buyViewModel.savePurchase(
                    categoryName = cat,
                    brand = brand,
                    model = model,
                    imei = imei,
                    color = color,
                    issue = issue,
                    priceRs = price,
                    sellerName = name,
                    sellerPhone = phone,
                    sellerCnic = cnic,
                    recordedBy = recordedBy,
                    paidAmountRs = paid,
                    paymentMethodStr = method,
                    paymentDetails = details,
                    promisedDateStr = promised,
                    onSuccess = {}
                )
            },
            onSaveSuccess = { navController.popBackStack() }
        )
    }

    composable(NavRoutes.SellWizard.route) {
        val sellViewModel: SellViewModel = viewModel()
        val stockList by sellViewModel.stockList.collectAsState()

        SellWizardScreen(
            stockList = stockList,
            currentUserName = registeredUserName,
            onNavigateBack = { navController.popBackStack() },
            onSaveSale = { itemId, price, name, phone, recordedBy, received, method, details, promised ->
                sellViewModel.saveSale(
                    stockItemId = itemId,
                    salePriceRs = price,
                    buyerName = name,
                    buyerPhone = phone,
                    recordedBy = recordedBy,
                    receivedAmountRs = received,
                    paymentMethodStr = method,
                    paymentDetails = details,
                    promisedDateStr = promised,
                    onSuccess = {}
                )
            },
            onSaveSuccess = { navController.popBackStack() }
        )
    }

    composable(NavRoutes.ExchangeWizard.route) {
        val exchangeViewModel: ExchangeViewModel = viewModel()
        val stockList by exchangeViewModel.stockList.collectAsState()

        ExchangeWizardScreen(
            stockList = stockList,
            currentUserName = registeredUserName,
            onNavigateBack = { navController.popBackStack() },
            onSaveExchange = { soldItemId, newPrice, oldCat, oldBrand, oldModel, oldImei, oldColor, oldIssue, oldPrice, name, phone, recordedBy, cash, method ->
                exchangeViewModel.saveExchange(
                    soldStockItemId = soldItemId,
                    newPhonePriceRs = newPrice,
                    oldCategory = oldCat,
                    oldBrand = oldBrand,
                    oldModel = oldModel,
                    oldImei = oldImei,
                    oldColor = oldColor,
                    oldIssue = oldIssue,
                    oldPhoneValueRs = oldPrice,
                    customerName = name,
                    customerPhone = phone,
                    recordedBy = recordedBy,
                    cashPaidRs = cash,
                    paymentMethodStr = method,
                    onSuccess = {}
                )
            },
            onSaveSuccess = { navController.popBackStack() }
        )
    }
}
