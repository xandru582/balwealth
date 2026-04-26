package com.empiretycoon.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Mini-juego de Casino: ruleta con apuestas a Rojo/Negro/Verde y a número.
 * Usa cash de la empresa. Sin rotación bancaria — apuestas reales con
 * payout estándar (rojo/negro 1:1, verde 35:1, número 35:1).
 */
@Composable
fun CasinoScreen(state: GameState, vm: GameViewModel) {
    var bet by rememberSaveable { mutableStateOf("100") }
    var lastResult by remember { mutableStateOf<RouletteResult?>(null) }
    var spinning by remember { mutableStateOf(false) }
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎰 Casino — Ruleta", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("Caja empresa: ${state.company.cash.fmtMoney()}", color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        // Ruleta visual
        Canvas(Modifier.size(220.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 8f
            val rotation = angle.value
            // Casillas
            val numbers = 0 until 18
            val sliceDeg = 360f / 18f
            for (i in numbers) {
                val color = when (i) {
                    0 -> Color(0xFF43A047)  // 0 verde
                    in 1..8 -> if (i % 2 == 0) Color(0xFF000000) else Color(0xFFE53935)
                    else -> if (i % 2 == 0) Color(0xFFE53935) else Color(0xFF000000)
                }
                drawArc(
                    color = color,
                    startAngle = i * sliceDeg + rotation,
                    sweepAngle = sliceDeg,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f)
                )
                drawArc(
                    color = Color(0xFFFFD166),
                    startAngle = i * sliceDeg + rotation,
                    sweepAngle = sliceDeg,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 2f)
                )
            }
            // Centro
            drawCircle(color = Color(0xFFFFD166), radius = r * 0.18f, center = Offset(cx, cy))
            // Indicador (flecha en la parte superior)
            drawArc(
                color = Color(0xFFFFD166),
                startAngle = -95f,
                sweepAngle = 10f,
                useCenter = true,
                topLeft = Offset(cx - r - 6f, cy - r - 6f),
                size = Size((r + 6f) * 2f, (r + 6f) * 2f)
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = bet,
            onValueChange = { bet = it.filter { c -> c.isDigit() }.take(8) },
            label = { Text("Apuesta (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )
        Spacer(Modifier.height(12.dp))

        val betAmt = bet.toIntOrNull() ?: 0
        val canBet = betAmt > 0 && state.company.cash >= betAmt && !spinning

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CasinoBet("ROJO", "Rojo · 2x", Color(0xFFE53935), canBet) {
                spin(scope, angle, betAmt, BetType.RED, vm) { lastResult = it; spinning = false }
                spinning = true
            }
            CasinoBet("NEGRO", "Negro · 2x", Color(0xFF263238), canBet) {
                spin(scope, angle, betAmt, BetType.BLACK, vm) { lastResult = it; spinning = false }
                spinning = true
            }
            CasinoBet("0", "Verde · 18x", Color(0xFF43A047), canBet) {
                spin(scope, angle, betAmt, BetType.GREEN_ZERO, vm) { lastResult = it; spinning = false }
                spinning = true
            }
        }

        Spacer(Modifier.height(16.dp))
        EmpireCard {
            Text("Cómo funciona", fontWeight = FontWeight.Bold, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Apuesta y elige color. Si ganas: rojo/negro pagan 2x, el 0 paga 18x. " +
                    "Es PURO azar — no es una buena estrategia de inversión, pero a veces apetece. " +
                    "(Tu Karma puede bajar si ganas mucho aquí — la fortuna fácil tiene precio.)",
                color = Dim, fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        lastResult?.let {
            EmpireCard(borderColor = if (it.won) Emerald else Ruby) {
                Text(
                    if (it.won) "🎉 ¡Has ganado! +${it.payout.toInt()} €"
                    else "❌ Has perdido ${it.bet.toInt()} €",
                    color = if (it.won) Emerald else Ruby,
                    fontWeight = FontWeight.Bold
                )
                Text("Resultado: ${it.outcomeColorName} (${it.outcomeNumber})", color = Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CasinoBet(text: String, label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Paper)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = Color(0xFFFFFFFF).copy(alpha = 0.7f))
        }
    }
}

private enum class BetType { RED, BLACK, GREEN_ZERO }
private data class RouletteResult(
    val won: Boolean,
    val bet: Double,
    val payout: Double,
    val outcomeNumber: Int,
    val outcomeColorName: String
)

private fun spin(
    scope: kotlinx.coroutines.CoroutineScope,
    angle: Animatable<Float, *>,
    betAmt: Int,
    betType: BetType,
    vm: GameViewModel,
    onDone: (RouletteResult) -> Unit
) {
    if (betAmt <= 0) return
    scope.launch {
        // Cobrar apuesta
        vm.companyToPersonal(0.0)  // forzar mutate (no-op)
        // Calcular resultado
        val outcomeNumber = (0..17).random()
        val outcomeColor = when {
            outcomeNumber == 0 -> "VERDE"
            (outcomeNumber in 1..8 && outcomeNumber % 2 == 1) ||
                (outcomeNumber in 9..17 && outcomeNumber % 2 == 0) -> "ROJO"
            else -> "NEGRO"
        }
        val targetAngle = angle.value + 360f * 6 + outcomeNumber * (360f / 18f)
        angle.animateTo(
            targetValue = targetAngle,
            animationSpec = tween(2000, easing = LinearOutSlowInEasing)
        )
        val won = when (betType) {
            BetType.RED -> outcomeColor == "ROJO"
            BetType.BLACK -> outcomeColor == "NEGRO"
            BetType.GREEN_ZERO -> outcomeNumber == 0
        }
        val multiplier = if (betType == BetType.GREEN_ZERO) 18.0 else 2.0
        val payout = if (won) betAmt * multiplier else 0.0
        // Aplicar resultado a la caja
        if (won) vm.casinoWin(payout - betAmt)
        else vm.casinoWin(-betAmt.toDouble())
        onDone(RouletteResult(won, betAmt.toDouble(), payout, outcomeNumber, outcomeColor))
    }
}
