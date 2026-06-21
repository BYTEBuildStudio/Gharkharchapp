package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.ui.components.CategoryIcons
import com.example.util.FormatUtils
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Preset categories safely loaded
    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.firstOrNull { it.name == "Groceries" } ?: categories.firstOrNull()
        }
    }

    val scrollState = rememberScrollState()

    // DatePicker Dialog setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(Calendar.YEAR, year)
            selectedCal.set(Calendar.MONTH, month)
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dateMillis = selectedCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Budget alert check logic on-the-fly
    val categoryBudget = budgets.firstOrNull { it.category == selectedCategory?.name }
    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0

    val existingCatSpendThisMonth = remember(expenses, selectedCategory, dateMillis) {
        if (selectedCategory == null) return@remember 0.0
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis

        expenses.filter { 
            it.category == selectedCategory?.name && it.dateMillis >= startOfMonth 
        }.sumOf { it.amount }
    }

    val budgetAlertState = remember(enteredAmount, existingCatSpendThisMonth, categoryBudget) {
        if (categoryBudget == null || enteredAmount <= 0.0) return@remember null
        val totalProjSpend = existingCatSpendThisMonth + enteredAmount
        val budgetVal = categoryBudget.limitAmount
        
        when {
            totalProjSpend > budgetVal -> {
                val excess = totalProjSpend - budgetVal
                "Exceeded" to "Warning: Saving this will EXCEED your monthly budget of ${FormatUtils.formatIndianCurrency(budgetVal)} for ${selectedCategory?.name} by ${FormatUtils.formatIndianCurrency(excess)}!"
            }
            totalProjSpend >= (budgetVal * categoryBudget.alertThreshold) -> {
                val percentUsed = (totalProjSpend / budgetVal) * 100
                "Nearing" to "Notice: Saving this will use ${String.format(Locale.getDefault(), "%.1f", percentUsed)}% of your monthly budget limit for ${selectedCategory?.name}."
            }
            else -> null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Log Household Expense",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Amount Box field (Large size)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Amount (₹)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                TextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                            amountText = input
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    placeholder = {
                        Text(
                            text = "0.00",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note Card field
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Notes / Descriptions (optional)") },
            placeholder = { Text("e.g. Weekly milk bill, DMart, electrician fees...") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction Date",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Modifier.let { MaterialTheme.colorScheme.onSurface }
            )
            
            Button(
                onClick = { datePickerDialog.show() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("date_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val formattedStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
                Text(text = formattedStr, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method row
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val paymentOptions = listOf("Cash", "UPI", "Card", "Net Banking")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paymentOptions.forEach { method ->
                val isSelected = selectedPaymentMethod == method
                val tintBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val tintText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tintBg)
                        .clickable { selectedPaymentMethod = method }
                        .padding(vertical = 10.dp)
                        .testTag("payment_${method.lowercase().replace(" ", "_")}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = method,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = tintText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories select grid
        Text(
            text = "Expense Category",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Render beautiful category scroll grid
        Box(
            modifier = Modifier
                .height(280.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory?.name == category.name
                    val parsedColor = Color(android.graphics.Color.parseColor(category.colorHex))
                    
                    val bgCol = if (isSelected) parsedColor else Color.Transparent
                    val itemContentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .clickable { selectedCategory = category }
                            .padding(8.dp)
                            .testTag("cat_tile_${category.name.lowercase().replace(" ", "_")}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else parsedColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIcons.getIcon(category.iconName),
                                contentDescription = category.name,
                                tint = if (isSelected) Color.White else parsedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            color = itemContentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic budget warnings/notices block
        if (budgetAlertState != null) {
            val (status, textMsg) = budgetAlertState
            val containerBgColor = if (status == "Exceeded") MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF9C4)
            val textCol = if (status == "Exceeded") MaterialTheme.colorScheme.onErrorContainer else Color(0xFF3E2723)
            val iconTint = if (status == "Exceeded") MaterialTheme.colorScheme.error else Color(0xFFE65100)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = containerBgColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = textMsg,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = textCol
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save transaction trigger
        Button(
            onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val cat = selectedCategory?.name ?: "Other"
                if (amt > 0) {
                    viewModel.addExpense(
                        amount = amt,
                        category = cat,
                        dateMillis = dateMillis,
                        note = noteText.trim(),
                        paymentMethod = selectedPaymentMethod
                    )
                    onSuccess()
                }
            },
            enabled = amountText.isNotEmpty() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_expense_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Log Expense",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
