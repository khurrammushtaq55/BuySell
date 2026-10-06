package com.mmushtaq04.buysell.presentation.screens.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    private val _stockItems = MutableStateFlow<List<DisplayStock>>(emptyList())
    val stockItems: StateFlow<List<DisplayStock>> = _stockItems.asStateFlow()

    init {
        loadStockItems()
    }

    private fun loadStockItems() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"

            stockRepository.observeInStockItems(activeShopId).collect { list ->
                val displayList = list.map { item ->
                    val attrMap = runCatching {
                        item.attributes?.let { json ->
                            val type = object : TypeToken<Map<String, String>>() {}.type
                            Gson().fromJson<Map<String, String>>(json, type)
                        }
                    }.getOrNull() ?: emptyMap()

                    DisplayStock(
                        id = item.id,
                        brand = item.brand,
                        model = item.model,
                        imei = item.identifier ?: "N/A",
                        category = item.categoryId,
                        color = attrMap["color"] ?: "",
                        issue = attrMap["issue"] ?: item.condition ?: "",
                        remainingQty = item.remainingQty,
                        status = item.status.name
                    )
                }
                _stockItems.value = displayList
            }
        }
    }
}
