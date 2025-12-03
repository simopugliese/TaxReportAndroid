package it.simonepugliese.taxreport

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.simonepugliese.taxreport.ui.screens.AddExpenseScreen
import it.simonepugliese.taxreport.ui.screens.DashboardScreen
import it.simonepugliese.taxreport.ui.screens.SettingsScreen
import it.simonepugliese.taxreport.util.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            // Stato iniziale: "loading", "dashboard" o "settings"
            var startDestination by remember { mutableStateOf("loading") }

            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.IO) {
                    val isConnected = try {
                        ServiceManager.init(applicationContext)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Errore avvio: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        false
                    }
                    startDestination = if (isConnected) "dashboard" else "settings"
                }
            }

            if (startDestination == "loading") {
                Surface {
                    CircularProgressIndicator()
                }
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
                        onNavigateAddExpense = { navController.navigate("add_expense") },
                        onNavigateEditExpense = { id -> navController.navigate("add_expense?expenseId=$id") }
                    )
                }

                composable(
                    route = "add_expense?expenseId={expenseId}",
                    arguments = listOf(navArgument("expenseId") { nullable = true; type = NavType.StringType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getString("expenseId")
                    AddExpenseScreen(
                        expenseId = expenseId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}