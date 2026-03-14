package com.prumo.androidclient

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PromoLightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2463B2),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondary = androidx.compose.ui.graphics.Color(0xFF1D4ED8),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiary = androidx.compose.ui.graphics.Color(0xFF0E7490),
    background = androidx.compose.ui.graphics.Color(0xFFF4F7FB),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1E2B3B),
    error = androidx.compose.ui.graphics.Color(0xFFB91C1C)
)

private val PromoDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3D7CCD),
    secondary = androidx.compose.ui.graphics.Color(0xFF7FB0F2),
    tertiary = androidx.compose.ui.graphics.Color(0xFF2FA4C2)
)

@Composable
fun PromoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PromoDarkColors else PromoLightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
