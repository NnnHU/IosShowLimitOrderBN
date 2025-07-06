
package com.pythonn.androidshowlimitorderbn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pythonn.androidshowlimitorderbn.ui.screens.MainScreen
import com.pythonn.androidshowlimitorderbn.ui.screens.SpotOrderBookDetailsScreen
import com.pythonn.androidshowlimitorderbn.ui.screens.FuturesOrderBookDetailsScreen
import com.pythonn.androidshowlimitorderbn.ui.screens.AboutScreen
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object SpotOrderBookDetails : Screen("spot_details/{initialThreshold}") {
        fun createRoute(initialThreshold: Double) = "spot_details/$initialThreshold"
    }
    object FuturesOrderBookDetails : Screen("futures_details/{initialThreshold}") {
        fun createRoute(initialThreshold: Double) = "futures_details/$initialThreshold"
    }
    object About : Screen("about")
}

@Composable
fun AppNavigation(viewModel: MarketDataViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSpotDetails = { navController.navigate(Screen.SpotOrderBookDetails.createRoute(viewModel.currentThreshold.value)) },
                onNavigateToFuturesDetails = { navController.navigate(Screen.FuturesOrderBookDetails.createRoute(viewModel.currentThreshold.value)) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.SpotOrderBookDetails.route) {
            val initialThreshold = it.arguments?.getString("initialThreshold")?.toDoubleOrNull() ?: 0.0
            SpotOrderBookDetailsScreen(viewModel = viewModel, navController = navController, initialThreshold = initialThreshold)
        }
        composable(Screen.FuturesOrderBookDetails.route) {
            val initialThreshold = it.arguments?.getString("initialThreshold")?.toDoubleOrNull() ?: 0.0
            FuturesOrderBookDetailsScreen(viewModel = viewModel, navController = navController, initialThreshold = initialThreshold)
        }
        composable(Screen.About.route) {
            AboutScreen(viewModel = viewModel)
        }
    }
}
