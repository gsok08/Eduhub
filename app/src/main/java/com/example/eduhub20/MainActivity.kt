package com.example.eduhub20

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
import com.example.eduhub20.ui.auth.AuthViewModel
import com.example.eduhub20.ui.auth.LoginScreen
import com.example.eduhub20.ui.components.OfflineBanner
import com.example.eduhub20.ui.navigation.EduHubNavHost
import com.example.eduhub20.ui.theme.Eduhub20Theme
import com.example.eduhub20.util.NetworkConnectivityObserver

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        // NOTE: All .init() calls are now centralised in EduHubApp.onCreate()
        // so they run before any Activity/Service/ViewModel is created.

        val networkObserver = NetworkConnectivityObserver(applicationContext)

        setContent {
            Eduhub20Theme(darkTheme = false){
                val uiState by authViewModel.uiState.collectAsState()
                val isOnline by networkObserver.isOnline.collectAsState(initial = true)

                // Single Device Login Enforcement: Check if session is still valid.
                // Wrapped in runCatching so a deleted/missing Supabase profile row
                // does not crash the periodic polling loop.
                androidx.compose.runtime.LaunchedEffect(uiState.currentUser?.id) {
                    if (uiState.currentUser != null) {
                        runCatching { authViewModel.verifySingleDeviceSession() }
                        while (true) {
                            kotlinx.coroutines.delay(3000L)
                            runCatching { authViewModel.verifySingleDeviceSession() }
                        }
                    }
                }

                // Also verify immediately whenever app comes into foreground
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner, uiState.currentUser?.id) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && uiState.currentUser != null) {
                            authViewModel.verifySingleDeviceSession()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

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
