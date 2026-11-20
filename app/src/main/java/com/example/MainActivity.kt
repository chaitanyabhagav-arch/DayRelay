package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DayRelayApp
import com.example.ui.MainViewModel
import com.example.ui.theme.DayRelayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DayRelayApplication
        setContent {
            DayRelayTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(app, app.repository)
                )
                DayRelayApp(viewModel = vm)
            }
        }
    }
}
