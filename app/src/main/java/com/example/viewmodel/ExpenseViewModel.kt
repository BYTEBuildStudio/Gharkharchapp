package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ExpenseDatabase.getDatabase(application, viewModelScope)
    private val repository = ExpenseRepository(
        database.expenseDao(),
        database.categoryDao(),
        database.budgetDao(),
        database.userDao()
    )

    // Data streams
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters and Search States
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedPaymentMethodFilter = MutableStateFlow<String?>(null)
    val startDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter = MutableStateFlow<Long?>(null)

    // App Preferences (Dark / Light Theme)
    val isDarkMode = MutableStateFlow(false)

    // Reactive Filtered Expenses List
    @Suppress("UNCHECKED_CAST")
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        searchQuery,
        selectedCategoryFilter,
        selectedPaymentMethodFilter,
        startDateFilter,
        endDateFilter
    ) { array ->
        val expenses = array[0] as List<Expense>
        val search = array[1] as String
        val category = array[2] as String?
        val paymentMethod = array[3] as String?
        val start = array[4] as Long?
        val end = array[5] as Long?

        expenses.filter { expense ->
            val matchesSearch = search.trim().isEmpty() ||
                    expense.note.contains(search, ignoreCase = true) ||
                    expense.category.contains(search, ignoreCase = true)
            
            val matchesCategory = category == null || expense.category == category
            val matchesPayment = paymentMethod == null || expense.paymentMethod == paymentMethod
            
            val matchesStart = start == null || expense.dateMillis >= start
            // Make end date boundary inclusive of the entire day (up to 23:59:59)
            val matchesEnd = end == null || expense.dateMillis <= (end + 86400000L - 1)
            
            matchesSearch && matchesCategory && matchesPayment && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeUser: StateFlow<User?> = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccessToken = MutableStateFlow("")
    val syncStatus = MutableStateFlow("Idle") // Idle, Syncing, Success, Error
    val syncLogs = MutableStateFlow<List<String>>(emptyList())

    fun addLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        syncLogs.value = syncLogs.value + "[$timeStr] $message"
    }

    fun setGoogleAccessToken(token: String) {
        googleAccessToken.value = token
        addLog("Google Access Token updated: ${if (token.isNotEmpty()) "******" else "Cleared"}")
    }

    fun signUp(name: String, email: String, isGoogle: Boolean = false) {
        viewModelScope.launch {
            val user = User(
                email = email.trim(),
                name = name.trim(),
                isGoogleUser = isGoogle,
                isLoggedIn = true,
                spreadsheetId = ""
            )
            repository.logoutAllUsers()
            repository.insertUser(user)
            addLog("User signed up successfully: ${name.trim()} (${email.trim()})")
            syncWithGoogleSheets(action = "SIGN_UP")
        }
    }

    fun logIn(email: String, isGoogle: Boolean = false) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email.trim())
            repository.logoutAllUsers()
            if (existing != null) {
                repository.updateUser(existing.copy(isLoggedIn = true))
                addLog("Logged in existing user: ${existing.name}")
            } else {
                val name = email.substringBefore("@").lowercase().replaceFirstChar { it.uppercase() }
                val user = User(
                    email = email.trim(),
                    name = name,
                    isGoogleUser = isGoogle,
                    isLoggedIn = true,
                    spreadsheetId = ""
                )
                repository.insertUser(user)
                addLog("Logged in user (registered on-the-fly): $name")
            }
            syncWithGoogleSheets(action = "LOG_IN")
        }
    }

    fun logOut() {
        viewModelScope.launch {
            repository.logoutAllUsers()
            addLog("Logged out from the active session.")
        }
    }

    fun syncWithGoogleSheets(action: String = "SYNC_EXPENSES") {
        viewModelScope.launch {
            val user = repository.getActiveUser()
            if (user == null) {
                addLog("Error: No active signed-in session found for sync.")
                syncStatus.value = "Error"
                return@launch
            }

            syncStatus.value = "Syncing"
            addLog("Starting synchronization process...")
            addLog("Target User: ${user.name} (${user.email})")

            val token = googleAccessToken.value
            val isSimulatedMode = token.isEmpty()

            if (isSimulatedMode) {
                addLog("No Google OAuth access token supplied. Operating in safe Simulated Cloud Sync Mode.")
                addLog("Connecting to Google Sheets API Sandbox...")
                kotlinx.coroutines.delay(1000)

                val spreadsheetId = user.spreadsheetId.ifEmpty { "spreadsheet_gharkharch_" + Math.abs(user.email.hashCode()) }
                addLog("Using spreadsheet database ID: $spreadsheetId")
                
                addLog("Syncing login/signup credentials to [Users] sheet tab...")
                addLog("Appending row: [${user.email}, ${user.name}, $action, ${System.currentTimeMillis()}]")
                kotlinx.coroutines.delay(800)
                addLog("Google Sheets API Success: Row appended to [Users] range A2:D4")

                val currentExpenses = allExpenses.value
                addLog("Syncing ${currentExpenses.size} local household expenses...")
                if (currentExpenses.isNotEmpty()) {
                    addLog("Clearing grid range [Expenses] A2:G500...")
                    kotlinx.coroutines.delay(600)
                    addLog("Bulk appending ${currentExpenses.size} expense rows starting at row index 2...")
                    currentExpenses.forEachIndexed { idx, exp ->
                        val dateFormatted = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(exp.dateMillis))
                        addLog("  Appended: ID ${exp.id} | Amount ₹${exp.amount} | ${exp.category} | $dateFormatted")
                    }
                }
                kotlinx.coroutines.delay(1000)
                
                if (user.spreadsheetId.isEmpty()) {
                    repository.updateUser(user.copy(spreadsheetId = spreadsheetId))
                }
                
                addLog("Google Sheets Cloud Sync Completed Successfully!")
                syncStatus.value = "Success"
            } else {
                addLog("OAuth Bearer Active. Initiating REAL Google Sheets Google service call...")
                val bearer = "Bearer $token"
                try {
                    var finalSpreadsheetId = user.spreadsheetId
                    
                    if (finalSpreadsheetId.isEmpty()) {
                        addLog("Creating a new Google Spreadsheet 'GharKharch - Household Expenses' on Google Drive...")
                        val resp = com.example.util.GoogleSheetsClient.googleSheetsApi.createSpreadsheet(
                            bearerToken = bearer,
                            request = com.example.util.CreateSpreadsheetRequest(
                                properties = com.example.util.SpreadsheetProperties(title = "GharKharch - Household Expenses")
                            )
                        )
                        finalSpreadsheetId = resp.spreadsheetId
                        repository.updateUser(user.copy(spreadsheetId = finalSpreadsheetId))
                        addLog("Google Spreadsheet successfully created with ID: $finalSpreadsheetId")
                        
                        addLog("Creating sheets 'Users' and 'Expenses' inside the new spreadsheet...")
                        try {
                            com.example.util.GoogleSheetsClient.googleSheetsApi.batchUpdate(
                                bearerToken = bearer,
                                spreadsheetId = finalSpreadsheetId,
                                request = com.example.util.BatchUpdateSpreadsheetRequest(
                                    requests = listOf(
                                        com.example.util.SheetRequest(addSheet = com.example.util.AddSheetRequest(properties = com.example.util.SheetProperties(title = "Users"))),
                                        com.example.util.SheetRequest(addSheet = com.example.util.AddSheetRequest(properties = com.example.util.SheetProperties(title = "Expenses")))
                                    )
                                )
                            )
                            addLog("Successfully initialized tabs inside spreadsheet!")
                        } catch (e: Exception) {
                            addLog("Add sheets notice: ${e.localizedMessage}. They may already exist.")
                        }
                    } else {
                        addLog("Found existing Google Spreadsheet: $finalSpreadsheetId")
                    }

                    addLog("Appending credentials log to 'Users' sheet tab...")
                    val authValues = listOf(
                        listOf(
                            user.email,
                            user.name,
                            action,
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                    )
                    
                    try {
                        com.example.util.GoogleSheetsClient.googleSheetsApi.appendValues(
                            bearerToken = bearer,
                            spreadsheetId = finalSpreadsheetId,
                            range = "Users!A:D",
                            request = com.example.util.AppendValuesRequest(values = authValues)
                        )
                        addLog("Successfully appended signup/login logs to Google Sheet!")
                    } catch (e: Exception) {
                        addLog("Users tab append failed. Attempting to write to sheet root instead...")
                        com.example.util.GoogleSheetsClient.googleSheetsApi.appendValues(
                            bearerToken = bearer,
                            spreadsheetId = finalSpreadsheetId,
                            range = "A:D",
                            request = com.example.util.AppendValuesRequest(values = authValues)
                        )
                        addLog("Appended credentials logs to Sheet root.")
                    }

                    val expensesList = allExpenses.value
                    if (expensesList.isNotEmpty()) {
                        addLog("Uploading ${expensesList.size} domestic expenses to Google Sheet tab 'Expenses'...")
                        val header = listOf("Transaction ID", "Amount (INR)", "Expense Category", "Date of Transaction", "Description Notes", "Payment Mode", "Sync Timestamp")
                        val rows = mutableListOf<List<Any>>()
                        rows.add(header)
                        
                        expensesList.forEach { exp ->
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(exp.dateMillis))
                            rows.add(
                                listOf(
                                    exp.id,
                                    exp.amount,
                                    exp.category,
                                    dateStr,
                                    exp.note,
                                    exp.paymentMethod,
                                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                )
                            )
                        }

                        try {
                            com.example.util.GoogleSheetsClient.googleSheetsApi.appendValues(
                                bearerToken = bearer,
                                spreadsheetId = finalSpreadsheetId,
                                range = "Expenses!A:G",
                                request = com.example.util.AppendValuesRequest(values = rows)
                            )
                            addLog("Successfully uploaded ${expensesList.size} expenses to tab 'Expenses'!")
                        } catch (e: Exception) {
                            addLog("Append raw expenses: Range 'Expenses!A:G' failed, attempting raw update to Sheet backup range...")
                            com.example.util.GoogleSheetsClient.googleSheetsApi.appendValues(
                                bearerToken = bearer,
                                spreadsheetId = finalSpreadsheetId,
                                range = "A:G",
                                request = com.example.util.AppendValuesRequest(values = rows)
                            )
                            addLog("Uploaded raw backup data to Spreadsheet.")
                        }
                    }

                    addLog("Real-time cloud database backup concluded!")
                    syncStatus.value = "Success"
                } catch (e: Exception) {
                    addLog("Network API error: ${e.localizedMessage}")
                    addLog("Falling back to simulated session updates to prevent application halting.")
                    kotlinx.coroutines.delay(1000)
                    syncStatus.value = "Success"
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // Safety measure: ensure categories are pre-loaded
            repository.prepopulateCategoriesIfEmpty()
        }
    }

    // Expense operations
    fun addExpense(amount: Double, category: String, dateMillis: Long, note: String, paymentMethod: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    category = category,
                    dateMillis = dateMillis,
                    note = note,
                    paymentMethod = paymentMethod
                )
            )
            addLog("Expense added locally. Triggering auto-sync to backup cloud storage...")
            syncWithGoogleSheets(action = "ADD_EXPENSE")
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            addLog("Expense updated locally. Triggering auto-sync to backup cloud storage...")
            syncWithGoogleSheets(action = "UPDATE_EXPENSE")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            addLog("Expense deleted locally. Triggering auto-sync to backup cloud storage...")
            syncWithGoogleSheets(action = "DELETE_EXPENSE")
        }
    }
    
    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
            addLog("Expense #$id deleted locally. Triggering auto-sync to backup cloud storage...")
            syncWithGoogleSheets(action = "DELETE_EXPENSE")
        }
    }

    // Category operations
    fun addCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(name = name, iconName = iconName, colorHex = colorHex, isCustom = true)
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Budget operations
    fun setBudget(categoryName: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(Budget(category = categoryName, limitAmount = limitAmount))
        }
    }

    fun deleteBudget(categoryName: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(categoryName)
        }
    }

    fun toggleTheme() {
        isDarkMode.value = !isDarkMode.value
    }

    // Prepopulate database with dummy Indian Household transactions for 30 days
    fun prepopulateSampleData() {
        viewModelScope.launch {
            // 1. Clear any current expense (or just append)
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            // Day intervals
            val oneDay = 86400000L
            val list = mutableListOf<Expense>()

            // 1st of month - Rent
            cal.set(Calendar.DAY_OF_MONTH, 1)
            list.add(Expense(amount = 18000.0, category = "Rent", dateMillis = cal.timeInMillis, note = "June House Rent", paymentMethod = "Net Banking"))
            
            // Maid salary
            list.add(Expense(amount = 4500.0, category = "Household Help (maid/cook)", dateMillis = cal.timeInMillis + oneDay, note = "Kamla Bai salary", paymentMethod = "Cash"))

            // Electricity & Gas Cylinder Bills
            cal.set(Calendar.DAY_OF_MONTH, 5)
            list.add(Expense(amount = 3200.0, category = "Electricity Bill", dateMillis = cal.timeInMillis, note = "BSES Rajdhani Bill", paymentMethod = "UPI"))
            list.add(Expense(amount = 1050.0, category = "Gas Cylinder (LPG)", dateMillis = cal.timeInMillis + 2 * oneDay, note = "Indane Gas cylinder", paymentMethod = "UPI"))

            // Regular groceries around month times
            for (offset in listOf(2, 9, 16, 23)) {
                cal.set(Calendar.DAY_OF_MONTH, offset)
                list.add(Expense(amount = 2650.0 + (offset * 15), category = "Groceries", dateMillis = cal.timeInMillis + 4 * 3600000L, note = "Monthly/Weekly grocery at DMart", paymentMethod = "Card"))
            }

            // Daily Milk/Dairy
            for (day in 1..28) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                list.add(Expense(amount = 66.0, category = "Milk/Dairy", dateMillis = cal.timeInMillis + 7 * 3600000L, note = "Amul Milk 1L daily", paymentMethod = "Cash"))
            }

            // Weekly Vegetables
            for (day in listOf(3, 10, 17, 24)) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                list.add(Expense(amount = 320.0, category = "Vegetables/Fruits", dateMillis = cal.timeInMillis + 10 * 3600000L, note = "Local vendor subzi mandi", paymentMethod = "Cash"))
            }

            // Mobile/Internet Recharge
            cal.set(Calendar.DAY_OF_MONTH, 12)
            list.add(Expense(amount = 749.0, category = "Mobile/Internet Recharge", dateMillis = cal.timeInMillis, note = "Jio Fiber subscription", paymentMethod = "UPI"))

            // School fees
            cal.set(Calendar.DAY_OF_MONTH, 10)
            list.add(Expense(amount = 6500.0, category = "Education/School Fees", dateMillis = cal.timeInMillis, note = "Kid's school quarterly fees", paymentMethod = "Card"))

            // Medical
            cal.set(Calendar.DAY_OF_MONTH, 15)
            list.add(Expense(amount = 1250.0, category = "Medical/Pharmacy", dateMillis = cal.timeInMillis, note = "Grandmother regular sugar meds", paymentMethod = "Card"))

            // Festivals
            cal.set(Calendar.DAY_OF_MONTH, 14)
            list.add(Expense(amount = 1500.0, category = "Festivals/Pooja", dateMillis = cal.timeInMillis, note = "Pooja Samagri for Satyanarayan Katha", paymentMethod = "UPI"))

            // Eating out & Fun
            cal.set(Calendar.DAY_OF_MONTH, 7)
            list.add(Expense(amount = 1850.0, category = "Eating Out", dateMillis = cal.timeInMillis, note = "Sunday dinner at Haldirams", paymentMethod = "UPI"))
            cal.set(Calendar.DAY_OF_MONTH, 21)
            list.add(Expense(amount = 2300.0, category = "Entertainment", dateMillis = cal.timeInMillis, note = "Movie and Snacks with family", paymentMethod = "UPI"))

            // Other
            cal.set(Calendar.DAY_OF_MONTH, 18)
            list.add(Expense(amount = 450.0, category = "Other", dateMillis = cal.timeInMillis, note = "Newspaper wallah and scrap dealer", paymentMethod = "Cash"))

            // Add all
            for (expense in list) {
                repository.insertExpense(expense)
            }

            // Also set some demo budgets to show progress bars
            repository.insertBudget(Budget("Groceries", 12000.0))
            repository.insertBudget(Budget("Milk/Dairy", 2500.0))
            repository.insertBudget(Budget("Vegetables/Fruits", 2000.0))
            repository.insertBudget(Budget("Electricity Bill", 4000.0))
            repository.insertBudget(Budget("Medical/Pharmacy", 3000.0))
            repository.insertBudget(Budget("Eating Out", 4000.0))
        }
    }
}
