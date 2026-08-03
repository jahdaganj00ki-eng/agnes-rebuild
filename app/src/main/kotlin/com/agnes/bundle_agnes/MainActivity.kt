package com.agnes.bundle_agnes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnes.bundle_agnes.core.di.AppContainer
import com.agnes.bundle_agnes.core.ui.theme.Theme.Agnes
import com.agnes.bundle_agnes.feature.auth.repository.AuthRepository
import com.agnes.bundle_agnes.feature.chat.repository.ChatRepository
import com.agnes.bundle_agnes.ui.chat.ChatScreen
import com.agnes.bundle_agnes.ui.auth.AuthScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val appContainer: AppContainer by lazy {
        AgnesApplication.getAppContainer(this)
    }

    private val authRepository: AuthRepository by lazy {
        appContainer.authRepository
    }

    private val chatRepository: ChatRepository by lazy {
        appContainer.chatRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgnesTheme {
                Surface(
                    modifier = androidx.compose.foundation.layout.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        authRepository = authRepository,
                        chatRepository = chatRepository
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun AppNavHost(
    authRepository: AuthRepository,
    chatRepository: ChatRepository
) {
    val navController = androidx.navigation.compose.rememberNavController()
    val isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn) }

    // Observe auth state
    androidx.compose.runtime.LaunchedEffect(key1 = Unit) {
        authRepository.currentUser.collect { user ->
            isLoggedIn = user != null
        }
    }

    androidx.navigation.compose.NavHost(navController, startDestination = if (isLoggedIn) "chat" else "auth") {
        composable("auth") {
            AuthScreen(
                authRepository = authRepository,
                onLoginSuccess = { navController.navigate("chat") { popUpTo("auth") { inclusive = true } } }
            )
        }
        composable("chat") {
            ChatScreen(
                chatRepository = chatRepository,
                onLogout = {
                    authRepository.logout()
                    navController.navigate("auth") { popUpTo("chat") { inclusive = true } }
                }
            )
        }
    }
}