package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper

/**
 * Diálogo compacto y NO invasivo. Reemplazo de [androidx.compose.material3.AlertDialog]:
 *
 *  - Anclado al centro pero limitado al 88% del ancho y 78% del alto disponible.
 *  - Padding interno reducido (12.dp en lugar de 24.dp por defecto).
 *  - Header pequeño con emoji opcional y título a la izquierda.
 *  - Footer opcional para botones.
 *  - Cierra al tocar fuera por defecto (configurable).
 */
@Composable
fun CompactDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxW = (maxWidth * 0.92f).coerceAtMost(420.dp)
            val maxH = maxHeight * 0.78f
            Surface(
                modifier = modifier
                    .widthIn(min = 240.dp, max = maxW)
                    .heightIn(max = maxH)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, InkBorder, RoundedCornerShape(14.dp)),
                color = InkSoft,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Text(icon, fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            title,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Body (scrollable si excede)
                    Column(Modifier.weight(1f, fill = false)) {
                        content()
                    }

                    // Footer
                    if (footer != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            content = footer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Versión aún más reducida para mensajes informativos: sin scroll, máximo 280dp ancho.
 */
@Composable
fun MiniInfoDialog(
    title: String,
    body: String,
    icon: String? = null,
    onDismiss: () -> Unit,
    actionLabel: String = "OK"
) {
    CompactDialog(
        title = title,
        icon = icon,
        onDismiss = onDismiss,
        modifier = Modifier.widthIn(max = 280.dp),
        footer = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(actionLabel, color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Text(body, color = Paper, fontSize = 12.sp)
    }
}
