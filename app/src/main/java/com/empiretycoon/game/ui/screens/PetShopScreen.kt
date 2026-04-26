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
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.world.PetSpecies
import com.empiretycoon.game.world.sprites.drawPet

@Composable
fun PetShopScreen(state: GameState, vm: GameViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var pendingSpecies by remember { mutableStateOf<PetSpecies?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().background(Ink).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Text("🐾 Tienda de mascotas", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(
                    "Tu mascota te seguirá por la ciudad y subirá tu felicidad cada día. " +
                        "No te alejes mucho. Aliméntala con el botón al verla cerca.",
                    color = Dim, fontSize = 12.sp
                )
                if (state.pets.owned.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Tus mascotas (${state.pets.owned.size}):", color = Paper, fontWeight = FontWeight.Bold)
                    state.pets.owned.forEach { pet ->
                        val active = state.pets.activePetId == pet.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Emerald.copy(alpha = 0.2f) else InkBorder.copy(alpha = 0.4f))
                                .clickable { vm.setActivePet(if (active) null else pet.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pet.spec().emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pet.name, fontWeight = FontWeight.SemiBold)
                                Text("Hambre ${pet.hunger}/100 · Felicidad ${pet.happiness}/100",
                                    color = Dim, fontSize = 10.sp)
                            }
                            Text(
                                if (active) "Activa" else "Inactiva",
                                color = if (active) Emerald else Dim,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { vm.feedActivePet() },
                        enabled = state.pets.activePetId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper)
                    ) { Text("🍖 Alimentar mascota activa (5 €)") }
                }
            }
        }
        items(PetSpecies.values().toList()) { species ->
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B1220)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawPet(species, size.width / 2f, size.height / 2f, 100f, 0f)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${species.emoji} ${species.displayName}", fontWeight = FontWeight.Bold)
                        Text("Felicidad/día: +${species.happinessPerDay}", color = Gold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { pendingSpecies = species; nameInput = species.displayName },
                        enabled = state.company.cash >= species.cost,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
                    ) {
                        Text(species.cost.fmtMoney(), fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    pendingSpecies?.let { sp ->
        com.empiretycoon.game.ui.components.CompactDialog(
            title = "Adoptar ${sp.displayName}",
            icon = sp.emoji,
            onDismiss = { pendingSpecies = null },
            footer = {
                TextButton(onClick = { pendingSpecies = null }) { Text("Cancelar", color = Dim) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    vm.buyPet(sp, nameInput)
                    pendingSpecies = null
                }) { Text("Adoptar", color = Gold, fontWeight = FontWeight.Bold) }
            }
        ) {
            Text("¿Cómo le llamarás?", color = Paper, fontSize = 12.sp)
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it.take(20) },
                label = { Text("Nombre", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Coste: ${sp.cost.fmtMoney()}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
