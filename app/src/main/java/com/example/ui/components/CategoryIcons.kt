package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "shopping_basket" -> Icons.Default.ShoppingBasket
            "eco" -> Icons.Default.Eco
            "opacity" -> Icons.Default.Opacity
            "flash_on" -> Icons.Default.FlashOn
            "water" -> Icons.Default.WaterDrop
            "local_fire_department" -> Icons.Default.LocalFireDepartment
            "home" -> Icons.Default.Home
            "directions_car" -> Icons.Default.DirectionsCar
            "phone_android" -> Icons.Default.PhoneAndroid
            "school" -> Icons.Default.School
            "medical_services" -> Icons.Default.MedicalServices
            "person" -> Icons.Default.Person
            "brightness_high" -> Icons.Default.WbSunny
            "account_balance" -> Icons.Default.AccountBalance
            "movie" -> Icons.Default.Movie
            "restaurant" -> Icons.Default.Restaurant
            "local_mall" -> Icons.Default.LocalMall
            "construction" -> Icons.Default.Construction
            "shield" -> Icons.Default.Shield
            "category" -> Icons.Default.Category
            else -> Icons.Default.Category
        }
    }
}
