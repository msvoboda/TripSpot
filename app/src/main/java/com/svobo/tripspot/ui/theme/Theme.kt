package com.svobo.tripspot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TripSpotColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF16A34A),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFF8FAFC),
    surface = Color.White
)

@Composable
fun TripSpotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TripSpotColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
