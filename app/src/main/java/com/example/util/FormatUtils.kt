package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Expense
import java.io.File
import java.io.FileWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    
    // In India, numbers are formatted using Lakhing convention:
    // e.g. 150000 -> 1,50,000 (group size of 3, then groups of 2)
    fun formatIndianCurrency(amount: Double): String {
        return try {
            val symbols = DecimalFormatSymbols(Locale("en", "IN"))
            // Setting custom symbols to force standard comma and ₹ symbol prefix
            symbols.currencySymbol = "₹"
            
            // DecimalFormat for Lakh: #,##,##,##0.00 or #,##,##,##0 (without decimals if integer)
            val pattern = if (amount % 1.0 == 0.0) "₹#,##,##,##0" else "₹#,##,##,##0.00"
            val df = DecimalFormat(pattern, symbols)
            df.format(amount)
        } catch (e: Exception) {
            // Backup formatter
            "₹" + String.format(Locale.US, "%,.2f", amount)
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN"))
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("en", "IN"))
        return sdf.format(Date(millis))
    }
    
    fun getStartOfMonth(millis: Long): Long {
        val sdf = SimpleDateFormat("yyyyMM", Locale("en", "IN"))
        val monthStr = sdf.format(Date(millis))
        val fullSdf = SimpleDateFormat("yyyyMMdd", Locale("en", "IN"))
        return fullSdf.parse(monthStr + "01")?.time ?: millis
    }

    // Exports a list of expenses to CSV and triggers share sheet
    fun exportExpensesToCSV(context: Context, expenses: List<Expense>): Uri? {
        return try {
            val filename = "GharKharch_Expenses_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            val tempFile = File(context.cacheDir, filename)
            
            val writer = FileWriter(tempFile)
            // CSV Headers
            writer.append("ID,Amount (INR),Category,Date & Time,Payment Method,Notes\n")
            
            for (expense in expenses) {
                val formattedDate = formatDateTime(expense.dateMillis)
                val sanitizedNote = expense.note.replace("\n", " ").replace(",", ";")
                writer.append("${expense.id},")
                writer.append("${expense.amount},")
                writer.append("\"${expense.category}\",")
                writer.append("\"$formattedDate\",")
                writer.append("\"${expense.paymentMethod}\",")
                writer.append("\"$sanitizedNote\"\n")
            }
            writer.flush()
            writer.close()
            
            // Get URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Shares the CSV file via Intent
    fun shareCSVFile(context: Context, fileUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "GharKharch - Household Expenses Export")
            putExtra(Intent.EXTRA_TEXT, "Sending household expense data from GharKharch App.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Expenses Data"))
    }
}
