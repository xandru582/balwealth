package com.empiretycoon.game.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Catálogo de gradientes pre-fabricados que se reutilizan en cards,
 * botones, fondos y headers. Usar como parámetro `brush =` en
 * `Modifier.background(...)`.
 */
val GoldenSheen: Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8860B),
        Gold,
        Color(0xFFFFE08A),
        Gold,
        Color(0xFFB8860B)
    ),
    start = Offset(0f, 0f),
    end = Offset(800f, 200f)
)

val EmeraldGlow: Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF034F3D),
        Emerald,
        Color(0xFF7BFFD3)
    )
)

val RubyBlaze: Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6B0E25),
        Ruby,
        Color(0xFFFF89A0)
    )
)

val SapphireWave: Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0A4860),
        Sapphire,
        Color(0xFF6BD1E8)
    )
)

val MidnightSky: Brush = Brush.verticalGradient(
    colors = listOf(
        Midnight,
        Color(0xFF0A2030),
        Ink
    )
)

val EconomicBoom: Brush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF014F3C),
        Emerald,
        Gold
    )
)

val EconomicCrash: Brush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF3A1116),
        Ruby,
        Color(0xFFB02240)
    )
)

/**
 * Botón dorado con texto oscuro, para acciones primarias del juego
 * (comprar, ascender, prestigio…).
 */
@Composable
fun goldenButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Gold,
    contentColor = Midnight,
    disabledContainerColor = InkBorder,
    disabledContentColor = Dim
)

/**
 * Botón rubí para acciones destructivas o de venta.
 */
@Composable
fun dangerButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Ruby,
    contentColor = Paper,
    disabledContainerColor = InkBorder,
    disabledContentColor = Dim
)

/**
 * Botón esmeralda neutro/positivo, para confirmar, cobrar, recibir.
 */
@Composable
fun actionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Emerald,
    contentColor = Ink,
    disabledContainerColor = InkBorder,
    disabledContentColor = Dim
)
