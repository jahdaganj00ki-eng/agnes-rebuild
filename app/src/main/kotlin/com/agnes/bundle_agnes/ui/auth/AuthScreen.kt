package com.agnes.bundle_agnes.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnes.bundle_agnes.core.ui.theme.Theme.AgnesTheme
import com.agnes.bundle_agnes.feature.auth.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    val email by remember { mutableStateOf("") }
    val password by remember { mutableStateOf("") }
    val isLoading by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Agnes", fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        errorMessage?.let { msg ->
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
            Text(text = msg, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(24.dp))

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                // In a real implementation, this would call authRepository.loginByEmail
                // For now, just simulate success
                androidx.compose.runtime.LaunchedEffect(key1 = Unit) {
                    try {
                        // Simulate API call
                        kotlin.coroutines.delay(1000)
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage = "Login failed: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).size(height = 48.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                androidx.compose.material.ProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login", fontSize = 16.sp)
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        Text(text = "Demo: any email/password works (MOCK mode)", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}