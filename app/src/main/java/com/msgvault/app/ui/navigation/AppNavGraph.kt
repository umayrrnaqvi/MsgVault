package com.msgvault.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.msgvault.app.ui.screens.AdminConversationsScreen
import com.msgvault.app.ui.screens.AdminMessagesScreen
import com.msgvault.app.ui.screens.AdminScreen
import com.msgvault.app.ui.screens.HomeScreen
import com.msgvault.app.ui.screens.PermissionScreen
import com.msgvault.app.ui.screens.SearchScreen
import com.msgvault.app.ui.screens.SettingsScreen
import com.msgvault.app.ui.screens.ThreadScreen
import com.msgvault.app.ui.viewmodel.AdminViewModel
import com.msgvault.app.ui.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    hasPermissions: Boolean
) {
    val authViewModel: AuthViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        authViewModel.signInAnonymously()
    }

    val startDestination = if (hasPermissions) NavRoutes.HOME else NavRoutes.PERMISSION

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
                onAdminClick = {
                    navController.navigate(NavRoutes.ADMIN)
                }
            )
        }

        composable(NavRoutes.ADMIN) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminScreen(
                onBackClick = {
                    authViewModel.signOutAdmin()
                    authViewModel.signInAnonymously()
                    navController.popBackStack()
                },
                onViewUserMessages = { uid, displayName ->
                    navController.navigate(NavRoutes.adminConversations(uid, displayName))
                },
                viewModel = adminViewModel
            )
        }

        composable(
            route = NavRoutes.ADMIN_CONVERSATIONS,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val displayName = backStackEntry.arguments?.getString("displayName") ?: ""
            val adminViewModel: AdminViewModel = hiltViewModel(
                navController.getBackStackEntry(NavRoutes.ADMIN)
            )
            AdminConversationsScreen(
                uid = uid,
                displayName = displayName,
                onBackClick = { navController.popBackStack() },
                onConversationClick = { address, contactName ->
                    navController.navigate(NavRoutes.adminMessages(uid, address, contactName))
                },
                viewModel = adminViewModel
            )
        }

        composable(
            route = NavRoutes.ADMIN_MESSAGES,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("address") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
            val adminViewModel: AdminViewModel = hiltViewModel(
                navController.getBackStackEntry(NavRoutes.ADMIN)
            )
            AdminMessagesScreen(
                uid = uid,
                address = address,
                contactName = contactName,
                onBackClick = { navController.popBackStack() },
                viewModel = adminViewModel
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
