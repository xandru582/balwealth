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
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Pantalla del negocio de un oficio concreto. Si no hay negocio abierto,
 * muestra la opción de abrirlo. Si está abierto: header con stats,
 * empleados activos, candidatos, opciones de mejora/cobrar/cerrar.
 */
@Composable
fun JobBusinessScreen(
    job: JobId,
    state: GameState,
    vm: GameViewModel,
    onBack: () -> Unit
) {
    val biz = state.jobBusinesses.businessOf(job)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Volver al hub", color = Gold) }
        }
        SectionTitle(
            "${job.emoji} ${job.displayName} — Tu empresa",
            subtitle = "Empleados especializados producen mientras estás fuera. Capped 24h offline."
        )
        Spacer(Modifier.height(12.dp))

        if (biz == null) {
            BusinessLockedView(job, state, vm)
        } else {
            BusinessActiveView(job, biz, state, vm)
        }
    }
}

@Composable
private fun BusinessLockedView(job: JobId, state: GameState, vm: GameViewModel) {
    val fee = JobBusinessCatalog.openingFee(job)
    val unlockedJob = state.jobs.progressOf(job).unlocked
    EmpireCard(borderColor = if (unlockedJob && state.company.cash >= fee) Sapphire else InkBorder) {
        Text("Aún no tienes empresa de ${job.displayName}",
            fontWeight = FontWeight.Bold, color = Paper, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Apertura: ${fee.fmtMoney()} (de la caja de tu empresa principal). " +
                "Después contratas empleados especializados que generan ingresos pasivos " +
                "(capeados a 24h si la app está cerrada).",
            color = Dim, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        if (!unlockedJob) {
            Text("🔒 Necesitas haber desbloqueado el oficio para montar empresa.",
                color = Color(0xFFFF7A7A), fontSize = 11.sp)
        } else if (state.company.cash < fee) {
            Text("💸 Tu empresa principal no tiene ${fee.fmtMoney()}.",
                color = Color(0xFFFF7A7A), fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { vm.jobBusinessOpen(job) },
        enabled = unlockedJob && state.company.cash >= fee,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("🏢 Abrir empresa de ${job.displayName}",
            color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BusinessActiveView(
    job: JobId,
    biz: JobBusinessState,
    state: GameState,
    vm: GameViewModel
) {
    val baseWage = job.baseHourlyWage
    val hourlyRev = biz.hourlyRevenue(baseWage)
    val dailySalaries = biz.dailySalaries(baseWage)
    var confirmClose by remember(job) { mutableStateOf(false) }

    // Header con treasury + KPIs
    EmpireCard(borderColor = Gold) {
        Text("📊 Resumen", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(4.dp))
        Row {
            HeaderCol("Treasury", biz.treasury.fmtMoney(), color = Emerald)
            Spacer(Modifier.width(12.dp))
            HeaderCol("Nivel local", "${biz.upgradeLevel}/5")
            Spacer(Modifier.width(12.dp))
            HeaderCol("Empleados", "${biz.employees.size}/${biz.maxEmployees}")
        }
        Spacer(Modifier.height(6.dp))
        Row {
            HeaderCol("Revenue/h", hourlyRev.fmtMoney(), color = Sapphire)
            Spacer(Modifier.width(12.dp))
            HeaderCol("Salarios/día", dailySalaries.fmtMoney(),
                color = if (dailySalaries > 0) Color(0xFFFFB74D) else Dim)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Lifetime: generado ${biz.totalEarned.fmtMoney()} · " +
                "salarios pagados ${biz.totalPaidSalaries.fmtMoney()} · " +
                "cobrado ${biz.totalCollected.fmtMoney()}",
            color = Dim, fontSize = 10.sp
        )
    }
    Spacer(Modifier.height(12.dp))

    // Botón cobrar (grande, prominente)
    Button(
        onClick = { vm.jobBusinessCollect(job) },
        enabled = biz.treasury > 0.01,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("💰 COBRAR ${biz.treasury.fmtMoney()}",
            color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
    Spacer(Modifier.height(12.dp))

    // Mejora local
    if (biz.upgradeLevel < 5) {
        val upCost = JobBusinessCatalog.upgradeCost(job, biz.upgradeLevel)
        EmpireCard(borderColor = if (state.company.cash >= upCost) Sapphire else InkBorder) {
            Text(
                "🏗️ Mejorar local a nivel ${biz.upgradeLevel + 1}/5",
                fontWeight = FontWeight.Bold, color = Paper, fontSize = 13.sp
            )
            Text(
                "+1 empleado de capacidad · revenue ×${"%.1f".format(1.0 + (biz.upgradeLevel + 1) * 0.20)}",
                color = Dim, fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { vm.jobBusinessUpgrade(job) },
                enabled = state.company.cash >= upCost,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mejorar (${upCost.fmtMoney()})")
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    // Empleados
    SectionTitle("👥 Empleados (${biz.employees.size}/${biz.maxEmployees})")
    Spacer(Modifier.height(6.dp))
    if (biz.employees.isEmpty()) {
        EmpireCard {
            Text("Aún sin empleados. Contrata abajo para empezar a producir.",
                color = Dim, fontSize = 12.sp)
        }
    } else {
        for (emp in biz.employees) {
            EmployeeRow(job, emp, vm)
            Spacer(Modifier.height(6.dp))
        }
    }
    Spacer(Modifier.height(12.dp))

    // Candidatos
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("🎯 Candidatos")
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { vm.jobBusinessRefreshCandidates() }) {
            Text("Refrescar", color = Sapphire, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
    if (biz.candidatePool.isEmpty()) {
        EmpireCard {
            Text("Pool vacío — pulsa Refrescar.", color = Dim, fontSize = 12.sp)
        }
    } else {
        for (cand in biz.candidatePool) {
            CandidateRow(job, cand, biz, state, vm)
            Spacer(Modifier.height(6.dp))
        }
    }
    Spacer(Modifier.height(12.dp))

    // Cerrar negocio (peligroso)
    val refund = JobBusinessCatalog.closingRefund(job) + biz.treasury
    val severance = biz.employees.sumOf { JobBusinessCatalog.severance(job, it) }
    val net = refund - severance
    EmpireCard(borderColor = Color(0xFFB85C5C)) {
        Text("⚠️ Cerrar la empresa", fontWeight = FontWeight.Bold,
            color = Color(0xFFFF7A7A), fontSize = 13.sp)
        Text(
            "Refund + treasury ${refund.fmtMoney()} − indemnizaciones ${severance.fmtMoney()} = " +
                "${if (net >= 0) "+" else ""}${net.fmtMoney()}",
            color = Dim, fontSize = 11.sp
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { confirmClose = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar empresa", color = Color(0xFFFF7A7A), fontWeight = FontWeight.Bold)
        }
    }

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("¿Cerrar ${job.displayName}?") },
            text = {
                Text(
                    "Recibirás ${refund.fmtMoney()} (refund + treasury) y pagarás " +
                        "${severance.fmtMoney()} en indemnizaciones. " +
                        "Resultado neto: ${if (net >= 0) "+" else ""}${net.fmtMoney()}. " +
                        "\n\nEsta acción NO se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.jobBusinessClose(job)
                    confirmClose = false
                }) {
                    Text("Cerrar empresa", color = Color(0xFFFF7A7A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) {
                    Text("Cancelar", color = Dim)
                }
            }
        )
    }
}

@Composable
private fun HeaderCol(label: String, value: String, color: Color = Paper) {
    Column {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmployeeRow(job: JobId, emp: JobEmployee, vm: GameViewModel) {
    val baseWage = job.baseHourlyWage
    EmpireCard(borderColor = if (emp.skill >= 70) Emerald else if (emp.skill >= 50) Sapphire else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(emp.name, fontWeight = FontWeight.Bold, color = Paper, fontSize = 13.sp)
                Text(
                    "Skill ${emp.skill} · genera ${emp.hourlyRevenue(baseWage).fmtMoney()}/h",
                    color = Sapphire, fontSize = 11.sp
                )
                Text(
                    "Salario ${emp.dailySalary(baseWage).fmtMoney()}/día",
                    color = Color(0xFFFFB74D), fontSize = 10.sp
                )
            }
            TextButton(onClick = { vm.jobBusinessFire(job, emp.id) }) {
                Text("Despedir", color = Color(0xFFFF7A7A), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CandidateRow(
    job: JobId,
    cand: JobEmployee,
    biz: JobBusinessState,
    state: GameState,
    vm: GameViewModel
) {
    val fee = JobBusinessCatalog.signingFee(job, cand)
    val canHire = biz.employees.size < biz.maxEmployees && state.company.cash >= fee
    EmpireCard(borderColor = if (canHire) Gold else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text("🎯", fontSize = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(cand.name, fontWeight = FontWeight.Bold, color = Paper, fontSize = 13.sp)
                Text(
                    "Skill ${cand.skill} · genera ${cand.hourlyRevenue(job.baseHourlyWage).fmtMoney()}/h",
                    color = Sapphire, fontSize = 11.sp
                )
                Text(
                    "Salario ${cand.dailySalary(job.baseHourlyWage).fmtMoney()}/día · prima ${fee.fmtMoney()}",
                    color = Dim, fontSize = 10.sp
                )
            }
            Button(
                onClick = { vm.jobBusinessHire(job, cand.id) },
                enabled = canHire,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Fichar", color = Color.Black, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}
