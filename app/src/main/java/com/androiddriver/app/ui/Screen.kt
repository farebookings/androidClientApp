package com.androiddriver.app.ui

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Map : Screen("map")
    data object ScheduleBooking : Screen("schedule_booking")
    data object History : Screen("history")
    data object Profile : Screen("profile")
}
