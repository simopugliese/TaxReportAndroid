package it.simonepugliese.taxreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pugliesesimone.taxreport.model.Expense
import pugliesesimone.taxreport.model.ExpenseState

data class DashboardUiState(
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val selectedYear: String = "2025",
    val error: String? = null
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    // Carica i dati appena il VM viene creato
    init {
        loadData(_uiState.value.selectedYear)
    }

    fun onYearSelected(year: String) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        loadData(year)
    }

    fun loadData(year: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!ServiceManager.isReady()) {
                    _uiState.value = _uiState.value.copy(error = "Servizio non connesso")
                    return@launch
                }

                // [RIUTILIZZO BACKEND] Chiamata diretta al metodo del JAR
                val expenses = ServiceManager.get().metadata.findByYear(year)

                // Ordiniamo per data (dal più recente) parsando la stringa rawDate
                val sorted = expenses.sortedByDescending {
                    try {
                        // Formato atteso "dd/MM/yyyy", facciamo un parsing veloce per ordinamento
                        val parts = it.rawDate.split("/")
                        if(parts.size == 3) "${parts[2]}${parts[1]}${parts[0]}" else "00000000"
                    } catch (e: Exception) { "00000000" }
                }

                _uiState.value = _uiState.value.copy(expenses = sorted, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun runComplianceCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // [RIUTILIZZO BACKEND] Esegue le regole di validazione
                ServiceManager.get().runComplianceCheck(_uiState.value.selectedYear)
                loadData(_uiState.value.selectedYear) // Ricarica per vedere i nuovi stati
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Errore validazione: ${e.message}")
            }
        }
    }
}