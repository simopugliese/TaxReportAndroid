package it.simonepugliese.taxreport.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
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
import pugliesesimone.taxreport.model.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onConfigSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Stati Configurazione
    var host by remember { mutableStateOf("") }
    var dbPort by remember { mutableStateOf("3306") }
    var dbName by remember { mutableStateOf("taxreport") }
    var dbUser by remember { mutableStateOf("") }
    var dbPass by remember { mutableStateOf("") }
    var smbShare by remember { mutableStateOf("TaxData") }
    var smbUser by remember { mutableStateOf("") }
    var smbPass by remember { mutableStateOf("") }

    // Stati Aggiunta Persona
    var newPersonName by remember { mutableStateOf("") }
    var newPersonCF by remember { mutableStateOf("") }

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Database (MariaDB)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dbPort, onValueChange = { dbPort = it }, label = { Text("Porta") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = dbName, onValueChange = { dbName = it }, label = { Text("Nome DB") }, modifier = Modifier.weight(2f), singleLine = true)
            }
            OutlinedTextField(value = dbUser, onValueChange = { dbUser = it }, label = { Text("Utente DB") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = dbPass, onValueChange = { dbPass = it }, label = { Text("Password DB") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), singleLine = true)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text("Storage (SMB/Samba)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(value = smbShare, onValueChange = { smbShare = it }, label = { Text("Share Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = smbUser, onValueChange = { smbUser = it }, label = { Text("Utente SMB") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = smbPass, onValueChange = { smbPass = it }, label = { Text("Password SMB") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), singleLine = true)

            Spacer(modifier = Modifier.height(12.dp))

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
                            ServiceManager.saveConfigAndConnect(context, config)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Connesso!", Toast.LENGTH_SHORT).show()
                                onConfigSaved()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Salva e Connetti")
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // SEZIONE AGGIUNTA PERSONE
            Text("Gestione Persone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = newPersonName,
                onValueChange = { newPersonName = it },
                label = { Text("Nome Cognome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newPersonCF,
                onValueChange = { newPersonCF = it },
                label = { Text("Codice Fiscale") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newPersonName.isBlank() || newPersonCF.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        try {
                            if(!ServiceManager.isReady()) ServiceManager.init(context)
                            val p = Person(newPersonName, newPersonCF)
                            ServiceManager.get().registerPerson(p)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Persona aggiunta!", Toast.LENGTH_SHORT).show()
                                newPersonName = ""
                                newPersonCF = ""
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Aggiungi Persona")
            }
        }
    }
}