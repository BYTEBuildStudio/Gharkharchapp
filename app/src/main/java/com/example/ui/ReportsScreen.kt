package com.example.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.components.CategoryIcons
import com.example.util.FormatUtils
import com.example.viewmodel.ExpenseViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Query states observed from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPayment by viewModel.selectedPaymentMethodFilter.collectAsState()
    val startDate by viewModel.startDateFilter.collectAsState()
    val endDate by viewModel.endDateFilter.collectAsState()

    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var showDeleteDialogFor by remember { mutableStateOf<Expense?>(null) }

    val calendar = Calendar.getInstance()
    
    // Date Pickers callbacks
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val sel = Calendar.getInstance()
            sel.set(Calendar.YEAR, year)
            sel.set(Calendar.MONTH, month)
            sel.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            viewModel.startDateFilter.value = sel.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val sel = Calendar.getInstance()
            sel.set(Calendar.YEAR, year)
            sel.set(Calendar.MONTH, month)
            sel.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            viewModel.endDateFilter.value = sel.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Calculate total spend in filtered results
    val totalFilteredSpend = filteredExpenses.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        
        // Header row with export action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reports & History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Search, filter & export transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Export button
            IconButton(
                onClick = {
                    if (filteredExpenses.isEmpty()) {
                        Toast.makeText(context, "No expense data to export!", Toast.LENGTH_SHORT).show()
                    } else {
                        val fileUri = FormatUtils.exportExpensesToCSV(context, filteredExpenses)
                        if (fileUri != null) {
                            FormatUtils.shareCSVFile(context, fileUri)
                        } else {
                            Toast.makeText(context, "Could not generate report!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("export_csv_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Expenses CSV",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by description or note...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date pickers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start Date picker
            OutlinedButton(
                onClick = { startDatePickerDialog.show() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_start_date"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Start Date", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (startDate != null) FormatUtils.formatDate(startDate!!) else "From: All Time",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // End Date picker
            OutlinedButton(
                onClick = { endDatePickerDialog.show() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_end_date"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "End Date", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (endDate != null) FormatUtils.formatDate(endDate!!) else "To: All Time",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable category filter pills
        Text(
            text = "Category Filter",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.selectedCategoryFilter.value = null },
                    label = { Text("All Categories") },
                    modifier = Modifier.testTag("cat_filter_all")
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category.name,
                    onClick = { viewModel.selectedCategoryFilter.value = category.name },
                    label = { Text(category.name) },
                    modifier = Modifier.testTag("cat_filter_${category.name.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payment chip filters
        val paymentOptions = listOf("Cash", "UPI", "Card", "Net Banking")
        Text(
            text = "Payment Method Filter",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedPayment == null,
                    onClick = { viewModel.selectedPaymentMethodFilter.value = null },
                    label = { Text("All Methods") },
                    modifier = Modifier.testTag("pay_filter_all")
                )
            }
            items(paymentOptions) { payment ->
                FilterChip(
                    selected = selectedPayment == payment,
                    onClick = { viewModel.selectedPaymentMethodFilter.value = payment },
                    label = { Text(payment) },
                    modifier = Modifier.testTag("pay_filter_${payment.lowercase().replace(" ", "_")}")
                )
            }
        }

        // Displaying Filter Summary & "Clear Filters" pill if any filters are active
        val hasFilters = searchQuery.isNotEmpty() || selectedCategory != null || selectedPayment != null || startDate != null || endDate != null
        
        if (hasFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing results (${filteredExpenses.size})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Clear Filters",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedCategoryFilter.value = null
                            viewModel.selectedPaymentMethodFilter.value = null
                            viewModel.startDateFilter.value = null
                            viewModel.endDateFilter.value = null
                        }
                        .testTag("clear_filters_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total spending label in filtered range
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total in Result:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = FormatUtils.formatIndianCurrency(totalFilteredSpend),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Transactions List
        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recorded transactions match filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("reports_transaction_list")
            ) {
                items(filteredExpenses) { expense ->
                    val matchedCategory = categories.firstOrNull { it.name == expense.category }
                    val colorHex = matchedCategory?.colorHex ?: "#9E9E9E"
                    val iconCode = matchedCategory?.iconName ?: "category"
                    val parsedColor = Color(android.graphics.Color.parseColor(colorHex))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_card_${expense.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            // Left Category color segment & icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parsedColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.getIcon(iconCode),
                                    contentDescription = expense.category,
                                    tint = parsedColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Middle Description & Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = FormatUtils.formatIndianCurrency(expense.amount),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                
                                if (expense.note.isNotEmpty()) {
                                    Text(
                                        text = expense.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = FormatUtils.formatDateTime(expense.dateMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    
                                    // Payment Badge
                                    Text(
                                        text = expense.paymentMethod,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Delete Action Trigger
                            IconButton(
                                onClick = { showDeleteDialogFor = expense },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("delete_expense_${expense.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swipe deletion check confirmation Dialog
        if (showDeleteDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialogFor = null },
                title = { Text("Delete Expense Transaction?") },
                text = { Text("Are you sure you want to permanently delete this house expense of ${FormatUtils.formatIndianCurrency(showDeleteDialogFor!!.amount)} recorded under '${showDeleteDialogFor!!.category}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteExpense(showDeleteDialogFor!!)
                            showDeleteDialogFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_btn")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialogFor = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
