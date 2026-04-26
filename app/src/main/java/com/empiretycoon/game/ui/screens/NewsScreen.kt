package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EconomyBanner
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

@Composable
fun NewsScreen(state: GameState, vm: GameViewModel) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    val items = state.news.items
        .filter { filter == null || it.category.name == filter }
        .sortedByDescending { it.timestamp }

    Column(Modifier.fillMaxSize()) {
        // Banner de fase económica
        EconomyBanner(
            econ = state.economy,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text("Todo") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Gold,
                    selectedLabelColor = Ink
                )
            )
            NewsCategory.values().forEach { c ->
                FilterChip(
                    selected = filter == c.name,
                    onClick = { filter = if (filter == c.name) null else c.name },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.emoji)
                            Spacer(Modifier.width(4.dp))
                            Text(c.displayName, fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold,
                        selectedLabelColor = Ink
                    )
                )
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📰", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sin noticias por ahora.",
                        color = Dim
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Las noticias aparecerán al avanzar el juego.",
                        color = Dim,
                        fontSize = 11.sp
                    )
                }
            }
            return
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionTitle(
                    "Feed de noticias",
                    subtitle = "Total: ${state.news.items.size}"
                )
            }
            items(items, key = { it.id }) { n ->
                NewsCard(state, n)
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun NewsCard(state: GameState, n: NewsItem) {
    val severityColor = when (n.severity) {
        NewsSeverity.TRIVIAL -> Dim
        NewsSeverity.MINOR -> Sapphire
        NewsSeverity.MAJOR -> Gold
        NewsSeverity.BREAKING -> Ruby
    }
    val impactColor = when {
        n.priceImpact > 0.0 -> Emerald
        n.priceImpact < 0.0 -> Ruby
        else -> Dim
    }
    EmpireCard(borderColor = severityColor.copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(n.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityPill(n.severity, severityColor)
                    Spacer(Modifier.width(6.dp))
                    CategoryPill(n.category)
                    Spacer(Modifier.weight(1f))
                    Text(
                        timeAgo(state.tick, n.timestamp),
                        color = Dim, fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    n.headline,
                    fontWeight = FontWeight.Bold,
                    color = Paper,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(n.body, color = Dim, fontSize = 12.sp, maxLines = 3)
                if (n.affectedResources.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Afecta:", color = Dim, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        n.affectedResources.take(5).forEach { rid ->
                            val r = ResourceCatalog.tryById(rid)
                            if (r != null) {
                                Row(
                                    Modifier
                                        .padding(end = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(InkBorder)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(r.emoji, fontSize = 10.sp)
                                    Spacer(Modifier.width(2.dp))
                                    Text(r.name, color = Paper, fontSize = 10.sp)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            (if (n.priceImpact >= 0) "+" else "") +
                                "${"%.1f".format(n.priceImpact * 100)}%",
                            color = impactColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                val active = n.expiresAtDay >= state.day
                Text(
                    if (active) "Activa · expira día ${n.expiresAtDay}"
                    else "Expirada (día ${n.expiresAtDay})",
                    color = if (active) Emerald else Dim,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SeverityPill(s: NewsSeverity, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(s.displayName.uppercase(), color = color, fontSize = 9.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryPill(c: NewsCategory) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(InkBorder)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(c.emoji, fontSize = 10.sp)
        Spacer(Modifier.width(3.dp))
        Text(c.displayName, color = Paper, fontSize = 9.sp)
    }
}

/** Devuelve un texto tipo "hace 3m" o "hace 2h" basado en ticks (1 tick = 1s). */
private fun timeAgo(currentTick: Long, generatedTick: Long): String {
    val diff = (currentTick - generatedTick).coerceAtLeast(0L)
    return when {
        diff < 60 -> "hace ${diff}s"
        diff < 60 * 60 -> "hace ${diff / 60}m"
        diff < 24 * 60 * 60 -> "hace ${diff / 3_600}h"
        else -> "hace ${diff / 86_400}d"
    }
}
