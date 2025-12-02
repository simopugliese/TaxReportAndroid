package it.simonepugliese.taxreport.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.simonepugliese.taxreport.ui.viewmodel.AddExpenseViewModel
import it.simonepugliese.taxreport.ui.viewmodel.UiAttachment
import pugliesesimone.taxreport.model.DocumentType
import pugliesesimone.taxreport.model.ExpenseType
import pugliesesimone.taxreport.model.Person
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Stati del Form
    val dummyPersons = listOf(Person("Simone", "PGLSMN..."), Person("Mario", "MARROSS..."))
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    var expandedPerson by remember { mutableStateOf(false) }

    var selectedYear by remember { mutableStateOf("2025") }
    var expandedYear by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(ExpenseType.VISITA_MEDICA) }
    var expandedType by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    // Gestione File
    var attachments by remember { mutableStateOf<List<UiAttachment>>(emptyList()) }
    var showDocTypeDialog by remember { mutableStateOf(false) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Launcher per selezionare file
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            showDocTypeDialog = true
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            onBack()
            viewModel.resetSuccess()
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            dateStr = String.format("%02d/%02d/%04d", day, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showDocTypeDialog) {
        AlertDialog(
            onDismissRequest = { showDocTypeDialog = false },
            title = { Text("Tipo Documenti") },
            text = { Text("Che tipo di documenti stai caricando?") },
            confirmButton = { },
            dismissButton = {
                Column {
                    DocumentType.values().forEach { type ->
                        TextButton(
                            onClick = {
                                val newAttachments = pendingUris.map { uri ->
                                    UiAttachment(uri, getFileName(context, uri), type)
                                }
                                attachments = attachments + newAttachments
                                showDocTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(type.name)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuova Spesa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // [FIX] Sostituito Default.ArrowBack con AutoMirrored.Filled.ArrowBack
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RIGA 1: Persona e Anno
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                ExposedDropdownMenuBox(
                    expanded = expandedPerson,
                    onExpandedChange = { expandedPerson = !expandedPerson },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedPerson?.name ?: "",
                        onValueChange = {},
                        label = { Text("Persona") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPerson) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPerson,
                        onDismissRequest = { expandedPerson = false }
                    ) {
                        val list = if(state.persons.isNotEmpty()) state.persons else dummyPersons
                        list.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.name) },
                                onClick = {
                                    selectedPerson = person
                                    expandedPerson = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = !expandedYear },
                    modifier = Modifier.width(100.dp)
                ) {
                    OutlinedTextField(
                        value = selectedYear,
                        onValueChange = {},
                        label = { Text("Anno") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        listOf("2023", "2024", "2025").forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }

            // RIGA 2: Data e Tipo
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    label = { Text("Data") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { datePickerDialog.show() }
                )

                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        label = { Text("Tipo") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        ExpenseType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            }

            // Descrizione
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Lista Allegati
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Allegati (${attachments.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { fileLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.AttachFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(attachments) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text(item.type.name) },
                        trailingContent = {
                            IconButton(onClick = { attachments = attachments - item }) {
                                Icon(Icons.Default.Delete, "Rimuovi", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.saveExpense(
                        context, selectedPerson, selectedYear, dateStr, selectedType, description, attachments
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("SALVA SPESA")
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if(index >= 0) result = it.getString(index)
            }
        }
    }
    return result ?: "file_unknown"
}