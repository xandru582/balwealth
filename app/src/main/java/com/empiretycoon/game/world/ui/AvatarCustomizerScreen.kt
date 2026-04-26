package com.empiretycoon.game.world.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.world.AvatarLook
import com.empiretycoon.game.world.Facing
import com.empiretycoon.game.world.sprites.drawAvatar

private val SkinTones = listOf(
    0xFFFAD5A5.toInt(), 0xFFE0AC69.toInt(), 0xFFC68642.toInt(),
    0xFF8D5524.toInt(), 0xFFFFE4B5.toInt(), 0xFFD2A679.toInt()
)
private val HairColors = listOf(
    0xFF3E2723.toInt(), 0xFF000000.toInt(), 0xFFFDD835.toInt(),
    0xFF5D4037.toInt(), 0xFFD32F2F.toInt(), 0xFF455A64.toInt()
)
private val ShirtColors = listOf(
    0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(),
    0xFFFFB300.toInt(), 0xFF8E24AA.toInt(), 0xFF00ACC1.toInt(), 0xFFE91E63.toInt()
)
private val PantsColors = listOf(
    0xFF263238.toInt(), 0xFF3E2723.toInt(), 0xFF1A237E.toInt(),
    0xFF212121.toInt(), 0xFF4527A0.toInt()
)

@Composable
fun AvatarCustomizerScreen(
    currentLook: AvatarLook,
    onSave: (AvatarLook) -> Unit,
    onCancel: () -> Unit
) {
    var look by remember { mutableStateOf(currentLook) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1724))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Personaliza tu avatar",
            color = Color(0xFFFFD166),
            fontWeight = FontWeight.Black,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        // Preview
        Box(
            Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF162133))
                .border(2.dp, Color(0xFFFFD166), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(140.dp)) {
                drawAvatar(
                    look = look,
                    facing = Facing.DOWN,
                    walkPhase = 0f,
                    x = (size.width - 16 * 8f) / 2f,
                    y = (size.height - 16 * 8f) / 2f,
                    scale = 8f
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        SwatchRow("Piel", SkinTones, look.skinTone) { look = look.copy(skinTone = it) }
        SwatchRow("Pelo", HairColors, look.hairColor) { look = look.copy(hairColor = it) }
        SwatchRow("Camiseta", ShirtColors, look.shirtColor) { look = look.copy(shirtColor = it) }
        SwatchRow("Pantalón", PantsColors, look.pantsColor) { look = look.copy(pantsColor = it) }

        Spacer(Modifier.height(8.dp))
        Text("Accesorio", color = Color(0xFFCFD8DC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row {
            listOf("Gorra", "Gafas", "Ninguno", "Chistera").forEachIndexed { idx, name ->
                OutlinedButton(
                    onClick = { look = look.copy(accessory = idx) },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = if (look.accessory == idx)
                        ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFD166), contentColor = Color(0xFF0F1724))
                    else
                        ButtonDefaults.outlinedButtonColors()
                ) { Text(name, fontSize = 10.sp) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = { onSave(look) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD166), contentColor = Color(0xFF0F1724))
            ) { Text("Guardar") }
        }
    }
}

@Composable
private fun SwatchRow(label: String, colors: List<Int>, current: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
        Text(label, color = Color(0xFFCFD8DC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row {
            colors.forEach { c ->
                val color = Color(c.toLong() and 0xFFFFFFFFL or 0xFF000000)
                Box(
                    Modifier
                        .size(34.dp)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                        .border(
                            width = if (current == c) 2.dp else 1.dp,
                            color = if (current == c) Color(0xFFFFD166) else Color(0x44FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onPick(c) }
                )
            }
        }
    }
}
