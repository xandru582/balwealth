package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Tarjeta enriquecida de empleado: combina datos del `Employee` clásico con
 * el `EmployeeProfile` ampliado del HrState.
 *
 *  - role pill con emoji y nombre del rol
 *  - chips de rasgos (traits)
 *  - badge de educación
 *  - barras de satisfacción y burnout
 *  - acciones (composables) inyectables vía slot `actions`
 */
@Composable
fun EmployeeCard(
    employee: Employee,
    profile: EmployeeProfile?,
    isExecutive: Boolean = false,
    execSlot: ExecSlot? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val role = profile?.role ?: EmployeeRole.LABORER
    val roleProfile = RoleCatalog.get(role)
    EmpireCard(borderColor = if (isExecutive) Gold else InkBorder) {
        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isExecutive) "👔" else roleProfile.emoji,
                fontSize = 26.sp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        employee.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (isExecutive && execSlot != null) {
                        Spacer(Modifier.width(6.dp))
                        BadgePill(
                            text = execSlot.displayName,
                            emoji = execSlot.emoji,
                            tint = Gold
                        )
                    }
                }
                Text(
                    "Skill ${"%.2f".format(employee.skill)} · " +
                        "Salario ${employee.monthlySalary.fmtMoney()}/mes",
                    color = Dim,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Pills de rol, nivel y educación
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgePill(
                text = roleProfile.displayName,
                emoji = roleProfile.emoji,
                tint = Sapphire
            )
            Spacer(Modifier.width(6.dp))
            BadgePill(
                text = "Nv ${profile?.level ?: 1}",
                emoji = "⭐",
                tint = Emerald
            )
            Spacer(Modifier.width(6.dp))
            BadgePill(
                text = (profile?.education ?: Education.HIGHSCHOOL).displayName,
                emoji = (profile?.education ?: Education.HIGHSCHOOL).emoji,
                tint = Paper
            )
        }

        // Rasgos
        val traits = profile?.traits ?: emptyList()
        if (traits.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowChips(traits = traits)
        }

        Spacer(Modifier.height(8.dp))

        // Barras
        val sat = (profile?.satisfactionScore ?: 70)
        val burn = (profile?.burnoutRisk ?: 10)
        ProgressBarWithLabel(
            progress = sat / 100f,
            label = "Satisfacción $sat%",
            color = if (sat >= 60) Emerald else if (sat >= 30) Gold else Ruby
        )
        Spacer(Modifier.height(4.dp))
        ProgressBarWithLabel(
            progress = burn / 100f,
            label = "Burnout $burn%",
            color = if (burn < 40) Sapphire else if (burn < 75) Gold else Ruby
        )

        // XP / nivel
        if (profile != null) {
            Spacer(Modifier.height(4.dp))
            val needed = profile.xpForNextLevel().coerceAtLeast(1L)
            ProgressBarWithLabel(
                progress = (profile.xp.toFloat() / needed.toFloat()).coerceIn(0f, 1f),
                label = "XP ${profile.xp}/${needed}",
                color = Gold
            )
        }

        // Asignación
        Spacer(Modifier.height(4.dp))
        Text(
            employee.assignedBuildingId?.let { "Asignado a $it" } ?: "Sin asignar",
            color = Dim,
            fontSize = 11.sp
        )

        // Slot acciones
        if (actions != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

@Composable
fun BadgePill(text: String, emoji: String? = null, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .border(1.dp, tint.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 11.sp)
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Render minimalista de chips de rasgos. Se distribuye en filas manuales
 * (Compose 1.5 no incluye FlowRow estable en M3 sin dependencia adicional).
 */
@Composable
private fun FlowChips(traits: List<EmployeeTrait>) {
    Column {
        traits.chunked(3).forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.forEach { t ->
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ink)
                            .border(1.dp, InkBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t.emoji, fontSize = 11.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(t.displayName, fontSize = 10.sp, color = Paper)
                    }
                }
            }
        }
    }
}
