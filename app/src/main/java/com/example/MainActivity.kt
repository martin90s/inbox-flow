package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.FakeEmailRepository
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screen.ComposeScreen
import com.example.ui.screen.InboxScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InboxViewModel
import com.example.ui.viewmodel.InboxViewModelFactory

class MainActivity : ComponentActivity() {

    // Simple, clean constructor-based injection instantiating the fake repository in-memory
    private val emailRepository = FakeEmailRepository()

    // ViewModel creation using custom Provider Factory
    private val inboxViewModel: InboxViewModel by viewModels {
        InboxViewModelFactory(emailRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "inbox") {
                        composable("inbox") {
                            InboxScreen(
                                viewModel = inboxViewModel,
                                onComposeClick = { navController.navigate("compose") }
                            )
                        }
                        composable("compose") {
                            ComposeScreen(
                                onBack = { navController.popBackStack() },
                                onSend = { subject, body ->
                                    inboxViewModel.onSendEmail(subject, body)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

