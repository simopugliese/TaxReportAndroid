package it.simonepugliese.taxreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pugliesesimone.taxreport.model.Expense
import pugliesesimone.taxreport.model.ExpenseType
import pugliesesimone.taxreport.model.Person

data class DashboardUiState(
    val expenses: List<Expense> = emptyList(),       // Dati grezzi
    val filteredExpenses: List<Expense> = emptyList(), // Dati da mostrare
    val persons: List<Person> = emptyList(),         // Anagrafica per i dialoghi
    val isLoading: Boolean = false,
    val selectedYear: String = "2025",

    // Nuovi stati per filtri multipli (Set invece di String singola)
    val selectedPersonIds: Set<String> = emptySet(),
    val selectedCategories: Set<ExpenseType> = emptySet(),

    val error: String? = null
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData(_uiState.value.selectedYear)
    }

    fun onYearSelected(year: String) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        loadData(year)
    }

    // Imposta massivamente i filtri persona (dal Dialog)
    fun setPersonFilters(ids: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedPersonIds = ids)
        applyFilters()
    }

    // Imposta massivamente i filtri categoria (dal Dialog)
    fun setCategoryFilters(types: Set<ExpenseType>) {
        _uiState.value = _uiState.value.copy(selectedCategories = types)
        applyFilters()
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedPersonIds = emptySet(),
            selectedCategories = emptySet()
        )
        applyFilters()
    }

    // Cuore della logica combinata
    private fun applyFilters() {
        val state = _uiState.value
        val all = state.expenses

        val filtered = all.filter { expense ->
            // Logica: (Se nessun filtro persona attivo, accetta tutto, ALTRIMENTI l'ID deve essere nel Set)
            // AND
            // (Se nessun filtro categoria attivo, accetta tutto, ALTRIMENTI il tipo deve essere nel Set)

            val personMatch = if (state.selectedPersonIds.isEmpty()) true
            else expense.person.id.toString() in state.selectedPersonIds

            val categoryMatch = if (state.selectedCategories.isEmpty()) true
            else expense.expenseType in state.selectedCategories

            personMatch && categoryMatch
        }

        _uiState.value = state.copy(filteredExpenses = filtered)
    }

    fun loadData(year: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!ServiceManager.isReady()) {
                    _uiState.value = _uiState.value.copy(error = "Servizio non connesso")
                    return@launch
                }

                val expenses = ServiceManager.get().metadata.findByYear(year)
                val persons = ServiceManager.get().allPersons

                val sorted = expenses.sortedByDescending {
                    try {
                        val parts = it.rawDate.split("/")
                        if(parts.size == 3) "${parts[2]}${parts[1]}${parts[0]}" else "00000000"
                    } catch (e: Exception) { "00000000" }
                }

                _uiState.value = _uiState.value.copy(
                    expenses = sorted,
                    persons = persons,
                    isLoading = false
                )
                // Riapplica i filtri sui nuovi dati
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun runComplianceCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                ServiceManager.get().runComplianceCheck(_uiState.value.selectedYear)
                loadData(_uiState.value.selectedYear)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Errore validazione: ${e.message}")
            }
        }
    }
}