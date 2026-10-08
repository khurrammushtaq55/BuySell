package com.mmushtaq04.buysell.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.CategoryPresets
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _enabledCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val enabledCategories: StateFlow<List<CategoryEntity>> = _enabledCategories.asStateFlow()

    private val _allCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val allCategories: StateFlow<List<CategoryEntity>> = _allCategories.asStateFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            db.appMetaDao().observeAppMeta().collect { meta ->
                val user = db.userDao().getPrimaryUser()
                val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

                if (activeShopId.isNotBlank()) {
                    val existing = db.categoryDao().getCategories(activeShopId)
                    if (existing.isEmpty()) {
                        val presets = CategoryPresets.getPresetCategories(activeShopId)
                        db.categoryDao().insertCategories(presets)
                    }

                    db.categoryDao().observeCategories(activeShopId).collect { enabledList ->
                        _enabledCategories.value = enabledList
                    }
                }
            }
        }

        viewModelScope.launch {
            db.appMetaDao().observeAppMeta().collect { meta ->
                val user = db.userDao().getPrimaryUser()
                val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

                if (activeShopId.isNotBlank()) {
                    db.categoryDao().observeAllCategories(activeShopId).collect { allList ->
                        _allCategories.value = allList
                    }
                }
            }
        }
    }

    fun toggleCategoryEnabled(category: CategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(
                enabled = !category.enabled,
                updatedAt = System.currentTimeMillis()
            )
            db.categoryDao().updateCategory(updated)
        }
    }
}
