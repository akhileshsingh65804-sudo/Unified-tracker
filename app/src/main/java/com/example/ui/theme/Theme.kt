package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val CosmicDarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryAmber,
    tertiary = AccentRose,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF030712), // Very dark slate/black
    onSecondary = Color(0xFF030712),
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = SurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for premium Cosmic look
    dynamicColor: Boolean = false, // Disable to prevent generic dynamic colors from overwriting design
    content: @Composable () -> Unit,
) {
    val colorScheme = CosmicDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
