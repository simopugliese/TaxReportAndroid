package it.simonepugliese.taxreport.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pugliesesimone.taxreport.model.*
import java.io.InputStream
import java.util.UUID

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

    private val _attachments = MutableStateFlow<List<UiAttachmentWrapper>>(emptyList())
    val attachments = _attachments.asStateFlow()

    // Canale per inviare eventi "Apri File" alla UI
    private val _viewAction = MutableSharedFlow<Intent?>()
    val viewAction = _viewAction.asSharedFlow()

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
            } catch (e: Exception) { e.printStackTrace() }
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
                    val serverDocs = exp.documents.map { doc ->
                        val name = doc.relativePath.replace("\\", "/").substringAfterLast("/")
                        UiAttachmentWrapper.Server(doc, name)
                    }
                    _attachments.value = serverDocs
                    _uiState.value = _uiState.value.copy(isLoading = false, isEditing = true, initialData = exp)
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

    // --- NUOVA LOGICA: APERTURA FILE ---
    fun openDocument(context: Context, item: UiAttachmentWrapper) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val uri: Uri
                val mimeType: String?

                when (item) {
                    is UiAttachmentWrapper.Local -> {
                        // File locale appena selezionato: abbiamo già l'URI
                        uri = item.uri
                        mimeType = context.contentResolver.getType(uri)
                    }
                    is UiAttachmentWrapper.Server -> {
                        // File remoto: scarichiamo e generiamo URI sicuro
                        val file = ServiceManager.downloadDocument(context, item.document)

                        // Generiamo URI tramite FileProvider
                        uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )

                        // Indoviniamo MimeType dall'estensione
                        val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                    }
                }

                // Creiamo l'Intent
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType ?: "*/*")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Inviamo l'intent alla UI
                _viewAction.emit(intent)
                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Impossibile aprire file: ${e.message}")
            }
        }
    }

    fun saveExpense(context: Context, person: Person?, year: String, date: String, type: ExpenseType, description: String) {
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

                val expenseToSave = if (isEdit && originalExp != null) {
                    val e = Expense(originalExp.id, year, person, type, description, date, originalExp.expenseState)
                    val survivingDocs = currentAtts.filterIsInstance<UiAttachmentWrapper.Server>().map { it.document }
                    e.documents = survivingDocs
                    e
                } else {
                    Expense(year, person, type, description, date)
                }

                val newAttachments = currentAtts.filterIsInstance<UiAttachmentWrapper.Local>().map { local ->
                    val inputStream: InputStream = context.contentResolver.openInputStream(local.uri) ?: throw Exception("Impossibile leggere: ${local.name}")
                    Attachment(local.type, local.name, inputStream)
                }

                ServiceManager.get().registerExpense(expenseToSave, newAttachments)
                _uiState.value = _uiState.value.copy(isLoading = false, success = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Errore salvataggio: ${e.message}")
            }
        }
    }

    fun resetSuccess() { _uiState.value = _uiState.value.copy(success = false) }
}