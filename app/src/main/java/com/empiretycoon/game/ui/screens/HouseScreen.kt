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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.empiretycoon.game.world.FurnitureKind
import com.empiretycoon.game.world.PlacedFurniture

@Composable
fun HouseScreen(state: GameState, vm: GameViewModel) {
    var pickedKind by rememberSaveable { mutableStateOf<String?>(null) }
    var familyTab by rememberSaveable { mutableStateOf(0) }

    LazyColumn(
        Modifier.fillMaxSize().background(Ink).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Text("🏠 Mi casa", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(
                    "Toca un mueble del catálogo y luego una celda libre para colocarlo. " +
                        "Toca un mueble ya puesto para venderlo (50% reembolso).",
                    color = Dim, fontSize = 12.sp
                )
            }
        }

        // Vista del interior
        item {
            EmpireCard {
                Text("Interior (toca para colocar/quitar)", fontWeight = FontWeight.Bold, color = Paper)
                Spacer(Modifier.height(6.dp))
                HouseGrid(
                    state = state,
                    pickedKind = pickedKind?.let { runCatching { FurnitureKind.valueOf(it) }.getOrNull() },
                    onPlace = { kind, x, y ->
                        vm.placeFurniture(kind, x, y)
                        pickedKind = null
                    },
                    onRemove = { id -> vm.removeFurniture(id) }
                )
            }
        }

        // Catálogo de muebles
        item {
            EmpireCard {
                Text("Catálogo de muebles", fontWeight = FontWeight.Bold, color = Paper)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    FurnitureKind.values().forEach { fk ->
                        val selected = pickedKind == fk.name
                        Box(
                            Modifier
                                .padding(end = 8.dp)
                                .size(86.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Gold.copy(alpha = 0.3f) else InkBorder.copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    if (selected) Gold else InkBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { pickedKind = if (selected) null else fk.name }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(fk.emoji, fontSize = 22.sp)
                                Text(fk.displayName, fontSize = 10.sp, color = Paper)
                                Text(fk.price.fmtMoney(), fontSize = 9.sp,
                                    color = if (state.company.cash >= fk.price) Gold else Ruby)
                            }
                        }
                    }
                }
            }
        }

        // Familia
        item {
            EmpireCard {
                TabRow(selectedTabIndex = familyTab, containerColor = InkSoft, contentColor = Gold) {
                    Tab(selected = familyTab == 0, onClick = { familyTab = 0 }, text = { Text("Pareja") })
                    Tab(selected = familyTab == 1, onClick = { familyTab = 1 }, text = { Text("Hijos") })
                }
                Spacer(Modifier.height(8.dp))
                if (familyTab == 0) SpouseTab(state, vm) else ChildrenTab(state, vm)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun HouseGrid(
    state: GameState,
    pickedKind: FurnitureKind?,
    onPlace: (FurnitureKind, Int, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    val house = state.house
    val cellSize = 28.dp
    Column {
        for (y in 0 until house.height) {
            Row {
                for (x in 0 until house.width) {
                    val occupant = house.furniture.find { f ->
                        x in f.x until (f.x + f.spec().sizeX) && y in f.y until (f.y + f.spec().sizeY)
                    }
                    Box(
                        Modifier
                            .size(cellSize)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (occupant != null) Color(0xFF4E342E)
                                else Color(house.floorColor.toInt())
                            )
                            .border(
                                0.5.dp,
                                Color(0xFF6D4C41),
                                RoundedCornerShape(3.dp)
                            )
                            .clickable {
                                if (occupant != null) onRemove(occupant.id)
                                else if (pickedKind != null) onPlace(pickedKind, x, y)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (occupant != null && occupant.x == x && occupant.y == y) {
                            Text(occupant.spec().emoji, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpouseTab(state: GameState, vm: GameViewModel) {
    val sp = state.family.spouse
    if (sp == null) {
        Text("No tienes pareja todavía.", color = Dim)
        Spacer(Modifier.height(6.dp))
        Text(
            "Conoce NPCs en la ciudad (Bar, Plaza, etc.). Cuando tu friendship con un NPC sea alta, podrás proponerle matrimonio.",
            color = Dim, fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        var name by remember { mutableStateOf("") }
        Text("Atajo (testing): proponer matrimonio a alguien por nombre — cuesta 5.000 € (anillo).",
            color = Dim, fontSize = 10.sp)
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(20) },
            label = { Text("Nombre de tu pareja") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { if (name.isNotBlank()) vm.proposeMarriage(name) },
            enabled = state.company.cash >= 5_000.0 && name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
        ) { Text("💍 Proponer matrimonio (5.000 €)") }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💑", fontSize = 40.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(sp.name, fontWeight = FontWeight.Bold, color = Paper, fontSize = 18.sp)
                Text("Felicidad ${sp.happiness}/100 · ${sp.daysWith} días contigo",
                    color = Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Mantén una mascota o tu BalWealth alto para que no decaiga.", color = Dim, fontSize = 11.sp)
    }
}

@Composable
private fun ChildrenTab(state: GameState, vm: GameViewModel) {
    if (state.family.spouse == null) {
        Text("Necesitas pareja antes de tener hijos.", color = Dim)
        return
    }
    Text("Hijos: ${state.family.children.size}/4", color = Paper, fontWeight = FontWeight.Bold)
    state.family.children.forEach { c ->
        Text("👶 ${c.name} (${c.ageDays} días)", color = Paper, fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 2.dp))
    }
    Spacer(Modifier.height(6.dp))
    var childName by remember { mutableStateOf("") }
    OutlinedTextField(
        value = childName, onValueChange = { childName = it.take(15) },
        label = { Text("Nombre del bebé") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = { if (childName.isNotBlank()) vm.haveChild(childName); childName = "" },
        enabled = state.family.children.size < 4 && childName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink)
    ) { Text("👶 Tener bebé") }
}
