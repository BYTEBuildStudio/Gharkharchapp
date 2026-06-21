package com.example.util

import com.squareup.moshi.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// 1. Google Account User Info Response model
data class GoogleUserInfo(
    val email: String,
    val name: String,
    val picture: String? = null
)

// 2. Google Sheets API Request and Response Models
data class CreateSpreadsheetRequest(
    val properties: SpreadsheetProperties
)

data class SpreadsheetProperties(
    val title: String
)

data class CreateSpreadsheetResponse(
    val spreadsheetId: String,
    val spreadsheetUrl: String
)

data class AppendValuesRequest(
    val majorDimension: String = "ROWS",
    val values: List<List<Any>>
)

data class AppendValuesResponse(
    val spreadsheetId: String,
    val tableRange: String?,
    val updates: UpdateDetails?
)

data class UpdateDetails(
    val spreadSheetId: String?,
    val updatedRange: String?,
    val updatedRows: Int?,
    val updatedColumns: Int?,
    val updatedCells: Int?
)

data class BatchUpdateResponse(
    val spreadsheetId: String
)

data class BatchUpdateSpreadsheetRequest(
    val requests: List<SheetRequest>
)

data class SheetRequest(
    val addSheet: AddSheetRequest? = null
)

data class AddSheetRequest(
    val properties: SheetProperties
)

data class SheetProperties(
    val title: String
)

// 3. Retrofit service interfaces
interface GoogleAuthApi {
    @GET("oauth2/v3/userinfo")
    suspend fun getUserInfo(
        @Header("Authorization") bearerToken: String
    ): GoogleUserInfo
}

interface GoogleSheetsApi {
    @POST("v4/spreadsheets")
    suspend fun createSpreadsheet(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateSpreadsheetRequest
    ): CreateSpreadsheetResponse

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "USER_ENTERED",
        @Body request: AppendValuesRequest
    ): AppendValuesResponse

    @POST("v4/spreadsheets/{spreadsheetId}:batchUpdate")
    suspend fun batchUpdate(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Body request: BatchUpdateSpreadsheetRequest
    ): BatchUpdateResponse
}

// 4. API Clients provider
object GoogleSheetsClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val googleAuthApi: GoogleAuthApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(GoogleAuthApi::class.java)

    val googleSheetsApi: GoogleSheetsApi = Retrofit.Builder()
        .baseUrl("https://sheets.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(GoogleSheetsApi::class.java)
}
