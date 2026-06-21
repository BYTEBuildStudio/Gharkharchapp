package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.util.FormatUtils
import java.util.Locale

@Composable
fun CategoryDonutChart(
    spendMap: Map<String, Double>,
    categories: List<Category>,
    modifier: Modifier = Modifier
) {
    val totalSpend = spendMap.values.sum()
    
    // Parse category colors
    val colorMap = categories.associate { it.name to Color(android.graphics.Color.parseColor(it.colorHex)) }

    // If no spend, show a nice empty grey ring
    val chartData = if (totalSpend == 0.0) {
        listOf("No Expenses" to 1.0)
    } else {
        spendMap.entries
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(key1 = spendMap) {
        animatedProgress = 1f
    }
    
    val progressAnimation by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "chartReveal"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Donut ring
        Box(
            modifier = Modifier
                .size(150.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val strokeWidth = 18.dp
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                for ((categoryName, value) in chartData) {
                    val sweepAngle = ((value / (if (totalSpend == 0.0) 1.0 else totalSpend)) * 360f).toFloat() * progressAnimation
                    val color = colorMap[categoryName] ?: Color.Gray
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx()),
                        topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)
                    )
                    startAngle += sweepAngle
                }
            }
            
            // Central Info Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Total Spend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatIndianCurrency(totalSpend),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        // 2. Legend list
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            val topEntries = chartData.take(5)
            for ((categoryName, value) in topEntries) {
                val color = colorMap[categoryName] ?: Color.Gray
                val percentage = if (totalSpend == 0.0) 0.0 else (value / totalSpend) * 100
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${FormatUtils.formatIndianCurrency(value)} (${String.format(Locale.getDefault(), "%.1f", percentage)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (chartData.size > 5) {
                Text(
                    text = "+ ${chartData.size - 5} more categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, start = 18.dp)
                )
            }
        }
    }
}

@Composable
fun TrendBarChart(
    weeklySpend: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxSpend = weeklySpend.maxOfOrNull { it.second } ?: 1.0
    val maxCalculatedFactor = if (maxSpend == 0.0) 1.0 else maxSpend

    var animatedFactor by remember { mutableStateOf(0f) }
    LaunchedEffect(key1 = weeklySpend) {
        animatedFactor = 1f
    }

    val barFactorAnimation by animateFloatAsState(
        targetValue = animatedFactor,
        animationSpec = tween(durationMillis = 800),
        label = "barReveal"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for ((dayName, valDouble) in weeklySpend) {
                // Determine raw percentage height
                val heightPercent = (valDouble / maxCalculatedFactor).toFloat() * 0.9f // scale slightly inside container
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (valDouble > 0) {
                        Text(
                            text = "₹${if (valDouble >= 1000) String.format(Locale.getDefault(), "%.1fk", valDouble / 1000) else valDouble.toInt().toString()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(heightPercent * barFactorAnimation + 0.03f) // minimal height
                            .width(18.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (valDouble > 0) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
