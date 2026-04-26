package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.OwnedCar
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.world.Facing
import com.empiretycoon.game.world.sprites.drawPlayerCar

private val CarColors = listOf(
    0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFFFFB300L,
    0xFF8E24AAL, 0xFF263238L, 0xFFFFFFFFL, 0xFFFFEB3BL,
    0xFF00ACC1L, 0xFFE91E63L
)

@Composable
fun GarageScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().background(Ink).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Text("🏠 Mi Garaje", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(
                    "${state.garage.cars.size}/${state.garage.maxSlots} plazas usadas",
                    color = Dim, fontSize = 12.sp
                )
                if (!state.garage.canFitMore) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.expandGarage() },
                        colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ampliar garaje (${(state.garage.maxSlots * 10_000.0).fmtMoney()})") }
                }
            }
        }

        if (state.garage.cars.isEmpty()) {
            item {
                EmpireCard {
                    Text("Tu garaje está vacío.", color = Dim)
                    Spacer(Modifier.height(4.dp))
                    Text("Visita el concesionario para comprar tu primer coche.", color = Dim, fontSize = 12.sp)
                }
            }
        } else {
            items(state.garage.cars, key = { it.instanceId }) { car ->
                GarageCarCard(car, state, vm)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun GarageCarCard(car: OwnedCar, state: GameState, vm: GameViewModel) {
    val model = car.model()
    val isCurrent = state.garage.currentlyDrivingId == car.instanceId
    var showColors by remember(car.instanceId) { mutableStateOf(false) }

    EmpireCard(borderColor = if (isCurrent) Emerald else InkBorder) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B1220)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val tileSize = 70f
                    drawPlayerCar(car, Facing.RIGHT, size.width / 2f, size.height / 2f, tileSize)
                }
                if (isCurrent) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Emerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("CONDUCIENDO", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${model.brand.displayName} ${model.displayName}", fontWeight = FontWeight.Bold)
                    Text("Matrícula: ${car.plateNumber} · ${car.kilometers.toInt()} km",
                        color = Dim, fontSize = 11.sp)
                    Text("⚡${"%.1f".format(model.topSpeed)}× · 🏆${model.prestige}",
                        color = Gold, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { vm.toggleDriving(car.instanceId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrent) Ruby else Emerald,
                        contentColor = if (isCurrent) Paper else Ink
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(if (isCurrent) "Bajar" else "Conducir", fontSize = 12.sp)
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { showColors = !showColors },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("🎨 500€", fontSize = 12.sp) }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = { vm.sellCar(car.instanceId) }
                ) { Text("Vender", color = Ruby, fontSize = 12.sp) }
            }
            if (showColors) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CarColors.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(c.toInt()))
                                .border(1.dp, InkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    vm.repaintCar(car.instanceId, c)
                                    showColors = false
                                }
                        )
                    }
                }
            }
        }
    }
}
