package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.UnifiedRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tracker.TrackerScreen
import com.example.ui.tracker.TrackerViewModel
import com.example.ui.tracker.TrackerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database and DAOs
        val database = AppDatabase.getDatabase(this)
        val repository = UnifiedRepository(
            emailDao = database.emailDao(),
            orderDao = database.orderDao(),
            ticketDao = database.ticketDao()
        )
        
        // Create ViewModel through Factory
        val viewModel: TrackerViewModel by viewModels {
            TrackerViewModelFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrackerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
