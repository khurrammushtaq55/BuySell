package com.mmushtaq04.buysell.presentation.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.enums.PartyTypeHint
import com.mmushtaq04.buysell.data.repository.PartyRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PartyListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val partyRepository = PartyRepositoryImpl(db)

    private val _parties = MutableStateFlow<List<DisplayPartyBalance>>(emptyList())
    val parties: StateFlow<List<DisplayPartyBalance>> = _parties.asStateFlow()

    init {
        loadPartiesWithBalances()
    }

    private fun loadPartiesWithBalances() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isNotBlank()) {
                partyRepository.observePartiesWithBalances(activeShopId).collect { list ->
                    _parties.value = list.map { pb ->
                        DisplayPartyBalance(
                            id = pb.partyId,
                            name = pb.name,
                            phone = pb.phone ?: "",
                            balance = pb.balance
                        )
                    }
                }
            }
        }
    }

    fun addNewParty(name: String, phone: String, cnic: String) {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            val now = System.currentTimeMillis()

            val party = PartyEntity(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                name = name,
                phone = phone.ifBlank { null },
                cnic = cnic.ifBlank { null },
                typeHint = PartyTypeHint.BOTH,
                createdAt = now,
                updatedAt = now,
                createdBy = "Owner",
                updatedBy = "Owner"
            )
            db.partyDao().insertParty(party)
        }
    }
}
