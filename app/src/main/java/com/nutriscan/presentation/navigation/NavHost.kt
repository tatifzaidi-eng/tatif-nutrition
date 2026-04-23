package com.nutriscan.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.nutriscan.presentation.ui.barcode.BarcodeScannerScreen
import com.nutriscan.presentation.ui.compare.CompareScreen
import com.nutriscan.presentation.ui.detail.FoodDetailScreen
import com.nutriscan.presentation.ui.history.FavoritesScreen
import com.nutriscan.presentation.ui.history.HistoryScreen
import com.nutriscan.presentation.ui.home.HomeScreen

@Composable
fun NutriScanNavHost() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDest = currentBackStack?.destination

    // Masquer la bottom nav sur les écrans plein-écran (détail, scanner)
    val showBottomBar = bottomNavItems.any { item ->
        currentDest?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
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
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.labelRes
                                )
                            },
                            label = { Text(item.labelRes) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) +
                fadeIn(tween(280))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) +
                fadeOut(tween(180))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                fadeIn(tween(280))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                fadeOut(tween(180))
            }
        ) {
            // ── Accueil ──────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onFoodClick = { foodId ->
                        navController.navigate(Screen.Detail.createRoute(foodId))
                    },
                    onBarcodeScan = {
                        navController.navigate(Screen.BarcodeScanner.route)
                    }
                )
            }

            // ── Détail aliment ───────────────────────────────────────────────
            composable(Screen.Detail.route) {
                FoodDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAddToCompare = { foodId ->
                        navController.navigate(Screen.Compare.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── Comparaison ──────────────────────────────────────────────────
            composable(Screen.Compare.route) {
                CompareScreen()
            }

            // ── Historique ───────────────────────────────────────────────────
            composable(Screen.History.route) {
                HistoryScreen(
                    onFoodClick = { foodId ->
                        navController.navigate(Screen.Detail.createRoute(foodId))
                    }
                )
            }

            // ── Favoris ──────────────────────────────────────────────────────
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onFoodClick = { foodId ->
                        navController.navigate(Screen.Detail.createRoute(foodId))
                    }
                )
            }

            // ── Scanner code-barres ──────────────────────────────────────────
            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        // Naviguer vers la fiche du produit trouvé
                        navController.navigate(Screen.Detail.createRoute(barcode)) {
                            popUpTo(Screen.BarcodeScanner.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
