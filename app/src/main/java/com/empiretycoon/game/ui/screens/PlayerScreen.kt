package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun PlayerScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { PlayerHeader(state) }
        item { StatsCard(state, vm) }
        item { ActionsCard(state, vm) }
        item { WalletCard(state, vm) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun PlayerHeader(state: GameState) {
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧑‍💼", fontSize = 48.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.player.name, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
                Text("Nivel ${state.player.level} · CEO de ${state.company.name}",
                    color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                val xpFrac = state.player.xp.toFloat() / state.player.xpForNextLevel()
                ProgressBarWithLabel(xpFrac,
                    label = "${state.player.xp} / ${state.player.xpForNextLevel()} XP",
                    color = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Bar(label = "Energía", value = state.player.energy, max = state.player.maxEnergy,
                color = Ruby, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Bar(label = "Felicidad", value = state.player.happiness, max = 100,
                color = Emerald, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Bar(label: String, value: Int, max: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("$label  $value/$max", color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(InkBorder)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / max.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatsCard(state: GameState, vm: GameViewModel) {
    EmpireCard {
        SectionTitle("Atributos",
            subtitle = "Cada entrenamiento gasta 10 ⚡ y sube 1 punto.")
        Spacer(Modifier.height(6.dp))
        StatRow("Inteligencia", state.player.stats.intelligence, "🧠",
            "Acelera investigación") { vm.train("int") }
        StatRow("Fuerza", state.player.stats.strength, "💪",
            "Trabajos duros") { vm.train("str") }
        StatRow("Carisma", state.player.stats.charisma, "🗣",
            "Mejor precio de venta") { vm.train("cha") }
        StatRow("Suerte", state.player.stats.luck, "🍀",
            "Eventos favorables") { vm.train("luc") }
        StatRow("Destreza", state.player.stats.dexterity, "⚡",
            "Acelera producción") { vm.train("dex") }
    }
}

@Composable
private fun StatRow(name: String, value: Int, emoji: String, hint: String, onTrain: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("$name · $value", fontWeight = FontWeight.SemiBold)
            Text(hint, color = Dim, fontSize = 10.sp)
        }
        Button(
            onClick = onTrain,
            colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("Entrenar", fontSize = 12.sp) }
    }
}

@Composable
private fun ActionsCard(state: GameState, vm: GameViewModel) {
    EmpireCard {
        SectionTitle("Actividades")
        Spacer(Modifier.height(6.dp))
        Row {
            Button(
                onClick = { vm.rest() },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink),
                modifier = Modifier.weight(1f)
            ) { Text("Descansar +30⚡") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { vm.work() },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                modifier = Modifier.weight(1f),
                enabled = state.player.energy >= 15
            ) { Text("Trabajar") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Trabajar te da dinero personal según tu inteligencia y carisma.",
            color = Dim, fontSize = 11.sp
        )
    }
}

@Composable
private fun WalletCard(state: GameState, vm: GameViewModel) {
    var amt by remember { mutableStateOf("100") }
    EmpireCard {
        SectionTitle("Caja personal",
            subtitle = "Transfiere fondos entre tú y la empresa.")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tu cartera", color = Dim, fontSize = 11.sp)
                Text(state.player.cash.fmtMoney(),
                    fontWeight = FontWeight.Bold, color = Gold)
            }
            Column(Modifier.weight(1f)) {
                Text("Caja empresa", color = Dim, fontSize = 11.sp)
                Text(state.company.cash.fmtMoney(),
                    fontWeight = FontWeight.Bold, color = Emerald)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = amt, onValueChange = { amt = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Importe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Button(
                onClick = { vm.personalToCompany(amt.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink)
            ) { Text("→ Empresa") }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = { vm.companyToPersonal(amt.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
            ) { Text("← Personal") }
        }
    }
}
