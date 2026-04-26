package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.Ending
import com.empiretycoon.game.ui.theme.*

/**
 * Modal triunfal que se muestra cuando el jugador ha alcanzado un final.
 * Cinemático: emoji enorme, gradiente y la frase para compartir.
 */
@Composable
fun EndingDialog(
    ending: Ending,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar el destino", color = Gold,
                    fontWeight = FontWeight.Bold)
            }
        },
        title = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A2640), InkSoft, Color(0xFF1A2640))
                        )
                    )
                    .border(2.dp, Gold, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FINAL DESBLOQUEADO",
                    color = Gold, fontSize = 11.sp,
                    fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(ending.illustrationEmoji, fontSize = 88.sp)
                Spacer(Modifier.height(10.dp))
                Text(ending.title.uppercase(),
                    color = Paper,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Text(ending.narrativeText,
                    color = Paper,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(InkBorder)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Compartir:", color = Dim, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("\"${ending.shareLine}\"",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start)
                    }
                }
            }
        },
        containerColor = Ink,
        shape = RoundedCornerShape(18.dp)
    )
}
