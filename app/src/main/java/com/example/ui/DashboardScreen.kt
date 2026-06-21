package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryIcons
import com.example.ui.components.TrendBarChart
import com.example.util.FormatUtils
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val scrollState = rememberScrollState()

    // 1. Time boundaries
    val calendar = Calendar.getInstance()
    
    // Today Start
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis

    // Week Start (7 days ago)
    val weekStart = todayStart - (6 * 24 * 3600 * 1000L)

    // Current Month Start
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val currentMonthStart = calendar.timeInMillis

    // Previous Month Start and End
    val prevMonthCal = Calendar.getInstance()
    prevMonthCal.add(Calendar.MONTH, -1)
    prevMonthCal.set(Calendar.DAY_OF_MONTH, 1)
    prevMonthCal.set(Calendar.HOUR_OF_DAY, 0)
    prevMonthCal.set(Calendar.MINUTE, 0)
    prevMonthCal.set(Calendar.SECOND, 0)
    val prevMonthStart = prevMonthCal.timeInMillis
    
    val prevMonthEnd = currentMonthStart - 1

    // 2. Calculations
    val todaySpend = expenses.filter { it.dateMillis >= todayStart }.sumOf { it.amount }
    val weekSpend = expenses.filter { it.dateMillis >= weekStart }.sumOf { it.amount }
    val monthSpend = expenses.filter { it.dateMillis >= currentMonthStart }.sumOf { it.amount }
    
    val prevMonthSpend = expenses.filter { it.dateMillis in prevMonthStart..prevMonthEnd }.sumOf { it.amount }

    // Month comparison Percent
    val monthDiffPercent = if (prevMonthSpend > 0.0) {
        ((monthSpend - prevMonthSpend) / prevMonthSpend) * 100
    } else {
        0.0
    }

    // Category breakdown (for current month)
    val monthExpenses = expenses.filter { it.dateMillis >= currentMonthStart }
    val categorySpendMap = monthExpenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }

    // Top 3 categories spending
    val topCategories = categorySpendMap.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key to it.value }

    // Last 7 days trend
    val weeklyTrendList = remember(expenses) {
        val trend = mutableListOf<Pair<String, Double>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        for (i in 6 downTo 0) {
            val dCal = Calendar.getInstance()
            dCal.add(Calendar.DAY_OF_YEAR, -i)
            
            val dStart = dCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val dEnd = dStart + (24 * 3600 * 1000L) - 1
            
            val label = dayFormat.format(dCal.time)
            val dSpend = expenses.filter { it.dateMillis in dStart..dEnd }.sumOf { it.amount }
            trend.add(label to dSpend)
        }
        trend
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Welcoming Greeting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GharKharch 🏡",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Indian Household Expense Tracker",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (expenses.isEmpty()) {
            // First time/Empty landing card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "New user guide",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Namaste! Welcome to GharKharch",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get started by logging your daily domestic expenses or prepopulate realistic home data instantly to see the dashboard in action!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(
                            onClick = onNavigateToAdd,
                            modifier = Modifier.testTag("onboard_add_expense_btn")
                        ) {
                            Text("Add Expense")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.prepopulateSampleData() },
                            modifier = Modifier.testTag("onboard_load_demo_btn")
                        ) {
                            Text("Load Demo Data")
                        }
                    }
                }
            }
        }

        // Totals Grid Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Today Spend Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(105.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatIndianCurrency(todaySpend),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            // Week Spend Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(105.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "This Week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatIndianCurrency(weekSpend),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
            }

            // Month Spend Card
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .height(105.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Analysis",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = FormatUtils.formatIndianCurrency(monthSpend),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1
                        )
                        
                        // MoM comparison indicator
                        if (prevMonthSpend > 0.0) {
                            val isUp = monthSpend > prevMonthSpend
                            val textValue = String.format(Locale.getDefault(), "%.1f%%", Math.abs(monthDiffPercent))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = if (isUp) "Increase" else "Decrease",
                                    tint = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$textValue MoM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                )
                            }
                        } else {
                            Text(
                                text = "prev month ₹0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart - Category Spend Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Category Spends (This Month)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                CategoryDonutChart(
                    spendMap = categorySpendMap,
                    categories = categories
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Spend Trend Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Daily Trend (Last 7 Days)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                TrendBarChart(weeklySpend = weeklyTrendList)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top 3 Categories Cards
        if (topCategories.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔥 Top Spending Categories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    topCategories.forEachIndexed { index, (catName, sumDouble) ->
                        val matchedCategory = categories.firstOrNull { it.name == catName }
                        val iconCode = matchedCategory?.iconName ?: "category"
                        val colorHex = matchedCategory?.colorHex ?: "#9E9E9E"
                        val parsedColor = Color(android.graphics.Color.parseColor(colorHex))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(parsedColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIcons.getIcon(iconCode),
                                        contentDescription = catName,
                                        tint = parsedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = FormatUtils.formatIndianCurrency(sumDouble),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (index < topCategories.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
