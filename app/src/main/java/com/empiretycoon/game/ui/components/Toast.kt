package com.empiretycoon.game.ui.components

import kotlinx.coroutines.delay

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Host de notificaciones in-app.
 *
 * Comportamiento (rediseñado a petición del usuario):
 *  - **Una notificación a la vez**: si llega una nueva mientras hay otra
 *    activa, se ENCOLA y aparece cuando la actual termina su ciclo.
 *  - **Sin stack**: nunca se ven 2-3 toasts apilados tapando media pantalla.
 *  - **Translucente** (background con alpha ~0.85, borde 0.5): se ve
 *    parte del menú detrás del toast.
 *  - **No intercepta clicks**: ni el host ni la burbuja tienen `clickable`,
 *    así que los toques pasan a la UI de debajo (Compose default behavior
 *    cuando un componente no consume el gesto).
 *  - Cap de cola en 8 elementos: si el jugador no mira y se acumulan
 *    notificaciones, las viejas se descartan en lugar de retrasar las
 *    nuevas para siempre.
 *  - Tracking de IDs ya mostrados para no re-mostrar al recomponer; el
 *    set está topado a 200 para evitar leak en sesiones largas.
 */
@Composable
fun GameToastHost(
    notifications: List<GameNotification>,
    modifier: Modifier = Modifier,
    displayMs: Long = 2_400L,
    fadeMs: Int = 250
) {
    // IDs ya consumidos por el host para no re-mostrarlos en recomposiciones.
    val shownIds = remember { mutableStateMapOf<Long, Boolean>() }
    // Cola de notifs pendientes de mostrar.
    val queue = remember { mutableStateListOf<GameNotification>() }
    // Notif actualmente visible (null = nada).
    var current by remember { mutableStateOf<GameNotification?>(null) }
    // Flag de salida (true = animación de fade-out en curso).
    var fading by remember { mutableStateOf(false) }

    // Encolar nuevas notificaciones.
    LaunchedEffect(notifications) {
        for (n in notifications.takeLast(20)) {
            if (shownIds[n.id] == true) continue
            if (current?.id == n.id) continue
            if (queue.any { it.id == n.id }) continue
            queue.add(n)
            shownIds[n.id] = true
        }
        // Cap de cola: si se acumula demasiado, drop antiguos.
        while (queue.size > 8) queue.removeAt(0)
        // Cap de shownIds para no leak en sesiones largas.
        if (shownIds.size > 200) {
            val keep = shownIds.keys.sortedDescending().take(150).toSet()
            shownIds.keys.toList().forEach { if (it !in keep) shownIds.remove(it) }
        }
    }

    // Loop de procesamiento: una a una.
    LaunchedEffect(Unit) {
        while (true) {
            if (current == null && queue.isNotEmpty()) {
                fading = false
                current = queue.removeAt(0)
                delay(displayMs)
                fading = true
                delay(fadeMs.toLong())
                current = null
            } else {
                delay(60L)
            }
        }
    }

    // Renderizado: el modifier viene del Root (TopEnd + padding) y el host
    // solo ocupa el espacio del toast actual. NO consume clicks.
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = current != null && !fading,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = fadeMs)
            ) { full -> full / 2 } + fadeIn(tween(fadeMs)),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = fadeMs)
            ) { full -> full / 2 } + fadeOut(tween(fadeMs))
        ) {
            current?.let { ToastBubble(it) }
        }
    }
}

@Composable
private fun ToastBubble(notif: GameNotification) {
    val (icon, accent) = iconAndColor(notif.kind)
    Row(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 290.dp)
            .clip(RoundedCornerShape(10.dp))
            // Translucente — se ve la UI detrás.
            .background(InkSoft.copy(alpha = 0.86f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
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

private fun iconAndColor(kind: NotificationKind): Pair<String, Color> = when (kind) {
    NotificationKind.SUCCESS -> "✅" to Emerald
    NotificationKind.WARNING -> "⚠️" to Gold
    NotificationKind.ERROR -> "❌" to Ruby
    NotificationKind.EVENT -> "🎲" to Sapphire
    NotificationKind.ECONOMY -> "📈" to Gold
    NotificationKind.INFO -> "ℹ️" to Dim
}
