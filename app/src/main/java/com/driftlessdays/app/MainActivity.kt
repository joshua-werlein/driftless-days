package com.driftlessdays.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driftlessdays.app.ui.auth.AuthViewModel
import com.driftlessdays.app.ui.auth.AuthViewModelFactory
import com.driftlessdays.app.ui.auth.SignInScreen
import com.driftlessdays.app.ui.calendar.CalendarScreen
import com.driftlessdays.app.ui.calendar.CalendarViewModelFactory
import com.driftlessdays.app.ui.theme.DriftlessDaysTheme
import android.app.WallpaperManager

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DriftlessDaysTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A2E)
                ) {
                    authViewModel = viewModel(
                        factory = AuthViewModelFactory(application)
                    )
                    val authState by authViewModel.uiState.collectAsState()

                    if (authState.isSignedIn || authState.skipAuth) {
                        val targetDate = intent.getStringExtra("target_date")

                        CalendarScreen(
                            viewModel = viewModel(
                                factory = CalendarViewModelFactory(
                                    application,
                                    accountEmail = authState.email
                                )
                            ),
                            accountEmail = authState.email,
                            targetDate = targetDate,
                            onSetWallpaper = {
                                val intent = Intent(
                                    WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                                ).apply {
                                    putExtra(
                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                        android.content.ComponentName(
                                            this@MainActivity,
                                            com.driftlessdays.app.widget.DriftlessWallpaperService::class.java
                                        )
                                    )
                                }
                                startActivity(intent)
                            },
                            onSignOut = {
                                authViewModel.signOut()
                            }
                        )
                    } else {
                        SignInScreen(
                            onSignInClick = {
                                authViewModel.signIn(this@MainActivity)
                            },
                            onSkipClick = { authViewModel.skipAuth() },
                            isLoading = authState.isLoading,
                            errorMessage = authState.errorMessage
                        )
                    }
                }
            }
        }
    }
}