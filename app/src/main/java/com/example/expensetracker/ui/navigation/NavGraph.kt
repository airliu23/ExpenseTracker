package com.example.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.expensetracker.ui.home.HomeScreen
import com.example.expensetracker.ui.settings.SettingsScreen
import com.example.expensetracker.ui.statistics.CategoryDetailScreen
import com.example.expensetracker.ui.statistics.StatisticsScreen
import com.example.expensetracker.ui.statistics.StatisticsViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Statistics : Screen("statistics")
    data object CategoryDetail : Screen("category_detail/{categoryName}/{periodType}/{periodKey}") {
        fun createRoute(categoryName: String, periodType: String, periodKey: String): String =
            "category_detail/$categoryName/$periodType/$periodKey"
    }
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onVoiceInput: () -> Unit = {},
    voiceInputText: String = "",
    onVoiceConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                voiceInputText = voiceInputText,
                onVoiceInput = onVoiceInput,
                onVoiceConsumed = onVoiceConsumed,
                onStatistics = { navController.navigate(Screen.Statistics.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Statistics.route) {
            val viewModel: StatisticsViewModel = hiltViewModel()
            val periodType by viewModel.periodType.collectAsState()
            val selectedMonth by viewModel.selectedMonth.collectAsState()
            val selectedYear by viewModel.selectedYear.collectAsState()

            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCategoryClick = { categoryName ->
                    val pType = periodType.name
                    val pKey = if (periodType == com.example.expensetracker.ui.statistics.PeriodType.MONTH)
                        selectedMonth else selectedYear
                    navController.navigate(
                        Screen.CategoryDetail.createRoute(categoryName, pType, pKey)
                    )
                }
            )
        }

        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("periodType") { type = NavType.StringType },
                navArgument("periodKey") { type = NavType.StringType }
            )
        ) {
            CategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
