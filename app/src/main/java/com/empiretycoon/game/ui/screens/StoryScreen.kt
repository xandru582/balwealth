package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.StorylineEngine
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.StoryArc
import com.empiretycoon.game.model.StoryChapter
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.EndingDialog
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

@Composable
fun StoryScreen(state: GameState, vm: GameViewModel) {
    val playable = StorylineEngine.isChapterPlayable(state)
    val current = StoryArc.byId(state.storyline.currentChapterId)
    val ending = StorylineEngine.checkEndingEligible(state)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { KarmaBar(state.storyline.karma) }
        item {
            EmpireCard {
                SectionTitle("Tu historia",
                    subtitle = "Capítulo ${state.storyline.completedChapters.size}/${StoryArc.chapters.size}")
                Spacer(Modifier.height(6.dp))
                if (current != null && playable) {
                    Text("Hay una decisión esperándote.",
                        color = Gold, fontWeight = FontWeight.Bold)
                } else if (ending != null) {
                    Text("El epílogo está listo.",
                        color = Emerald, fontWeight = FontWeight.Bold)
                } else if (current != null) {
                    Text("Sigue creciendo. Tu próximo capítulo: \"${current.title}\".",
                        color = Dim, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(requirementHint(current, state), color = Dim, fontSize = 12.sp)
                } else {
                    Text("Tu historia ha llegado al final.", color = Dim)
                }
            }
        }

        if (current != null && playable) {
            item { CinematicChapterCard(current) { vm.resolveStoryChoice(it) } }
        }

        item {
            EmpireCard {
                SectionTitle("Capítulos")
            }
        }
        items(StoryArc.chapters, key = { it.id }) { ch ->
            ChapterRow(ch, state)
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    // Modal de final si se cumple
    if (ending != null) {
        EndingDialog(ending = ending, onDismiss = { vm.acknowledgeEnding(ending.type.name) })
    }
}

@Composable
private fun KarmaBar(karma: Int) {
    val pct = (karma + 100) / 200f
    val color = when {
        karma >= 30 -> Emerald
        karma <= -30 -> Ruby
        else -> Gold
    }
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👹", fontSize = 22.sp)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.weight(1f).height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(InkBorder)
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(pct)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Ruby, Gold, Emerald)
                            )
                        )
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("😇", fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Karma: $karma  ·  ${karmaLabel(karma)}",
            color = color, fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

private fun karmaLabel(k: Int): String = when {
    k >= 70 -> "Santo del mercado"
    k >= 40 -> "Empresario íntegro"
    k >= 15 -> "Empresario decente"
    k > -15 -> "Pragmático"
    k > -40 -> "Tiburón"
    k > -70 -> "Despiadado"
    else -> "Tirano"
}

@Composable
private fun CinematicChapterCard(ch: StoryChapter, onChoose: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Gold, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = InkSoft),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A2640), InkSoft))
                )
                .padding(20.dp)
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ch.illustrationEmoji, fontSize = 64.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(ch.title.uppercase(),
                    color = Gold, fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(ch.intro, color = Paper, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify)
                Spacer(Modifier.height(14.dp))
                ch.choices.forEachIndexed { idx, choice ->
                    Button(
                        onClick = { onChoose(idx) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InkBorder,
                            contentColor = Paper
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(choice.label, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(ch: StoryChapter, state: GameState) {
    val s = state.storyline
    val completed = s.completedChapters.contains(ch.id)
    val current = s.currentChapterId == ch.id && !completed
    val locked = !completed && !current
    val accent = when {
        completed -> Emerald
        current -> Gold
        else -> InkBorder
    }
    EmpireCard(borderColor = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ch.illustrationEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (locked) "??? ${ch.title.take(0)}".trim() + "Capítulo bloqueado"
                    else ch.title,
                    fontWeight = FontWeight.Bold,
                    color = if (locked) Dim else Paper
                )
                if (!locked) {
                    Text(ch.outro, color = Dim, fontSize = 12.sp)
                } else {
                    Text(requirementHint(ch, state), color = Dim, fontSize = 11.sp)
                }
            }
            val mark = when {
                completed -> "✅"
                current -> "▶"
                else -> "🔒"
            }
            Text(mark, fontSize = 20.sp)
        }
    }
}

private fun requirementHint(ch: StoryChapter, state: GameState): String {
    val r = ch.requirements
    val parts = mutableListOf<String>()
    if (r.minLevel > state.player.level) parts += "Nv jugador ${r.minLevel}+"
    if (r.minCash > state.company.cash) parts += "${"%,.0f".format(r.minCash)} € en caja"
    if (r.daysSinceStart > state.day - 1) parts += "Día ${r.daysSinceStart}+"
    return if (parts.isEmpty()) "Listo para jugarse"
        else "Requiere: ${parts.joinToString(", ")}"
}
