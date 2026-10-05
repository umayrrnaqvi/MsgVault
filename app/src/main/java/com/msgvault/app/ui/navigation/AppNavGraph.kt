package com.msgvault.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.msgvault.app.ui.screens.AdminScreen
import com.msgvault.app.ui.screens.HomeScreen
import com.msgvault.app.ui.screens.LoginScreen
import com.msgvault.app.ui.screens.PermissionScreen
import com.msgvault.app.ui.screens.SearchScreen
import com.msgvault.app.ui.screens.SettingsScreen
import com.msgvault.app.ui.screens.SignUpScreen
import com.msgvault.app.ui.screens.ThreadScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    hasPermissions: Boolean,
    isLoggedIn: Boolean
) {
    val startDestination = when {
        !isLoggedIn -> NavRoutes.LOGIN
        !hasPermissions -> NavRoutes.PERMISSION
        else -> NavRoutes.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(NavRoutes.SIGNUP)
                },
                onLoginSuccess = {
                    val dest = if (hasPermissions) NavRoutes.HOME else NavRoutes.PERMISSION
                    navController.navigate(dest) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SIGNUP) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    val dest = if (hasPermissions) NavRoutes.HOME else NavRoutes.PERMISSION
                    navController.navigate(dest) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.PERMISSION) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onConversationClick = { address, contactName ->
                    navController.navigate(NavRoutes.thread(address, contactName))
                },
                onSearchClick = {
                    navController.navigate(NavRoutes.SEARCH)
                },
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        composable(
            route = NavRoutes.THREAD,
            arguments = listOf(
                navArgument("address") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType }
            )
        ) {
            ThreadScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAdminClick = {
                    navController.navigate(NavRoutes.ADMIN)
                }
            )
        }

        composable(NavRoutes.ADMIN) {
            AdminScreen(
                onBackClick = { navController.popBackStack() },
                onUserLoggedIn = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onMessageClick = { address, contactName ->
                    navController.navigate(NavRoutes.thread(address, contactName))
                }
            )
        }
    }
}
