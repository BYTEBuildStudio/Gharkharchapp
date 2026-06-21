package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AddExpenseScreen
import com.example.ui.AuthScreen
import com.example.ui.DashboardScreen
import com.example.ui.ReportsScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge immersive draws
        enableEdgeToEdge()
        
        setContent {
            val isDarkSelected by viewModel.isDarkMode.collectAsState()
            val activeUser by viewModel.activeUser.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkSelected) {
                var selectedTab by remember { mutableIntStateOf(0) }
                
                if (activeUser == null) {
                    AuthScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") },
                                    modifier = Modifier.testTag("nav_tab_dashboard")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                                    label = { Text("Add Spend") },
                                    modifier = Modifier.testTag("nav_tab_add")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                    label = { Text("History") },
                                    modifier = Modifier.testTag("nav_tab_history")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        },
                        floatingActionButton = {
                            // Quick Add FAB shows up from any screen except on Add tab
                            if (selectedTab != 1) {
                                ExtendedFloatingActionButton(
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense Icon") },
                                    text = { Text("Log Spend") },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("global_add_expense_fab")
                                )
                            }
                        }
                    ) { innerPadding ->
                        // Host screens inside edge-to-edge scaffolding
                        val contentModifier = Modifier.padding(innerPadding)
                        
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { selectedTab = 1 },
                                modifier = contentModifier
                            )
                            1 -> AddExpenseScreen(
                                viewModel = viewModel,
                                onSuccess = { selectedTab = 0 }, // Navigate to Dashboard on save
                                modifier = contentModifier
                            )
                            2 -> ReportsScreen(
                                viewModel = viewModel,
                                modifier = contentModifier
                            )
                            3 -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
