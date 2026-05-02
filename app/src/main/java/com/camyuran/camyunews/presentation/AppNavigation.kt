package com.camyuran.camyunews.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.camyuran.camyunews.presentation.detail.DetailScreen
import com.camyuran.camyunews.presentation.favorites.FavoritesScreen
import com.camyuran.camyunews.presentation.home.HomeScreen
import com.camyuran.camyunews.presentation.search.SearchScreen
import com.camyuran.camyunews.presentation.settings.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "ホーム")
    data object Favorites : Screen("favorites", "お気に入り")
    data object Search : Screen("search", "検索")
    data object Settings : Screen("settings", "設定")
    data object Detail : Screen("detail/{articleId}", "詳細") {
        fun buildRoute(articleId: String) = "detail/$articleId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(Screen.Home, Screen.Favorites, Screen.Search, Screen.Settings)
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Home -> Icons.Default.Home
                                        Screen.Favorites -> Icons.Default.Bookmarks
                                        Screen.Search -> Icons.Default.Search
                                        Screen.Settings -> Icons.Default.Settings
                                        else -> Icons.Default.Home
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onArticleClick = { id ->
                    navController.navigate(Screen.Detail.buildRoute(id))
                })
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(onArticleClick = { id ->
                    navController.navigate(Screen.Detail.buildRoute(id))
                })
            }
            composable(Screen.Search.route) {
                SearchScreen(onArticleClick = { id ->
                    navController.navigate(Screen.Detail.buildRoute(id))
                })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Detail.route) { backStack ->
                val articleId = backStack.arguments?.getString("articleId") ?: return@composable
                DetailScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
