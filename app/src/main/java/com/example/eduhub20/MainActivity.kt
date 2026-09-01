package com.example.eduhub20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.eduhub20.ui.auth.AuthViewModel
import com.example.eduhub20.ui.auth.LoginScreen
import com.example.eduhub20.ui.components.OfflineBanner
import com.example.eduhub20.ui.navigation.EduHubNavHost
import com.example.eduhub20.ui.theme.Eduhub20Theme
import com.example.eduhub20.util.NetworkConnectivityObserver

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.eduhub20.data.local.EduHubLocalStorage.init(applicationContext)
        com.example.eduhub20.data.ai.GeminiConfig.init(applicationContext)
        com.example.eduhub20.ui.theme.ThemeState.init(applicationContext)

        val networkObserver = NetworkConnectivityObserver(applicationContext)

        setContent {
            Eduhub20Theme {
                val uiState by authViewModel.uiState.collectAsState()
                val isOnline by networkObserver.isOnline.collectAsState(initial = true)

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Offline Notice Banner
                        OfflineBanner(isOnline = isOnline)

                        BoxContent(
                            isAuthenticated = uiState.currentUser != null,
                            uiState = uiState,
                            authViewModel = authViewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

//Redo

@androidx.compose.runtime.Composable
private fun BoxContent(
    isAuthenticated: Boolean,
    uiState: com.example.eduhub20.ui.auth.AuthUiState,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isAuthenticated,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AuthTransition",
        modifier = modifier
    ) { authenticated ->
        if (authenticated) {
            EduHubNavHost(
                currentUser = uiState.currentUser,
                onUpdateName = { newName -> authViewModel.updateProfileName(newName) },
                onSignOut = { authViewModel.signOut() }
            )
        } else {
            LoginScreen(
                uiState = uiState,
                viewModel = authViewModel
            )
        }
    }
}
