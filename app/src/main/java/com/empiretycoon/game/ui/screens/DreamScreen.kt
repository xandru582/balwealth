package com.empiretycoon.game.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Paper
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mundo de sueños: pantalla onírica que aparece cuando el jugador descansa.
 * Atmósfera surrealista (gradiente animado + estrellas) con un diálogo
 * de "tu yo del pasado / futuro" dando hints contextuales según karma.
 */
@Composable
fun DreamScreen(state: GameState, vm: GameViewModel, onWake: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "dream")
    val phase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "p"
    )
    val karma = state.storyline.karma
    val tone = when {
        karma >= 50 -> DreamTone.SAINT
        karma >= 10 -> DreamTone.HOPEFUL
        karma <= -50 -> DreamTone.NIGHTMARE
        karma <= -10 -> DreamTone.HEAVY
        else -> DreamTone.NEUTRAL
    }
    val message = remember(tone) { messageFor(tone, state) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        when (tone) {
                            DreamTone.SAINT -> Color(0xFF1A237E)
                            DreamTone.HOPEFUL -> Color(0xFF283593)
                            DreamTone.NEUTRAL -> Color(0xFF1B1B3A)
                            DreamTone.HEAVY -> Color(0xFF311B92)
                            DreamTone.NIGHTMARE -> Color(0xFF1A0033)
                        },
                        Color.Black
                    )
                )
            )
    ) {
        // Estrellas animadas
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 80) {
                val px: Float = ((i * 137) % 1000).toFloat() / 1000f * size.width
                val py: Float = ((i * 251) % 1000).toFloat() / 1000f * size.height
                val twinkle: Float = (sin((phase * 6.28f + i * 0.7f).toDouble()) * 0.5 + 0.5).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f + twinkle * 0.5f),
                    radius = 1.5f + twinkle * 1.5f,
                    center = Offset(px, py)
                )
            }
            // Pulso central onírico
            for (i in 1..3) {
                val rPulse: Float = (size.minDimension / 4f) * (1f + phase * i * 0.4f)
                drawCircle(
                    color = Color(0xFFFFD166).copy(alpha = (0.3f - i * 0.08f).coerceAtLeast(0f)),
                    radius = rPulse,
                    center = Offset(size.width / 2f, size.height * 0.35f)
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text("💤 SUEÑO LÚCIDO", color = Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text("Tono: ${tone.label}", color = Dim, fontSize = 12.sp)
            Spacer(Modifier.height(40.dp))

            Text(
                message,
                color = Paper,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(60.dp))

            Button(
                onClick = onWake,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0F1724))
            ) {
                Text("Despertar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Cuando despiertas, tu energía se restaura y tu mente está más clara. " +
                    "+30 ⚡ +5 😊 +XP",
                color = Dim,
                fontSize = 11.sp
            )
        }
    }
}

private enum class DreamTone(val label: String) {
    SAINT("Etéreo"),
    HOPEFUL("Esperanzador"),
    NEUTRAL("Onírico"),
    HEAVY("Pesado"),
    NIGHTMARE("Pesadilla")
}

private fun messageFor(tone: DreamTone, state: GameState): String = when (tone) {
    DreamTone.SAINT -> listOf(
        "Una voz sin cuerpo te susurra: \"Las decisiones que tomas con generosidad se multiplican en otros.\"",
        "Te ves a ti mismo desde fuera, sonriendo a alguien que necesita ayuda. La luz emana de ti.",
        "Sueñas con una ciudad en la que no hay pobres. Te das cuenta de que la has construido tú."
    ).random()
    DreamTone.HOPEFUL -> listOf(
        "Tu yo del futuro te saluda desde un balcón con vistas al mar: \"Sigue así. Falta poco.\"",
        "Una niña te tiende un dibujo en el sueño. Es de tu empresa, pero con árboles.",
        "Caminas por una calle desconocida. La gente te sonríe sin reconocerte. Te sienta bien."
    ).random()
    DreamTone.NEUTRAL -> listOf(
        "Estás en un ascensor que sube y baja a la vez. Hay un espejo. No te reconoces.",
        "Sueñas que eres una hoja de cálculo flotando en el espacio.",
        "Una vieja te mira y te dice: \"Es hora de elegir lado.\""
    ).random()
    DreamTone.HEAVY -> listOf(
        "Caminas por tu fábrica vacía. Los empleados son sombras que te dan la espalda.",
        "Cuentas dinero pero los billetes se convierten en hojas secas en tus manos.",
        "Te sientes pequeño en un edificio enorme que es tuyo. Suena un teléfono que no encuentras."
    ).random()
    DreamTone.NIGHTMARE -> listOf(
        "Una multitud te persigue por las calles. Reconoces a varios. Trabajaron para ti.",
        "Estás solo en una mansión. Las paredes susurran nombres de gente a la que arruinaste.",
        "Tu yo joven te mira con desprecio: \"¿En esto te has convertido?\""
    ).random()
}
