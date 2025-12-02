package it.simonepugliese.taxreport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.simonepugliese.taxreport.ui.screens.AddExpenseScreen
import it.simonepugliese.taxreport.ui.screens.DashboardScreen
import it.simonepugliese.taxreport.ui.screens.SettingsScreen
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            // Stato iniziale
            var startDestination by remember { mutableStateOf("loading") }

            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.IO) {
                    val isConnected = ServiceManager.init(applicationContext)
                    startDestination = if (isConnected) "dashboard" else "settings"
                }
            }

            if (startDestination == "loading") {
                return@setContent
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable("settings") {
                    SettingsScreen(
                        onConfigSaved = {
                            navController.navigate("dashboard") {
                                popUpTo("settings") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    DashboardScreen(
                        onNavigateSettings = { navController.navigate("settings") },
                        onNavigateAddExpense = { navController.navigate("add_expense") }
                    )
                }

                composable("add_expense") {
                    AddExpenseScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}