package com.sdn.semambung.kaih.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet ala versi web (Tailwind: sky / amber / slate)
private val SkemaWarna = lightColorScheme(
    primary = Color(0xFF0369A1),            // sky-700
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),   // sky-100
    onPrimaryContainer = Color(0xFF0C4A6E), // sky-900
    secondary = Color(0xFFF59E0B),          // amber-500
    onSecondary = Color(0xFF451A03),        // amber-900
    background = Color(0xFFF0F9FF),         // sky-50
    onBackground = Color(0xFF334155),       // slate-700
    surface = Color.White,
    onSurface = Color(0xFF334155),
    error = Color(0xFFE11D48)               // rose-600
)

@Composable
fun KaihTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkemaWarna,
        content = content
    )
}
