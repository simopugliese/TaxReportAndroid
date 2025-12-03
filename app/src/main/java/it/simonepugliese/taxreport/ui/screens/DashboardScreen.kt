package it.simonepugliese.taxreport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.simonepugliese.taxreport.ui.viewmodel.DashboardViewModel
import pugliesesimone.taxreport.model.Expense
import pugliesesimone.taxreport.model.ExpenseState
import pugliesesimone.taxreport.model.ExpenseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSettings: () -> Unit,
    onNavigateAddExpense: () -> Unit,
    onNavigateEditExpense: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val years = listOf("2023", "2024", "2025")
    var expandedYear by remember { mutableStateOf(false) }

    // Dialoghi
    var showPersonDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expandedYear = true }) {
                        Text(text = "TaxReport ${state.selectedYear}", fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(text = { Text(year) }, onClick = {
                                viewModel.onYearSelected(year)
                                expandedYear = false
                            })
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runComplianceCheck() }) {
                        Icon(Icons.Default.Refresh, "Check")
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, "Setup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateAddExpense,
                icon = { Icon(Icons.Default.Add, "Nuova") },
                text = { Text("NUOVA SPESA") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {

            // --- BARRA FILTRI MULTIPLI ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. TUTTI (Reset)
                val allSelected = state.selectedPersonIds.isEmpty() && state.selectedCategories.isEmpty()
                FilterChip(
                    selected = allSelected,
                    onClick = { viewModel.clearFilters() },
                    label = { Text("Tutti") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(18.dp)) }
                )

                // 2. PERSONE (Con contatore)
                val pCount = state.selectedPersonIds.size
                FilterChip(
                    selected = pCount > 0,
                    onClick = { showPersonDialog = true },
                    label = { Text(if (pCount > 0) "Persone ($pCount)" else "Persone") },
                    leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(14.dp)) }
                )

                // 3. CATEGORIE (Con contatore)
                val cCount = state.selectedCategories.size
                FilterChip(
                    selected = cCount > 0,
                    onClick = { showTypeDialog = true },
                    label = { Text(if (cCount > 0) "Categ. ($cCount)" else "Categorie") },
                    leadingIcon = { Icon(Icons.Default.Category, null, Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(14.dp)) }
                )
            }

            // --- DIALOGHI MULTI-SELEZIONE ---

            if (showPersonDialog) {
                MultiSelectionDialog(
                    title = "Seleziona Persone",
                    items = state.persons,
                    selectedKeys = state.selectedPersonIds,
                    keySelector = { it.id.toString() },
                    labelSelector = { it.name },
                    onDismiss = { showPersonDialog = false },
                    onConfirm = { selected ->
                        viewModel.setPersonFilters(selected)
                        showPersonDialog = false
                    }
                )
            }

            if (showTypeDialog) {
                MultiSelectionDialog(
                    title = "Seleziona Categorie",
                    items = ExpenseType.values().toList(),
                    selectedKeys = state.selectedCategories.map { it.name }.toSet(),
                    keySelector = { it.name },
                    labelSelector = { it.name.replace("_", " ").lowercase().capitalize() },
                    onDismiss = { showTypeDialog = false },
                    onConfirm = { selectedNames ->
                        // Riconvertiamo le stringhe in Enum
                        val selectedEnums = selectedNames.map { ExpenseType.valueOf(it) }.toSet()
                        viewModel.setCategoryFilters(selectedEnums)
                        showTypeDialog = false
                    }
                )
            }

            // --- RIEPILOGO FILTRATO ---
            if (state.filteredExpenses.isNotEmpty()) {
                val completed = state.filteredExpenses.count { it.expenseState == ExpenseState.COMPLETED }
                val partial = state.filteredExpenses.size - completed

                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Filtrate: ${state.filteredExpenses.size}  •  ", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Text("Ok: $completed", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    Text("  •  ", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Text("Da fare: $partial", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
            }

            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // --- LISTA ---
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredExpenses) { expense ->
                    ExpenseCardModern(expense) { onNavigateEditExpense(expense.id.toString()) }
                }
            }
        }
    }
}

// Dialogo Generico con Checkbox
@Composable
fun <T> MultiSelectionDialog(
    title: String,
    items: List<T>,
    selectedKeys: Set<String>,
    keySelector: (T) -> String,
    labelSelector: (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    // Stato temporaneo locale al dialog per permettere "Annulla"
    var currentSelection by remember { mutableStateOf(selectedKeys) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(items) { item ->
                    val key = keySelector(item)
                    val isSelected = key in currentSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentSelection = if (isSelected) currentSelection - key else currentSelection + key
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null) // Gestito dal click sulla riga
                        Spacer(Modifier.width(8.dp))
                        Text(text = labelSelector(item), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentSelection) }) { Text("Applica") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Composable
fun ExpenseCardModern(expense: Expense, onClick: () -> Unit) {
    val statusColor = when (expense.expenseState) {
        ExpenseState.COMPLETED -> Color(0xFF4CAF50)
        ExpenseState.PARTIAL -> Color(0xFFFF9800)
        ExpenseState.INITIAL -> Color(0xFFE91E63)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = expense.rawDate ?: "--/--/----", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = expense.expenseState.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = expense.description?.ifBlank { "Nessuna descrizione" } ?: "Nessuna descrizione",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = expense.expenseType.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))

                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = expense.person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}