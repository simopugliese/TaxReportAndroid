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
import pugliesesimone.taxreport.model.*
import java.io.InputStream
import java.util.UUID

// Wrapper per gestire sia nuovi file (Uri) che file già sul server (Document)
sealed class UiAttachmentWrapper {
    data class Local(val uri: Uri, val name: String, val type: DocumentType) : UiAttachmentWrapper()
    data class Server(val document: Document, val name: String) : UiAttachmentWrapper()
}

data class AddExpenseUiState(
    val persons: List<Person> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val isEditing: Boolean = false,
    val initialData: Expense? = null
)

class AddExpenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState = _uiState.asStateFlow()

    // Lista ibrida di allegati
    private val _attachments = MutableStateFlow<List<UiAttachmentWrapper>>(emptyList())
    val attachments = _attachments.asStateFlow()

    init {
        loadPersons()
    }

    fun loadPersons() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (ServiceManager.isReady()) {
                    val persons = ServiceManager.get().allPersons
                    _uiState.value = _uiState.value.copy(persons = persons)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadExpenseForEdit(expenseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val uuid = UUID.fromString(expenseId)
                val expenseOpt = ServiceManager.get().metadata.findById(uuid)

                if (expenseOpt.isPresent) {
                    val exp = expenseOpt.get()
                    // 1. Popoliamo la lista allegati con quelli del server
                    val serverDocs = exp.documents.map { doc ->
                        // Ricava il nome file dal path relativo
                        val name = doc.relativePath.replace("\\", "/").substringAfterLast("/")
                        UiAttachmentWrapper.Server(doc, name)
                    }
                    _attachments.value = serverDocs

                    // 2. Aggiorniamo stato UI
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEditing = true,
                        initialData = exp
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Spesa non trovata")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Errore caricamento: ${e.message}")
            }
        }
    }

    fun addLocalAttachment(uri: Uri, name: String, type: DocumentType) {
        val newAtt = UiAttachmentWrapper.Local(uri, name, type)
        _attachments.value = _attachments.value + newAtt
    }

    fun removeAttachment(item: UiAttachmentWrapper) {
        _attachments.value = _attachments.value - item
    }

    fun saveExpense(
        context: Context,
        person: Person?,
        year: String,
        date: String,
        type: ExpenseType,
        description: String
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
                val currentAtts = _attachments.value
                val isEdit = _uiState.value.isEditing
                val originalExp = _uiState.value.initialData

                // 1. Prepariamo l'oggetto Expense
                val expenseToSave = if (isEdit && originalExp != null) {
                    // Manteniamo ID e Stato originali in caso di modifica
                    val e = Expense(originalExp.id, year, person, type, description, date, originalExp.expenseState)
                    // Gestione "Surviving Documents": diciamo al backend quali vecchi file tenere
                    val survivingDocs = currentAtts
                        .filterIsInstance<UiAttachmentWrapper.Server>()
                        .map { it.document }
                    e.documents = survivingDocs
                    e
                } else {
                    Expense(year, person, type, description, date)
                }

                // 2. Prepariamo i NUOVI allegati (Local) da caricare fisicamente
                val newAttachments = currentAtts
                    .filterIsInstance<UiAttachmentWrapper.Local>()
                    .map { local ->
                        val inputStream: InputStream = context.contentResolver.openInputStream(local.uri)
                            ?: throw Exception("Impossibile leggere: ${local.name}")
                        Attachment(local.type, local.name, inputStream)
                    }

                // 3. Salvataggio Backend
                ServiceManager.get().registerExpense(expenseToSave, newAttachments)

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