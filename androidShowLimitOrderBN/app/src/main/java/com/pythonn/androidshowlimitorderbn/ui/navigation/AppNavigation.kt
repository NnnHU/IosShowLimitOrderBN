
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
    object SpotOrderBookDetails : Screen("spot_details")
    object FuturesOrderBookDetails : Screen("futures_details")
    object About : Screen("about")
}

@Composable
fun AppNavigation(viewModel: MarketDataViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSpotDetails = { navController.navigate(Screen.SpotOrderBookDetails.route) },
                onNavigateToFuturesDetails = { navController.navigate(Screen.FuturesOrderBookDetails.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.SpotOrderBookDetails.route) {
            SpotOrderBookDetailsScreen(viewModel = viewModel)
        }
        composable(Screen.FuturesOrderBookDetails.route) {
            FuturesOrderBookDetailsScreen(viewModel = viewModel)
        }
        composable(Screen.About.route) {
            AboutScreen(viewModel = viewModel)
        }
    }
}
