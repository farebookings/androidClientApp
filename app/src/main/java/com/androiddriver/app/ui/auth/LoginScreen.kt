package com.androiddriver.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androiddriver.app.data.api.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (AuthResponse) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🚖 Android Driver",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isRegister) "Create a new account" else "Sign in to book a ride",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Fields
        if (isRegister) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isRegister) {
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Error
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                 style = MaterialTheme.typography.bodySmall,
                 textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        val response = if (isRegister) {
                            RetrofitClient.api.register(
                                RegisterRequest(name, email, phone, password)
                            )
                        } else {
                            RetrofitClient.api.login(
                                LoginRequest(email, password)
                            )
                        }

                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.token != null) {
                                RetrofitClient.setToken(body.token)
                                onLoginSuccess(body)
                            } else {
                                error = body.error ?: "Unknown error"
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            error = try {
                                val errResp = com.google.gson.Gson().fromJson(errorBody, AuthResponse::class.java)
                                errResp.error ?: "Server error (${response.code()})"
                            } catch (e: Exception) {
                                "Server error (${response.code()})"
                            }
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Connection failed"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (isRegister) "Register" else "Sign In",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isRegister = !isRegister; error = null }) {
            Text(
                if (isRegister) "Already have an account? Sign in"
                else "Don't have an account? Register"
            )
        }
    }
}
