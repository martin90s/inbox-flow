package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.FakeEmailRepository
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
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    InboxScreen(viewModel = inboxViewModel)
                }
            }
        }
    }
}

