package com.androiddriver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.androiddriver.app.data.api.RetrofitClient
import com.androiddriver.app.data.api.SessionManager
import com.androiddriver.app.ui.Screen
import com.androiddriver.app.ui.auth.LoginScreen
import com.androiddriver.app.ui.booking.ScheduleBookingScreen
import com.androiddriver.app.ui.history.HistoryScreen
import com.androiddriver.app.ui.map.MapScreen
import com.androiddriver.app.ui.profile.ProfileScreen

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5F6368),
    tertiaryContainer = Color(0xFFCEFAD0),
    onTertiaryContainer = Color(0xFF0D652D),
    error = Color(0xFFD93025),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init session manager
        SessionManager.init(applicationContext)

        // Restore token if saved
        SessionManager.getToken()?.let { RetrofitClient.setToken(it) }

        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }
                val navController = rememberNavController()
                var initializing by remember { mutableStateOf(isLoggedIn) }

                val startDestination = if (isLoggedIn) Screen.Map.route else Screen.Login.route

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    // ─── LOGIN ────────────────────────────────────
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = { response, email, password ->
                                response.token?.let { token ->
                                    RetrofitClient.setToken(token)
                                    response.user?.let { user ->
                                        SessionManager.saveLogin(
                                            token = token,
                                            email = email,
                                            password = password,
                                            name = user.name,
                                            phone = user.phone
                                        )
                                    }
                                }
                                isLoggedIn = true
                                navController.navigate(Screen.Map.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onNavigateRegister = {
                                navController.navigate(Screen.Register.route)
                            }
                        )
                    }

                    // ─── REGISTER ────────────────────────────────
                    composable(Screen.Register.route) {
                        LoginScreen(
                            onLoginSuccess = { response, email, password ->
                                response.token?.let { token ->
                                    RetrofitClient.setToken(token)
                                    response.user?.let { user ->
                                        SessionManager.saveLogin(
                                            token = token,
                                            email = email,
                                            password = password,
                                            name = user.name,
                                            phone = user.phone
                                        )
                                    }
                                }
                                isLoggedIn = true
                                navController.navigate(Screen.Map.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onNavigateRegister = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // ─── MAP (IMMEDIATE BOOKING) ─────────────────
                    composable(Screen.Map.route) {
                        MapScreen(
                            onNavigateSchedule = {
                                navController.navigate(Screen.ScheduleBooking.route)
                            },
                            onNavigateHistory = {
                                navController.navigate(Screen.History.route)
                            },
                            onNavigateProfile = {
                                navController.navigate(Screen.Profile.route)
                            },
                            onBookingCreated = { }
                        )
                    }

                    // ─── SCHEDULE BOOKING ───────────────────────
                    composable(Screen.ScheduleBooking.route) {
                        ScheduleBookingScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ─── HISTORY ────────────────────────────────
                    composable(Screen.History.route) {
                        HistoryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ─── PROFILE ────────────────────────────────
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                RetrofitClient.setToken(null)
                                SessionManager.clear()
                                isLoggedIn = false
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
