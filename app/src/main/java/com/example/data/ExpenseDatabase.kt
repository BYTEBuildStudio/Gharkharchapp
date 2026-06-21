package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Expense::class, Category::class, Budget::class, User::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.categoryDao())
                    }
                }
            }

            suspend fun populateDatabase(categoryDao: CategoryDao) {
                categoryDao.insertDefaultCategories(getDefaultCategories())
            }
        }

        fun getDefaultCategories(): List<Category> {
            return listOf(
                Category("Groceries", "shopping_basket", "#4CAF50"),
                Category("Vegetables/Fruits", "eco", "#8BC34A"),
                Category("Milk/Dairy", "opacity", "#03A9F4"),
                Category("Electricity Bill", "flash_on", "#FFC107"),
                Category("Water Bill", "water", "#2196F3"),
                Category("Gas Cylinder (LPG)", "local_fire_department", "#FF5722"),
                Category("Rent", "home", "#9C27B0"),
                Category("Transport/Fuel", "directions_car", "#3F51B5"),
                Category("Mobile/Internet Recharge", "phone_android", "#E91E63"),
                Category("Education/School Fees", "school", "#009688"),
                Category("Medical/Pharmacy", "medical_services", "#F44336"),
                Category("Household Help (maid/cook)", "person", "#795548"),
                Category("Festivals/Pooja", "brightness_high", "#FF9800"),
                Category("EMI/Loan", "account_balance", "#607D8B"),
                Category("Entertainment", "movie", "#673AB7"),
                Category("Eating Out", "restaurant", "#D32F2F"),
                Category("Shopping", "local_mall", "#00BCD4"),
                Category("Maintenance (society/repairs)", "construction", "#78909C"),
                Category("Insurance", "shield", "#4CAF50"),
                Category("Other", "category", "#9E9E9E")
            )
        }
    }
}
