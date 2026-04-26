package com.empiretycoon.game.ui.components

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.GameNotification
import com.empiretycoon.game.model.NotificationKind
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby
import com.empiretycoon.game.ui.theme.Sapphire

/**
 * Pila de toasts en la esquina superior derecha. Muestra las últimas
 * notificaciones del juego y las hace desaparecer tras 3 segundos.
 *
 * No modifica la lista subyacente: simplemente esconde los toasts ya vistos.
 * Pasa una sublista pequeña (ej. las últimas 4) para evitar acumulación.
 */
@Composable
fun GameToastHost(
    notifications: List<GameNotification>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4
) {
    val dismissed = remember { mutableStateMapOf<Long, Boolean>() }

    val visible = notifications
        .reversed()
        .filter { dismissed[it.id] != true }
        .take(maxVisible)

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visible.forEach { notif ->
            ToastItem(
                notif = notif,
                onDismiss = { dismissed[notif.id] = true }
            )
        }
    }
}

@Composable
private fun ToastItem(
    notif: GameNotification,
    onDismiss: () -> Unit
) {
    var visible by remember(notif.id) { mutableStateOf(true) }

    LaunchedEffect(notif.id) {
        delay(3_000L)
        visible = false
        delay(350L)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 320)
        ) { full -> full } + fadeIn(tween(220)),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 280)
        ) { full -> full } + fadeOut(tween(220))
    ) {
        val (icon, accent) = iconAndColor(notif.kind)
        Row(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 290.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(InkSoft)
                .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    notif.title,
                    color = Paper,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (notif.message.isNotBlank()) {
                    Text(
                        notif.message,
                        color = Dim,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun iconAndColor(kind: NotificationKind): Pair<String, Color> = when (kind) {
    NotificationKind.SUCCESS -> "✅" to Emerald
    NotificationKind.WARNING -> "⚠️" to Gold
    NotificationKind.ERROR -> "❌" to Ruby
    NotificationKind.EVENT -> "🎲" to Sapphire
    NotificationKind.ECONOMY -> "📈" to Gold
    NotificationKind.INFO -> "ℹ️" to Dim
}
