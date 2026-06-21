package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Budget
import com.example.data.Category
import com.example.ui.components.CategoryIcons
import com.example.util.FormatUtils
import com.example.viewmodel.ExpenseViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.allCategories.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val isDarkModeState by viewModel.isDarkMode.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val googleAccessToken by viewModel.googleAccessToken.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()

    var showTokenInput by remember { mutableStateOf(false) }
    var tokenInputText by remember { mutableStateOf(googleAccessToken) }

    // 1. Calculations: Month spends per category
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val currentMonthStart = cal.timeInMillis

    val categoryMonthSpend = remember(expenses) {
        expenses.filter { it.dateMillis >= currentMonthStart }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // Modal state for Setting Budget
    var showBudgetDialogFor by remember { mutableStateOf<Category?>(null) }
    var budgetInputText by remember { mutableStateOf("") }

    // Modal state for Custom Category Creation
    var showAddCategorySection by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var customCategoryColor by remember { mutableStateOf("#9C27B0") } // purple default
    var customCategoryIcon by remember { mutableStateOf("category") }

    val colorPresets = listOf("#E91E63", "#9C27B0", "#2196F3", "#4CAF50", "#FFC107", "#FF5722", "#00BCD4", "#795548")
    val iconsPreset = listOf("shopping_basket", "eco", "opacity", "flash_on", "directions_car", "school", "medical_services", "restaurant", "local_mall", "home", "category")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page title
        item {
            Column {
                Text(
                    text = "Preferences & Budgets",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configure limits, themes, and customized categories for your home",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Toggles Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Theme Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkModeState) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Dark mode"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Switch(
                            checked = isDarkModeState,
                            onCheckedChange = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // Active User & Google Sheets Synchronization Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Google Sheets Vault Backup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            activeUser?.let {
                                Text(
                                    text = "Connected: ${it.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Sheets Connected State",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    activeUser?.let { user ->
                        Text(
                            text = "Authenticated Family Email Log:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "Spreadsheet Storage Registry ID:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = user.spreadsheetId.ifEmpty { "Pending initial backup sync" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (user.spreadsheetId.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Action rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncWithGoogleSheets() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("sheets_sync_now_btn")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync now")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }

                            OutlinedButton(
                                onClick = { viewModel.logOut() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                                    .testTag("app_logout_btn")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Sign out")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Out")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Access Token details (for OAuth customization)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTokenInput = !showTokenInput }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configure custom OAuth token",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (showTokenInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand token trigger",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showTokenInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tokenInputText,
                            onValueChange = {
                                tokenInputText = it
                                viewModel.setGoogleAccessToken(it)
                            },
                            label = { Text("Google OAuth Token") },
                            placeholder = { Text("Paste Bearer OAuth Access Token") },
                            textStyle = MaterialTheme.typography.labelSmall,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sheets_custom_token_input")
                        )
                        Text(
                            text = "Leaves blank to automatically run our secure sandbox cloud emulator sync.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Real-time Cloud Operations Console Log Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), // Terminal Dark
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Sheets Live Terminal",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4CAF50) // Monospace Green
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (syncStatus) {
                                        "Syncing" -> Color.Yellow
                                        "Success" -> Color.Green
                                        "Error" -> Color.Red
                                        else -> Color.Gray
                                    }
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF111111), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        if (syncLogs.isEmpty()) {
                            Text(
                                text = "Terminal Idle. Ready for transaction backup sync...",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(syncLogs.reversed()) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("Success", ignoreCase = true)) Color.Green else if (log.contains("Error", ignoreCase = true)) Color.Red else Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Database backups / Demo presets card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prepopulate sample household expenses to see the charts and reports in full action under demo states.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { viewModel.prepopulateSampleData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_load_demo_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Prepopulate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Prepopulate Realistic Demo Data")
                    }
                }
            }
        }

        // Custom category creation Card trigger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Custom Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showAddCategorySection = !showAddCategorySection },
                            modifier = Modifier.testTag("toggle_custom_cat_card")
                        ) {
                            Icon(
                                imageVector = if (showAddCategorySection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle"
                            )
                        }
                    }

                    if (showAddCategorySection) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = customCategoryName,
                            onValueChange = { customCategoryName = it },
                            label = { Text("Category Name") },
                            placeholder = { Text("e.g. Milk, Pet Foods, Subscriptions...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_cat_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render color presets row
                        Text(text = "Choose Category Color", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(colorPresets) { hexStr ->
                                val rgbCol = Color(android.graphics.Color.parseColor(hexStr))
                                val isSelectedColor = customCategoryColor == hexStr
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(rgbCol)
                                        .border(
                                            width = if (isSelectedColor) 3.dp else 0.dp,
                                            color = if (isSelectedColor) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { customCategoryColor = hexStr }
                                        .testTag("color_preset_$hexStr")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render icon selection presets row
                        Text(text = "Choose Category Icon", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(iconsPreset) { iconName ->
                                val isSelectedIcon = customCategoryIcon == iconName
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelectedIcon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { customCategoryIcon = iconName }
                                        .padding(8.dp)
                                        .testTag("icon_preset_$iconName"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIcons.getIcon(iconName),
                                        contentDescription = iconName,
                                        tint = if (isSelectedIcon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (customCategoryName.trim().isNotEmpty()) {
                                    viewModel.addCategory(
                                        name = customCategoryName.trim(),
                                        iconName = customCategoryIcon,
                                        colorHex = customCategoryColor
                                    )
                                    // Reset inputs
                                    customCategoryName = ""
                                    showAddCategorySection = false
                                }
                            },
                            enabled = customCategoryName.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_custom_cat_btn")
                        ) {
                            Text("Create Category")
                        }
                    }
                }
            }
        }

        // Category budget progress list
        item {
            Text(
                text = "Monthly Budgets per Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(categories) { category ->
            val budget = budgets.firstOrNull { it.category == category.name }
            val currentSpend = categoryMonthSpend[category.name] ?: 0.0
            
            val parsedColor = Color(android.graphics.Color.parseColor(category.colorHex))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_card_${category.name.lowercase().replace(" ", "_")}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Category Details row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parsedColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.getIcon(category.iconName),
                                    contentDescription = category.name,
                                    tint = parsedColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Spent this month: ${FormatUtils.formatIndianCurrency(currentSpend)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Configure Budget Button
                        Button(
                            onClick = {
                                showBudgetDialogFor = category
                                budgetInputText = budget?.limitAmount?.toInt()?.toString() ?: ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (budget == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (budget == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("set_budget_btn_${category.name.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = if (budget == null) "Set Limit" else "Edit Limit",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Simple Visual progress indicators
                    if (budget != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val ratio = if (budget.limitAmount > 0) (currentSpend / budget.limitAmount).toFloat() else 0f
                        val progressRatio = ratio.coerceAtMost(1.0f)
                        
                        // Select colors nicely matching limit threshold warnings
                        val progressColor = when {
                            ratio > 1.0f -> MaterialTheme.colorScheme.error // 100%+ (Exceeded)
                            ratio >= budget.alertThreshold -> Color(0xFFFBC02D) // 80%+ (Nearing Alert)
                            else -> Color(0xFF4CAF50) // Safe green
                        }

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${Math.round(ratio * 100)}% of monthly budget utilized",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ratio > 1.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Limit: ${FormatUtils.formatIndianCurrency(budget.limitAmount)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Remove budget limit button
                        TextButton(
                            onClick = { viewModel.deleteBudget(category.name) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                                .height(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Remove limit",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    } else {
                        // Empty budget limit card
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No spending budget limit set for ${category.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Dialogue configuration for Setting Category limit budget
    if (showBudgetDialogFor != null) {
        val categoryName = showBudgetDialogFor!!.name
        
        AlertDialog(
            onDismissRequest = { showBudgetDialogFor = null },
            title = {
                Text(
                    text = "Set Monthly Limit for $categoryName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter maximum domestic budget limit (₹) for $categoryName. You will be alerted when spending nears or exceeds this limit.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { input ->
                            // Numeric inputs
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                budgetInputText = input
                            }
                        },
                        label = { Text("Budget Limit (₹)") },
                        placeholder = { Text("e.g. 5000, 10000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_limit_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetInputText.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            viewModel.setBudget(categoryName, limit)
                            showBudgetDialogFor = null
                        }
                    },
                    modifier = Modifier.testTag("submit_budget_btn"),
                    enabled = budgetInputText.isNotEmpty() && (budgetInputText.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialogFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
