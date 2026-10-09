package app.privatemoney.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface LedgerItem {
    data class Header(val date: LocalDate) : LedgerItem
    data class Transaction(val row: TransactionRowUi) : LedgerItem
}

class LedgerViewModel(
    private val ledger: LedgerRepository,
    categories: CategoryRepository,
) : ViewModel() {

    private val categoryNames = categories.observeActive()
        .map { cats -> cats.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<LedgerItem>> = searchQuery
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(pageSize = 30, enablePlaceholders = false)
            ) {
                ledger.pagingSource(query)
            }.flow
        }
        .cachedIn(viewModelScope)
        .combine(categoryNames) { pagingData, names ->
            pagingData.map { txn ->
                LedgerItem.Transaction(txn.toRowUi(names))
            }.insertSeparators { before, after ->
                if (after == null) return@insertSeparators null
                if (before == null) return@insertSeparators LedgerItem.Header(after.row.date)
                
                if (before.row.date != after.row.date) {
                    LedgerItem.Header(after.row.date)
                } else {
                    null
                }
            }
        }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun delete(id: String) {
        viewModelScope.launch { ledger.delete(id) }
    }
}
