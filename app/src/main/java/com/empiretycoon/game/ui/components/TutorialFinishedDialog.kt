package com.empiretycoon.game.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper

/**
 * Modal de enhorabuena tras completar el tutorial. Cierra simplemente.
 *
 * El callback `onClose` puede usarse para marcar el estado interno como
 * "cerrado" para que el modal no vuelva a salir.
 */
@Composable
fun TutorialFinishedDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
            ) {
                Text("Empezar a jugar", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                "¡Tutorial completado!",
                color = Emerald,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    "Has aprendido los sistemas básicos de Empire Tycoon.",
                    color = Paper,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "A partir de aquí: optimiza tu cadena de producción, diversifica " +
                        "patrimonio y prepárate para sacar tu empresa a bolsa. Cuando " +
                        "necesites refrescar conceptos, pulsa el botón ? en cada pantalla.",
                    color = Paper.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Buena suerte, magnate.",
                    color = Gold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = InkSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Emerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    )
}
