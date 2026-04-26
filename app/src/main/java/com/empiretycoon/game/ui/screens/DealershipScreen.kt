package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.world.Facing
import com.empiretycoon.game.world.sprites.drawPlayerCar

/**
 * Concesionario: navega coches por marca, ve stats, compra. La preview
 * dibuja el coche con `drawPlayerCar` real (mismo render que en mundo).
 */
@Composable
fun DealershipScreen(state: GameState, vm: GameViewModel) {
    var selectedBrand by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CarModel?>(null) }

    Column(Modifier.fillMaxSize().background(Ink)) {
        Text(
            "🚗 Concesionario BalWealth",
            color = Gold,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(16.dp)
        )

        // Filtros por marca
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedBrand == null,
                onClick = { selectedBrand = null },
                label = { Text("Todas") }
            )
            CarBrand.values().forEach { b ->
                FilterChip(
                    selected = selectedBrand == b.name,
                    onClick = { selectedBrand = b.name },
                    label = { Text(b.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold,
                        selectedLabelColor = Ink
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val list = if (selectedBrand == null) CarCatalog.all
                else CarCatalog.byBrand(CarBrand.valueOf(selectedBrand!!))
            items(list, key = { it.id }) { model ->
                CarRow(model, state, vm) { preview = model }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    preview?.let { model ->
        CarPreviewDialog(model = model, state = state, vm = vm, onDismiss = { preview = null })
    }
}

@Composable
private fun CarRow(model: CarModel, state: GameState, vm: GameViewModel, onPreview: () -> Unit) {
    val owned = state.garage.cars.any { it.modelId == model.id }
    val canAfford = state.company.cash >= model.price
    EmpireCard(borderColor = if (owned) Emerald else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini preview canvas
            Box(
                Modifier
                    .size(72.dp, 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B1220))
                    .border(1.dp, InkBorder, RoundedCornerShape(8.dp))
                    .clickable { onPreview() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val tileSize = 40f
                    val owned = OwnedCar(
                        instanceId = "preview",
                        modelId = model.id,
                        plateNumber = ""
                    )
                    drawPlayerCar(owned, Facing.RIGHT, size.width / 2f, size.height / 2f, tileSize)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${model.brand.displayName} ${model.displayName}", fontWeight = FontWeight.Bold)
                Text(model.body.name + if (model.isElectric) " · ⚡" else "" + if (model.isClassic) " · 🕰" else "",
                    color = Gold, fontSize = 10.sp)
                Text(model.description, color = Dim, fontSize = 11.sp, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text("⚡${"%.1f".format(model.topSpeed)}× · 🎯${"%.1f".format(model.handling)}× · 🏆${model.prestige}",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(model.price.fmtMoney(), fontWeight = FontWeight.Bold,
                    color = if (canAfford && !owned) Gold else Ruby)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { vm.buyCar(model.id) },
                    enabled = canAfford && !owned && state.garage.canFitMore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold, contentColor = Ink,
                        disabledContainerColor = InkBorder, disabledContentColor = Dim
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (owned) "Tienes" else "Comprar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CarPreviewDialog(model: CarModel, state: GameState, vm: GameViewModel, onDismiss: () -> Unit) {
    com.empiretycoon.game.ui.components.CompactDialog(
        title = "${model.brand.displayName} ${model.displayName}",
        icon = model.emoji,
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Dim) }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = { vm.buyCar(model.id); onDismiss() },
                enabled = state.company.cash >= model.price && state.garage.canFitMore
            ) {
                Text(model.price.fmtMoney(),
                    color = if (state.company.cash >= model.price) Gold else Dim,
                    fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B1220)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val tileSize = 90f
                    val owned = OwnedCar(instanceId = "preview", modelId = model.id, plateNumber = "")
                    drawPlayerCar(owned, Facing.RIGHT, size.width / 2f, size.height / 2f, tileSize)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(model.description, color = Paper, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            StatLine("Velocidad", model.topSpeed, "× a pie")
            StatLine("Manejo", model.handling, "× normal")
            StatLine("Prestigio", model.prestige.toFloat(), "/100")
            StatLine("Felicidad", model.happinessBoost.toFloat(), "😊")
            if (model.isElectric) Text("⚡ Eléctrico — sin emisiones", color = Color(0xFF03A9F4), fontSize = 11.sp)
            if (model.isClassic) Text("🕰 Clásico de coleccionista", color = Gold, fontSize = 11.sp)
            if (model.hasSpoiler) Text("🏁 Con alerón aerodinámico", color = Ruby, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatLine(label: String, value: Float, suffix: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, color = Dim, modifier = Modifier.weight(1f), fontSize = 11.sp)
        Text("${"%.1f".format(value)} $suffix", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

