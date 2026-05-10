package com.empiretycoon.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFFFD166)
val Emerald = Color(0xFF06D6A0)
val Ruby = Color(0xFFEF476F)
val Sapphire = Color(0xFF118AB2)
val Midnight = Color(0xFF073B4C)
val Ink = Color(0xFF0F1724)
val InkSoft = Color(0xFF162133)
val InkBorder = Color(0xFF243042)
val Paper = Color(0xFFF8F9FA)
val Dim = Color(0xFF8899AA)

private val Dark = darkColorScheme(
    primary = Gold,
    onPrimary = Midnight,
    secondary = Emerald,
    onSecondary = Midnight,
    tertiary = Sapphire,
    onTertiary = Paper,
    background = Ink,
    onBackground = Paper,
    surface = InkSoft,
    onSurface = Paper,
    surfaceVariant = InkBorder,
    onSurfaceVariant = Dim,
    error = Ruby,
    onError = Paper
)

private val Light = lightColorScheme(
    primary = Midnight,
    onPrimary = Paper,
    secondary = Emerald,
    onSecondary = Paper,
    tertiary = Sapphire,
    onTertiary = Paper,
    background = Paper,
    onBackground = Midnight,
    surface = Color(0xFFFFFFFF),
    onSurface = Midnight,
    surfaceVariant = Color(0xFFEDEFF2),
    onSurfaceVariant = Color(0xFF5C6B7A),
    error = Ruby,
    onError = Paper
)

@Composable
fun EmpireTheme(
    @Suppress("UNUSED_PARAMETER") dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Decisión de diseño: BalWealth siempre usa modo oscuro estilo tycoon.
    // El parámetro `dark` se mantiene por compatibilidad de signatura; el
    // esquema Light queda definido por si en el futuro se ofrece opción.
    MaterialTheme(
        colorScheme = Dark,
        typography = EmpireTypography,
        content = content
    )
}
