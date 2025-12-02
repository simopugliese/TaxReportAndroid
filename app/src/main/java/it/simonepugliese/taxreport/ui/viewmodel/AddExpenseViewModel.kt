package it.simonepugliese.taxreport.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pugliesesimone.taxreport.model.Attachment
import pugliesesimone.taxreport.model.DocumentType
import pugliesesimone.taxreport.model.Expense
import pugliesesimone.taxreport.model.ExpenseType
import pugliesesimone.taxreport.model.Person
import java.io.InputStream

data class AddExpenseUiState(
    val persons: List<Person> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// Classe di supporto per gestire i file scelti nell'interfaccia prima del salvataggio
data class UiAttachment(
    val uri: Uri,
    val name: String,
    val type: DocumentType
)

class AddExpenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPersons()
    }

    fun loadPersons() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (ServiceManager.isReady()) {
                    // [CORREZIONE] Specifichiamo esplicitamente il tipo List<Person>
                    val persons: List<Person> = try {
                        // Se il metodo getAllPersons mancasse nel JAR, usiamo una lista vuota per ora
                        emptyList<Person>()
                    } catch (e: Exception) {
                        emptyList<Person>()
                    }

                    // Nota: Se riesci a ricompilare il JAR esponendo "getAllPersons",
                    // potrai sostituire emptyList() con la chiamata reale.

                    _uiState.value = _uiState.value.copy(persons = persons, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Servizio non connesso")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveExpense(
        context: Context,
        person: Person?,
        year: String,
        date: String,
        type: ExpenseType,
        description: String,
        uiAttachments: List<UiAttachment>
    ) {
        if (person == null) {
            _uiState.value = _uiState.value.copy(error = "Seleziona una persona")
            return
        }
        if (description.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Inserisci una descrizione")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Creiamo l'oggetto Expense
                val expense = Expense(year, person, type, description, date)

                // 2. Convertiamo gli UiAttachment in Attachment del Backend
                val attachments = uiAttachments.map { uiAtt ->
                    val inputStream: InputStream = context.contentResolver.openInputStream(uiAtt.uri)
                        ?: throw Exception("Impossibile leggere file: ${uiAtt.name}")

                    Attachment(uiAtt.type, uiAtt.name, inputStream)
                }

                // 3. [BACKEND] Salvataggio
                ServiceManager.get().registerExpense(expense, attachments)

                _uiState.value = _uiState.value.copy(isLoading = false, success = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Errore salvataggio: ${e.message}")
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }
}