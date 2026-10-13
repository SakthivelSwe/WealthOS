package app.privatemoney.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.local.AccountEntity
import app.privatemoney.data.local.AccountWithBalance
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accounts: AccountRepository,
) : ViewModel() {

    val activeAccounts: StateFlow<List<AccountEntity>> = accounts.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountsWithBalances: StateFlow<List<AccountWithBalance>> = accounts.observeActiveWithBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAccount(name: String, type: AccountType, openingBalanceMinor: Long, currency: Currency) {
        viewModelScope.launch {
            accounts.create(name, type, openingBalanceMinor, currency)
        }
    }

    fun updateAccount(id: String, name: String, type: AccountType) {
        viewModelScope.launch {
            accounts.updateDetails(id, name, type)
        }
    }

    fun updateOpeningBalance(id: String, newOpeningBalanceMinor: Long) {
        viewModelScope.launch {
            accounts.updateOpeningBalance(id, newOpeningBalanceMinor)
        }
    }

    fun archiveAccount(id: String) {
        viewModelScope.launch {
            accounts.archive(id)
        }
    }
}
