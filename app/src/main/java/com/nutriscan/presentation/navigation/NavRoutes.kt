package com.nutriscan.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

// ─────────────────────────────────────────────────────────────────────────────
//  Routes de navigation
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Detail : Screen("detail/{foodId}") {
        fun createRoute(foodId: String) = "detail/$foodId"
    }
    data object Compare : Screen("compare")
    data object History : Screen("history")
    data object Favorites : Screen("favorites")
    data object BarcodeScanner : Screen("barcode_scanner")
    data object Settings : Screen("settings")
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom Navigation Items
// ─────────────────────────────────────────────────────────────────────────────

sealed class BottomNavItem(
    val screen: Screen,
    val labelRes: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        Screen.Home,
        "Accueil",
        Icons.Filled.Home,
        Icons.Outlined.Home
    )
    data object Compare : BottomNavItem(
        Screen.Compare,
        "Comparer",
        Icons.Filled.Balance,
        Icons.Outlined.Balance
    )
    data object History : BottomNavItem(
        Screen.History,
        "Historique",
        Icons.Filled.History,
        Icons.Outlined.History
    )
    data object Favorites : BottomNavItem(
        Screen.Favorites,
        "Favoris",
        Icons.Filled.Favorite,
        Icons.Outlined.FavoriteBorder
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Compare,
    BottomNavItem.History,
    BottomNavItem.Favorites
)
