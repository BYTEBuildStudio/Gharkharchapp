package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val dateMillis: Long,
    val note: String = "",
    val paymentMethod: String = "Cash" // Cash, UPI, Card, Net Banking
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val limitAmount: Double,
    val alertThreshold: Double = 0.8 // default alert at 80%
)
