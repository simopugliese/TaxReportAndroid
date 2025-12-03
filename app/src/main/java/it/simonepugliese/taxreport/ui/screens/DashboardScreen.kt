package it.simonepugliese.taxreport.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.simonepugliese.taxreport.ui.viewmodel.DashboardViewModel
import pugliesesimone.taxreport.model.Expense
import pugliesesimone.taxreport.model.ExpenseState

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Text(
                            text = "TaxReport ${state.selectedYear} ▼",
                            modifier = Modifier.clickable { expandedYear = true }
                        )
                        DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year) },
                                    onClick = {
                                        viewModel.onYearSelected(year)
                                        expandedYear = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runComplianceCheck() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna e Valida")
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, "Impostazioni")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateAddExpense) {
                Icon(Icons.Default.Add, "Nuova Spesa")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            if (state.expenses.isNotEmpty()) {
                val completed = state.expenses.count { it.expenseState == ExpenseState.COMPLETED }
                val partial = state.expenses.count { it.expenseState != ExpenseState.COMPLETED }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard("Completate", completed, Color(0xFF4CAF50), Modifier.weight(1f))
                    SummaryCard("Da Fare", partial, Color(0xFFFF9800), Modifier.weight(1f))
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.expenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onClick = { onNavigateEditExpense(expense.id.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onClick: () -> Unit) {
    val statusColor = when (expense.expenseState) {
        ExpenseState.COMPLETED -> Color(0xFF4CAF50)
        ExpenseState.PARTIAL -> Color(0xFFFF9800)
        ExpenseState.INITIAL -> Color(0xFFFF5722)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = expense.expenseType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = expense.rawDate ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = expense.description ?: "Nessuna descrizione",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = expense.person.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Box(modifier = Modifier.padding(12.dp).align(Alignment.CenterVertically)) {
                Text(
                    text = expense.expenseState.name.take(4),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}