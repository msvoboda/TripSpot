package com.svobo.tripspot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.svobo.tripspot.ui.screens.TripSpotApp
import com.svobo.tripspot.ui.screens.TripViewModel
import com.svobo.tripspot.ui.theme.TripSpotTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TripViewModel by viewModels {
        val repo = (application as TripSpotApplication).repository
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TripViewModel(repo) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TripSpotTheme {
                TripSpotApp(viewModel = viewModel)
            }
        }
    }
}
