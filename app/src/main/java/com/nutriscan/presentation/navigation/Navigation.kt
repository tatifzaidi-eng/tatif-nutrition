package com.nutriscan.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.nutriscan.presentation.ui.compare.CompareScreen
import com.nutriscan.presentation.ui.detail.FoodDetailScreen
import com.nutriscan.presentation.ui.history.FavoritesScreen
import com.nutriscan.presentation.ui.history.HistoryScreen
import com.nutriscan.presentation.ui.home.HomeScreen

sealed class Screen(val route: String) {
    data object Home      : Screen("home")
    data object Compare   : Screen("compare")
    data object History   : Screen("history")
    data object Favorites : Screen("favorites")
    data object Detail    : Screen("detail/{foodId}") {
        fun go(id: String) = "detail/$id"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Accueil",    Icons.Filled.Home,        Icons.Outlined.Home),
    BottomNavItem(Screen.Compare,   "Comparer",   Icons.Filled.Balance,     Icons.Outlined.Balance),
    BottomNavItem(Screen.History,   "Historique", Icons.Filled.History,     Icons.Outlined.History),
    BottomNavItem(Screen.Favorites, "Favoris",    Icons.Filled.Favorite,    Icons.Outlined.FavoriteBorder)
)

@Composable
fun NutriScanNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon
                                    else item.unselectedIcon,
                                    item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) +
                fadeIn(tween(260))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) +
                fadeOut(tween(180))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) +
                fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) +
                fadeOut(tween(180))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    contentPadding = padding,
                    onFoodClick = { navController.navigate(Screen.Detail.go(it)) }
                )
            }
            composable(Screen.Compare.route) {
                CompareScreen(contentPadding = padding)
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    contentPadding = padding,
                    onFoodClick = { navController.navigate(Screen.Detail.go(it)) }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    contentPadding = padding,
                    onFoodClick = { navController.navigate(Screen.Detail.go(it)) }
                )
            }
            composable(
                Screen.Detail.route,
                arguments = listOf(navArgument("foodId") { type = NavType.StringType })
            ) {
                FoodDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
