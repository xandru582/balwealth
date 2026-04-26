package com.empiretycoon.game.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Sapphire

/**
 * Modal de bienvenida que se muestra cuando el tutorial está en `WELCOME`.
 *
 * Tres opciones:
 * - Empezar tutorial: avanza al primer paso real (OPEN_EMPIRE).
 * - Saltar: marca el tutorial como saltado.
 * - Ver cómo se juega: abre la guía de ayuda general.
 */
@Composable
fun TutorialIntroDialog(
    playerName: String,
    onStartTutorial: () -> Unit,
    onSkipTutorial: () -> Unit,
    onShowOverview: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* obligatorio elegir */ },
        confirmButton = {
            Button(
                onClick = onStartTutorial,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
            ) {
                Text("Empezar tutorial", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkipTutorial) {
                Text("Saltar", color = Dim)
            }
        },
        title = {
            Column {
                Text(
                    "¡Bienvenido a Empire Tycoon!",
                    color = Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "Hola $playerName. Vas a construir un imperio empresarial desde cero.",
                    color = Paper,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Te enseñaré los sistemas básicos en pocos pasos: edificios, " +
                        "empleados, mercado, investigación, bolsa e inmuebles. Tardarás " +
                        "menos de 5 minutos.",
                    color = Dim,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onShowOverview) {
                        Text(
                            "Ver cómo se juega",
                            color = Sapphire,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = InkSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}
