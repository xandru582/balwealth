package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.AICompanionState
import com.empiretycoon.game.model.CompanionPersonality
import com.empiretycoon.game.model.CompanionTip
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

/**
 * Pantalla del Asistente IA. Dos estados:
 *  - LOCKED: presentación + selección de personalidad inicial.
 *  - UNLOCKED: lista de tips activos + ajustes de personalidad/nombre.
 */
@Composable
fun CompanionScreen(state: GameState, vm: GameViewModel) {
    val ai = state.aiCompanion
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (!ai.unlocked) {
            CompanionLockedView(state, vm)
        } else {
            CompanionUnlockedView(state, ai, vm)
        }
    }
}

// ===================== LOCKED =====================

@Composable
private fun CompanionLockedView(state: GameState, vm: GameViewModel) {
    SectionTitle(
        "🤖 Asistente IA",
        subtitle = "Heurísticas locales: detecta nóminas justas, reputación caída, riesgos cripto, retos casi hechos…"
    )
    Spacer(Modifier.height(12.dp))

    val canUnlock = state.player.level >= 3
    EmpireCard(borderColor = if (canUnlock) Sapphire else InkBorder) {
        Text(
            "¿Qué hace el asistente?",
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper
        )
        Text(
            "Lee tu estado cada 5 minutos in-game y te avisa SOLO cuando hay algo que merece tu atención. Nada de spam: como mucho 6 tips priorizados.",
            color = Dim, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        if (!canUnlock) {
            Text(
                "🔒 Necesitas nivel ≥ 3 para activarlo. Tu nivel actual: ${state.player.level}.",
                color = Color(0xFFFF7A7A), fontSize = 12.sp
            )
        } else {
            Text(
                "✅ Disponible. Elige una personalidad y opcionalmente cámbiale el nombre.",
                color = Emerald, fontSize = 12.sp
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    var selected by remember { mutableStateOf(CompanionPersonality.MENTOR) }
    var nameInput by remember { mutableStateOf("") }

    SectionTitle("Personalidad", subtitle = "Solo afecta al tono. Las recomendaciones son las mismas.")
    Spacer(Modifier.height(8.dp))
    for (p in CompanionPersonality.values()) {
        PersonalityRow(
            personality = p,
            selected = selected == p,
            onClick = { selected = p }
        )
        Spacer(Modifier.height(6.dp))
    }

    Spacer(Modifier.height(12.dp))
    SectionTitle("Nombre (opcional)")
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = nameInput,
        onValueChange = { if (it.length <= 16) nameInput = it },
        placeholder = { Text(AICompanionState.defaultName(selected), color = Dim) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { vm.companionUnlock(selected, nameInput) },
        enabled = canUnlock,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("🤖 Activar asistente", fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun PersonalityRow(
    personality: CompanionPersonality,
    selected: Boolean,
    onClick: () -> Unit
) {
    EmpireCard(
        modifier = Modifier.clickable { onClick() },
        borderColor = if (selected) Gold else InkBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(personality.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    personality.displayName,
                    fontWeight = FontWeight.SemiBold, color = Paper, fontSize = 14.sp
                )
                Text(
                    personality.tagline,
                    color = Dim, fontSize = 11.sp
                )
                Text(
                    "Default: ${AICompanionState.defaultName(personality)}",
                    color = Dim, fontSize = 10.sp
                )
            }
            if (selected) {
                Text("✓", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
        }
    }
}

// ===================== UNLOCKED =====================

@Composable
private fun CompanionUnlockedView(
    state: GameState,
    ai: AICompanionState,
    vm: GameViewModel
) {
    // Header
    EmpireCard(borderColor = Gold) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(ai.personality.emoji, fontSize = 32.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ai.displayName,
                    fontWeight = FontWeight.Black, color = Gold, fontSize = 18.sp
                )
                Text(
                    "${ai.personality.displayName} · ${ai.moodEmoji} ${ai.moodLabel}",
                    color = Dim, fontSize = 12.sp
                )
                Text(
                    "Tips atendidos: ${ai.tipsApplied} · descartados: ${ai.tipsDismissed}",
                    color = Dim, fontSize = 11.sp
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // Tips
    SectionTitle(
        "📋 Tips activos (${ai.tips.size}/6)",
        subtitle = "Marca \"Hecho\" si ya lo atendiste — sube el ánimo del asistente. \"Descartar\" silencia esa categoría un rato."
    )
    Spacer(Modifier.height(8.dp))
    if (ai.tips.isEmpty()) {
        EmpireCard {
            Text(
                "✨ Todo en orden. Volveré a revisar en unos minutos.",
                color = Dim, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    } else {
        for (tip in ai.tips) {
            TipCard(tip, vm)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = { vm.companionClearAll() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limpiar todos los tips", color = Dim, fontSize = 12.sp)
        }
    }

    Spacer(Modifier.height(16.dp))

    // Personality switcher
    SectionTitle("Cambiar personalidad", subtitle = "Solo afecta al tono.")
    Spacer(Modifier.height(8.dp))
    for (p in CompanionPersonality.values()) {
        PersonalityRow(
            personality = p,
            selected = ai.personality == p,
            onClick = { vm.companionSetPersonality(p) }
        )
        Spacer(Modifier.height(6.dp))
    }

    Spacer(Modifier.height(12.dp))

    // Renombrar
    var newName by remember(ai.name) { mutableStateOf(ai.name) }
    SectionTitle("Renombrar")
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = newName,
        onValueChange = { if (it.length <= 16) newName = it },
        placeholder = { Text(AICompanionState.defaultName(ai.personality), color = Dim) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { vm.companionRename(newName) },
        enabled = newName.trim() != ai.name
    ) {
        Text("Guardar nombre", color = Gold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TipCard(tip: CompanionTip, vm: GameViewModel) {
    val border = priorityColor(tip.priority)
    EmpireCard(borderColor = border) {
        Text(
            tip.title,
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper
        )
        Spacer(Modifier.height(4.dp))
        Text(tip.body, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkBorder)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "Prio ${tip.priority}",
                    color = border, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.companionDismiss(tip.id) }) {
                Text("Descartar", color = Dim, fontSize = 12.sp)
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = { vm.companionAcknowledge(tip.id) }) {
                Text("✓ Hecho", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun priorityColor(priority: Int): Color = when {
    priority >= 80 -> Color(0xFFFF7A7A)   // rojo — urgente
    priority >= 60 -> Gold                 // ámbar — importante
    priority >= 40 -> Sapphire             // azul — atento
    else -> Dim                            // gris — informativo
}
