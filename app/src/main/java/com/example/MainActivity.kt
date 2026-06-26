package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.db.ConversationSession
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleekPrimary
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ActiveConversationScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.OnboardingScreen
import com.example.viewmodel.ConversableViewModel
import com.example.viewmodel.RoleplayState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.AuthAppState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val authState by authViewModel.appState.collectAsState()

        val viewModel: ConversableViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val roleplayState by viewModel.roleplayState.collectAsState()
        
        var viewedPastSession by remember { mutableStateOf<ConversationSession?>(null) }
        var activeReplaySession by remember { mutableStateOf<ConversationSession?>(null) }

        val username by authViewModel.username.collectAsState()
        val email by authViewModel.userEmail.collectAsState()
        val age by authViewModel.userAge.collectAsState()
        val gender by authViewModel.userGender.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
          ) {
            when (authState) {
              is AuthAppState.Checking -> {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  CircularProgressIndicator(color = SleekPrimary)
                }
              }
              is AuthAppState.AuthScreen -> {
                AuthScreen(
                  viewModel = authViewModel,
                  modifier = Modifier.padding(innerPadding)
                )
              }
              is AuthAppState.ProfileSetupScreen -> {
                ProfileSetupScreen(
                  viewModel = authViewModel,
                  modifier = Modifier.padding(innerPadding)
                )
              }
              is AuthAppState.OnboardingScreen -> {
                OnboardingScreen(
                  onOnboardingComplete = { prefCategory -> authViewModel.completeOnboarding(prefCategory) },
                  modifier = Modifier.padding(innerPadding)
                )
              }
              is AuthAppState.MainAppScreen -> {
                when {
                  activeReplaySession != null -> {
                    com.example.ui.screens.AiReplayScreen(
                      session = activeReplaySession!!,
                      viewModel = viewModel,
                      onBack = { activeReplaySession = null },
                      modifier = Modifier.padding(innerPadding)
                    )
                  }
                  viewedPastSession != null -> {
                    FeedbackScreen(
                      session = viewedPastSession!!,
                      onBackToDashboard = { viewedPastSession = null },
                      onViewReplay = { session -> activeReplaySession = session },
                      modifier = Modifier.padding(innerPadding),
                      viewModel = viewModel
                    )
                  }
                  roleplayState is RoleplayState.FeedbackReady -> {
                    FeedbackScreen(
                      session = (roleplayState as RoleplayState.FeedbackReady).session,
                      onBackToDashboard = { viewModel.resetStateToIdle() },
                      onViewReplay = { session -> activeReplaySession = session },
                      modifier = Modifier.padding(innerPadding),
                      viewModel = viewModel
                    )
                  }
                  roleplayState is RoleplayState.Active || roleplayState is RoleplayState.LoadingFeedback || roleplayState is RoleplayState.Error -> {
                    ActiveConversationScreen(
                      viewModel = viewModel,
                      onBackToDashboard = { viewModel.resetStateToIdle() },
                      modifier = Modifier.padding(innerPadding)
                    )
                  }
                  else -> {
                    DashboardScreen(
                      viewModel = viewModel,
                      onStartScenario = { scenario -> viewModel.startSession(scenario) },
                      onViewPastFeedback = { pastSession -> viewedPastSession = pastSession },
                      onSignOut = { authViewModel.signOutUser() },
                      username = username,
                      email = email,
                      age = age,
                      gender = gender,
                      modifier = Modifier.padding(innerPadding)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
