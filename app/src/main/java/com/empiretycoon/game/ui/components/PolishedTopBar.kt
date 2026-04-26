package com.empiretycoon.game.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.Company
import com.empiretycoon.game.model.Player
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Midnight
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby
import com.empiretycoon.game.ui.theme.Sapphire

/**
 * Top bar pulida con gradiente, contadores animados y mini-medidores
 * gráficos para reputación y energía.
 *
 * Sustituye al TopInfoBar actual sin cambiar su API.
 */
@Composable
fun PolishedTopBar(
    company: Company,
    player: Player,
    day: Int,
    hour: Int,
    paused: Boolean,
    speed: Int,
    musicOn: Boolean = false,
    onTogglePause: () -> Unit,
    onSpeedCycle: () -> Unit,
    onToggleMusic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isNight = hour < 6 || hour >= 21
    val skyGradient = if (isNight) {
        Brush.horizontalGradient(listOf(Midnight, Ink))
    } else {
        Brush.horizontalGradient(listOf(InkSoft, Color(0xFF1B2940)))
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(skyGradient)
                .border(width = 1.dp, color = InkBorder)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        company.name,
                        fontWeight = FontWeight.Bold,
                        color = Paper,
                        fontSize = 16.sp,
                        modifier = Modifier.shimmer(color = Gold)
                    )
                    Text(
                        company.slogan,
                        style = MaterialTheme.typography.labelSmall,
                        color = Dim,
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mute/unmute música — siempre visible para silenciar al instante
                    androidx.compose.material3.IconButton(
                        onClick = onToggleMusic,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            if (musicOn) "🔊" else "🔇",
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    AnimatedPlayPauseButton(paused = paused, onClick = onTogglePause)
                    Spacer(Modifier.width(6.dp))
                    SpeedButton(speed = speed, onClick = onSpeedCycle)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Ink)
                    .border(1.dp, InkBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoneyChip(cash = company.cash)
                Spacer(Modifier.width(10.dp))
                LevelChip(level = company.level)
                Spacer(Modifier.width(10.dp))
                ReputationArc(value = company.reputation)
                Spacer(Modifier.width(10.dp))
                EnergyHeart(value = player.energy, max = player.maxEnergy)
                Spacer(Modifier.weight(1f))
                DayHourChip(day = day, hour = hour, isNight = isNight)
            }
        }
    }
}

@Composable
private fun MoneyChip(cash: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("💰", fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        AnimatedMoneyCounter(value = cash, color = Gold, fontSize = 13.sp)
    }
}

@Composable
private fun LevelChip(level: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("⭐", fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            "Lv $level",
            color = Emerald,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ReputationArc(value: Int) {
    val frac = (value.coerceIn(0, 100)) / 100f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(24.dp)) {
                drawArc(
                    color = InkBorder,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(2f, 2f),
                    size = Size(size.width - 4f, size.height - 4f),
                    style = Stroke(width = 4f)
                )
                drawArc(
                    color = Sapphire,
                    startAngle = 135f,
                    sweepAngle = 270f * frac,
                    useCenter = false,
                    topLeft = Offset(2f, 2f),
                    size = Size(size.width - 4f, size.height - 4f),
                    style = Stroke(width = 4f)
                )
            }
            Text("🏆", fontSize = 11.sp)
        }
        Spacer(Modifier.width(4.dp))
        AnimatedIntCounter(
            value = value,
            color = Sapphire,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EnergyHeart(value: Int, max: Int) {
    val frac = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(24.dp)) {
                // Caja como "ánfora" simple (rect redondeado), rellena de abajo a arriba
                val w = size.width
                val h = size.height
                val padding = 4f
                val fillH = (h - padding * 2) * frac
                drawRoundRect(
                    color = InkBorder,
                    topLeft = Offset(padding, padding),
                    size = Size(w - padding * 2, h - padding * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Ruby,
                    topLeft = Offset(padding, h - padding - fillH),
                    size = Size(w - padding * 2, fillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
            Text("⚡", fontSize = 11.sp)
        }
        Spacer(Modifier.width(4.dp))
        AnimatedIntCounter(
            value = value,
            color = Ruby,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun DayHourChip(day: Int, hour: Int, isNight: Boolean) {
    val brush = if (isNight) {
        Brush.horizontalGradient(listOf(Midnight, Color(0xFF112C40)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFCD8B0E), Gold))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (isNight) "🌙" else "☀️", fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            "D$day ${"%02d".format(hour)}:00",
            color = if (isNight) Paper else Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AnimatedPlayPauseButton(paused: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(InkBorder)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = paused,
            transitionSpec = {
                (scaleIn(tween(220)) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(180)) + fadeOut(tween(150)))
            },
            label = "playPause"
        ) { isPaused ->
            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPaused) "Reanudar" else "Pausar",
                tint = Gold
            )
        }
    }
}

@Composable
private fun SpeedButton(speed: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(InkBorder)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = speed,
            transitionSpec = {
                (scaleIn(tween(220)) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(180)) + fadeOut(tween(150)))
            },
            label = "speed"
        ) { s ->
            Text(
                "${s}x",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
