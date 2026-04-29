package com.androiddriver.app.data.api

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS_NAME = "androiddriver_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PASSWORD = "user_password"
    private const val KEY_NAME = "user_name"
    private const val KEY_PHONE = "user_phone"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLogin(token: String, email: String, password: String, name: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            putString(KEY_NAME, name)
            putString(KEY_PHONE, phone)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)
    fun getName(): String? = prefs.getString(KEY_NAME, null)
    fun getPhone(): String? = prefs.getString(KEY_PHONE, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
