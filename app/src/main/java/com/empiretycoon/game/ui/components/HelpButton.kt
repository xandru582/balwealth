package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.HelpContent
import com.empiretycoon.game.model.HelpTopic
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import kotlinx.coroutines.launch

/**
 * Pequeño botón "?" que abre una bottom sheet con la explicación del tema.
 *
 * Pensado para colocarse en la esquina superior-derecha de cada pantalla
 * del juego. La sheet se reutiliza entre temas: una sola instancia, varios
 * `topic` distintos según el lugar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpButton(
    topic: HelpTopic,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(
        modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(InkSoft)
            .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                "?",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = InkSoft
        ) {
            HelpSheetContent(
                topic = topic,
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        open = false
                    }
                }
            )
        }
    }
}

/**
 * Contenido renderizado dentro de la BottomSheet de ayuda. Expuesto por si
 * otra pantalla quiere embeberlo sin modal.
 */
@Composable
fun HelpSheetContent(topic: HelpTopic, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(topic.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                topic.displayName,
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Ink)
                .border(1.dp, InkBorder, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Text(
                HelpContent.get(topic),
                color = Paper,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Cerrar", color = Dim)
            }
        }
    }
}
