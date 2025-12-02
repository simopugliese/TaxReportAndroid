package it.simonepugliese.taxreport.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onConfigSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Stati per i campi di input
    // Li inizializziamo vuoti, poi li riempiamo leggendo le preferenze salvate
    var host by remember { mutableStateOf("") }
    var dbPort by remember { mutableStateOf("3306") }
    var dbName by remember { mutableStateOf("taxreport") }
    var dbUser by remember { mutableStateOf("") }
    var dbPass by remember { mutableStateOf("") }
    var smbShare by remember { mutableStateOf("TaxData") }
    var smbUser by remember { mutableStateOf("") }
    var smbPass by remember { mutableStateOf("") }

    // Caricamento dati iniziali (se esistono)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("taxreport_config", Context.MODE_PRIVATE)
        host = prefs.getString(ServiceManager.KEY_HOST, "") ?: ""
        dbPort = prefs.getString(ServiceManager.KEY_DB_PORT, "3306") ?: "3306"
        dbName = prefs.getString(ServiceManager.KEY_DB_NAME, "taxreport") ?: "taxreport"
        dbUser = prefs.getString(ServiceManager.KEY_DB_USER, "") ?: ""
        dbPass = prefs.getString(ServiceManager.KEY_DB_PASS, "") ?: ""
        smbShare = prefs.getString(ServiceManager.KEY_SMB_SHARE, "TaxData") ?: "TaxData"
        smbUser = prefs.getString(ServiceManager.KEY_SMB_USER, "") ?: ""
        smbPass = prefs.getString(ServiceManager.KEY_SMB_PASS, "") ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurazione Sistema") },
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
                .verticalScroll(rememberScrollState()), // Permette di scrollare se la tastiera copre i campi
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Database (MariaDB)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host IP (Raspberry)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dbPort,
                    onValueChange = { dbPort = it },
                    label = { Text("Porta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dbName,
                    onValueChange = { dbName = it },
                    label = { Text("Nome DB") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = dbUser,
                onValueChange = { dbUser = it },
                label = { Text("Utente DB") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dbPass,
                onValueChange = { dbPass = it },
                label = { Text("Password DB") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Storage (SMB/Samba)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = smbShare,
                onValueChange = { smbShare = it },
                label = { Text("Share Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = smbUser,
                onValueChange = { smbUser = it },
                label = { Text("Utente SMB") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = smbPass,
                onValueChange = { smbPass = it },
                label = { Text("Password SMB") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsante Salva & Connetti
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        isLoading = true
                        try {
                            val config = mapOf(
                                ServiceManager.KEY_HOST to host,
                                ServiceManager.KEY_DB_PORT to dbPort,
                                ServiceManager.KEY_DB_NAME to dbName,
                                ServiceManager.KEY_DB_USER to dbUser,
                                ServiceManager.KEY_DB_PASS to dbPass,
                                ServiceManager.KEY_SMB_SHARE to smbShare,
                                ServiceManager.KEY_SMB_USER to smbUser,
                                ServiceManager.KEY_SMB_PASS to smbPass
                            )

                            // 1. Salva e Tenta la connessione
                            // Se fallisce (es. IP sbagliato), lancerà IOException
                            ServiceManager.saveConfigAndConnect(context, config)

                            // 2. Se siamo qui, è andata bene!
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Connesso con successo!", Toast.LENGTH_SHORT).show()
                                onConfigSaved() // Callback per navigare alla Dashboard
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Errore connessione: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Connessione in corso...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salva e Connetti")
                }
            }
        }
    }
}