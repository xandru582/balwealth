package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

@Composable
fun NpcsScreen(state: GameState, vm: GameViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val known = state.npcs.known
    val unknown = NPCCatalog.all.filterNot { known.containsKey(it.id) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Tu agenda",
                    subtitle = "${known.size}/${NPCCatalog.all.size} contactos conocidos")
                Spacer(Modifier.height(4.dp))
                Text("Cuanto más subas la amistad, mejor te tratan los NPCs cuando " +
                    "te encuentras con ellos.", color = Dim, fontSize = 12.sp)
            }
        }

        if (known.isEmpty()) {
            item {
                EmpireCard {
                    Text("Aún no conoces a nadie. Pasa el día y aparecerán encuentros.",
                        color = Dim)
                }
            }
        }

        if (known.isNotEmpty()) {
            item { SectionTitle("Conocidos") }
            items(known.entries.toList(), key = { it.key }) { (id, rel) ->
                val npc = NPCCatalog.byId(id) ?: return@items
                NpcCard(npc, rel, onTap = { selectedId = id })
            }
        }
        if (unknown.isNotEmpty()) {
            item { SectionTitle("Por conocer") }
            items(unknown, key = { it.id }) { npc ->
                EmpireCard(borderColor = InkBorder) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("❓", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Desconocido (${npc.role.name.lowercase()})",
                                color = Dim, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text("Aún no os habéis cruzado.",
                                color = Dim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    selectedId?.let { id ->
        val npc = NPCCatalog.byId(id)
        val rel = state.npcs.known[id]
        if (npc != null) {
            NpcDetailDialog(
                npc = npc,
                rel = rel,
                onClose = { selectedId = null },
                onGift = { kind, cost ->
                    vm.giftNpc(id, kind, cost)
                    selectedId = null
                },
                onChat = {
                    vm.chatWithNpc(id)
                    selectedId = null
                }
            )
        }
    }
}

@Composable
private fun NpcCard(npc: NPC, rel: NPCRelationship, onTap: () -> Unit) {
    val accent = when {
        rel.friendshipLevel >= 70 -> Emerald
        rel.friendshipLevel >= 30 -> Gold
        else -> InkBorder
    }
    EmpireCard(borderColor = accent) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(InkSoft)
        ) {
            Text(npc.portrait, fontSize = 32.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkBorder)
                    .padding(6.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(npc.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(roleEmoji(npc.role), fontSize = 16.sp)
                }
                Text(npc.role.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = Dim, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                ProgressBarWithLabel(
                    rel.friendshipLevel / 100f,
                    label = "Amistad ${rel.friendshipLevel}/100",
                    color = accent
                )
                Spacer(Modifier.height(6.dp))
                if (rel.quote.isNotEmpty()) {
                    Text("\"${rel.quote}\"", color = Paper,
                        fontSize = 12.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onTap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBorder, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Detalles") }
            }
        }
    }
}

@Composable
private fun NpcDetailDialog(
    npc: NPC,
    rel: NPCRelationship?,
    onClose: () -> Unit,
    onGift: (String, Double) -> Unit,
    onChat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cerrar", color = Gold) }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(npc.portrait, fontSize = 32.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(npc.name, fontWeight = FontWeight.Bold)
                    Text(npc.role.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = Dim, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column {
                Text(npc.bio, color = Paper, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Personalidad: ${npc.personality}", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                if (rel != null) {
                    Text("Amistad ${rel.friendshipLevel}/100  ·  ${rel.encounters} encuentros",
                        color = Dim, fontSize = 12.sp)
                    if (rel.quote.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("\"${rel.quote}\"", fontSize = 13.sp,
                            color = Paper)
                    }
                } else {
                    Text("Aún no le conoces personalmente.", color = Dim)
                }
                Spacer(Modifier.height(12.dp))
                SectionTitle("Acciones")
                Button(
                    onClick = onChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sapphire, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Charlar (gratis, +1 amistad)") }
                Button(
                    onClick = { onGift("Café", 50.0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBorder, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Café  ·  50 €  (+2)") }
                Button(
                    onClick = { onGift("Cena", 300.0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBorder, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Cena buena  ·  300 €  (+6)") }
                Button(
                    onClick = { onGift("Reloj de marca", 1500.0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold, contentColor = Ink),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Reloj de marca  ·  1.500 €  (+12)") }
                Button(
                    onClick = { onGift("Viaje sorpresa", 6000.0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ruby, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Viaje sorpresa  ·  6.000 €  (+25)") }
            }
        },
        containerColor = InkSoft
    )
}

private fun roleEmoji(r: NPCRole): String = when (r) {
    NPCRole.MENTOR -> "🧓"
    NPCRole.RIVAL -> "🦊"
    NPCRole.ROMANTIC -> "💞"
    NPCRole.FAMILY -> "👨‍👩‍👧"
    NPCRole.EMPLOYEE_SPECIAL -> "🧑‍💼"
    NPCRole.JOURNALIST -> "📝"
    NPCRole.BANKER -> "🏦"
    NPCRole.POLITICIAN -> "🏛️"
    NPCRole.MOBSTER -> "🎩"
    NPCRole.CONSULTANT -> "📊"
    NPCRole.INFLUENCER -> "📱"
    NPCRole.OLD_FRIEND -> "🍻"
}
