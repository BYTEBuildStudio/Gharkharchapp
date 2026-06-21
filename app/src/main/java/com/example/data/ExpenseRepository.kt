package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val userDao: UserDao
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val activeUser: Flow<User?> = userDao.getActiveUserFlow()

    suspend fun getActiveUser(): User? = userDao.getActiveUser()

    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun logoutAllUsers() = userDao.logoutAllUsers()

    fun getExpensesInDateRange(start: Long, end: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesInDateRange(start, end)
    }

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insert(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.update(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.delete(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteById(id)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insert(budget)
    }

    suspend fun deleteBudgetByCategory(categoryName: String) {
        budgetDao.deleteByCategory(categoryName)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget)
    }

    // Safety initialization: pre-populates categories if database is empty
    suspend fun prepopulateCategoriesIfEmpty() {
        val currentCategories = allCategories.firstOrNull()
        if (currentCategories.isNullOrEmpty()) {
            categoryDao.insertDefaultCategories(ExpenseDatabase.getDefaultCategories())
        }
    }
}
