package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.util.fmtPct

@Composable
fun BankingScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = InkSoft,
            contentColor = Gold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("Préstamos disponibles") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("Mis préstamos") })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("Hipotecas") })
        }
        when (tab) {
            0 -> AvailableLoansTab(state, vm)
            1 -> ActiveLoansTab(state, vm)
            2 -> MortgagesTab(state, vm)
        }
    }
}

// =========== TAB 0: Disponibles ===========

@Composable
private fun AvailableLoansTab(state: GameState, vm: GameViewModel) {
    val offers = state.loans.offers.filter { it.type != LoanType.MORTGAGE }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Catálogo de préstamos",
                            subtitle = "Negocia condiciones según tu reputación.")
                        Text("Reputación actual: ${state.company.reputation}/100",
                            color = Dim, fontSize = 12.sp)
                        Text("Deuda total viva: ${state.loans.totalDebt.fmtMoney()}",
                            color = if (state.loans.totalDebt > 0) Ruby else Emerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { vm.refreshLoanOffers() }) {
                        Text("Refrescar", color = Sapphire)
                    }
                }
            }
        }
        if (offers.isEmpty()) {
            item {
                Text("No hay ofertas disponibles. Pulsa Refrescar.",
                    color = Dim, modifier = Modifier.padding(16.dp))
            }
        } else {
            items(offers, key = { it.id }) { LoanOfferCard(it, state, vm) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun LoanOfferCard(offer: LoanOffer, state: GameState, vm: GameViewModel) {
    val totalInterest = offer.totalInterest()
    val canTake = state.company.reputation >= offer.requiredReputation
    val borderColor = when (offer.type) {
        LoanType.PREDATORY -> Ruby
        LoanType.MORTGAGE -> Gold
        LoanType.BUSINESS -> Sapphire
        else -> InkBorder
    }
    EmpireCard(borderColor = borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(offer.type.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${offer.type.displayName} · ${offer.lenderName}",
                    fontWeight = FontWeight.Bold)
                Text(offer.type.description, color = Dim, fontSize = 11.sp)
            }
            Text(offer.amount.fmtMoney(), fontWeight = FontWeight.Bold, color = Gold)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            ChipMini("APR", offer.interestRateAPR.fmtPct(),
                if (offer.interestRateAPR > 0.20) Ruby else Emerald)
            Spacer(Modifier.width(6.dp))
            ChipMini("Plazo", "${offer.termDays}d", Sapphire)
            Spacer(Modifier.width(6.dp))
            ChipMini("Cuota/d", offer.estimatedDailyPayment().fmtMoney(), Paper)
        }
        Spacer(Modifier.height(6.dp))
        Text("Comisión: ${offer.fixedFee.fmtMoney()} · Interés total: ${totalInterest.fmtMoney()}",
            color = Dim, fontSize = 11.sp)
        if (offer.requiredReputation > 0)
            Text("Requiere reputación ≥ ${offer.requiredReputation}",
                color = if (canTake) Dim else Ruby, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.takeLoan(offer.id) },
            enabled = canTake,
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold, contentColor = Ink,
                disabledContainerColor = InkBorder, disabledContentColor = Dim
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (canTake) "Aceptar préstamo" else "No cualifica",
                fontWeight = FontWeight.Bold)
        }
    }
}

// =========== TAB 1: Activos ===========

@Composable
private fun ActiveLoansTab(state: GameState, vm: GameViewModel) {
    val active = state.loans.active
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Resumen de deuda")
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Principal vivo", color = Dim, fontSize = 11.sp)
                        Text(state.loans.totalDebt.fmtMoney(),
                            fontWeight = FontWeight.Bold, color = Ruby)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Cuota diaria", color = Dim, fontSize = 11.sp)
                        Text(state.loans.totalDailyPayment.fmtMoney(),
                            fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Intereses pagados", color = Dim, fontSize = 11.sp)
                        Text(state.loans.totalLifetimeInterest.fmtMoney(),
                            fontWeight = FontWeight.SemiBold, color = Gold)
                    }
                }
                if (state.loans.totalDefaults > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("Impagos históricos: ${state.loans.totalDefaults}",
                        color = Ruby, fontSize = 11.sp)
                }
            }
        }
        if (active.isEmpty()) {
            item {
                Text("No tienes préstamos activos.",
                    color = Dim, modifier = Modifier.padding(16.dp))
            }
        } else {
            items(active, key = { it.id }) { ActiveLoanCard(it, state, vm) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ActiveLoanCard(loan: ActiveLoan, state: GameState, vm: GameViewModel) {
    val borderColor = if (loan.defaulted) Ruby
        else if (loan.missedPayments > 0) Gold else InkBorder
    EmpireCard(borderColor = borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(loan.type.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${loan.type.displayName} · ${loan.lenderName}",
                    fontWeight = FontWeight.Bold)
                Text("APR ${loan.interestRateAPR.fmtPct()} · Cuota/d ${loan.dailyPayment.fmtMoney()}",
                    color = Dim, fontSize = 11.sp)
            }
            Text(loan.remainingPrincipal.fmtMoney(),
                fontWeight = FontWeight.Bold, color = Ruby)
        }
        Spacer(Modifier.height(6.dp))
        ProgressBarWithLabel(
            progress = loan.progress,
            label = "Día ${loan.daysElapsed}/${loan.termDays}",
            color = if (loan.defaulted) Ruby else Emerald
        )
        if (loan.missedPayments > 0 && !loan.defaulted) {
            Text("⚠ Cuotas impagadas: ${loan.missedPayments}/${LoanPenalties.DEFAULT_AT_MISSES}",
                color = Gold, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        if (loan.defaulted) {
            Text("✖ En mora — contacta a tu banca.",
                color = Ruby, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = { vm.repayLoan(loan.id, loan.dailyPayment) },
                enabled = !loan.defaulted && state.company.cash >= loan.dailyPayment,
                colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Pagar 1 cuota", fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = { vm.repayLoan(loan.id, loan.remainingPrincipal) },
                enabled = !loan.defaulted && state.company.cash >= loan.remainingPrincipal,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Liquidar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// =========== TAB 2: Hipotecas ===========

@Composable
private fun MortgagesTab(state: GameState, vm: GameViewModel) {
    val mortgageOffers = state.loans.offers.filter { it.type == LoanType.MORTGAGE }
    val mortgageActive = state.loans.active.filter { it.type == LoanType.MORTGAGE }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Hipotecas",
                    subtitle = "Plazo largo, tipo bajo, requiere colateral inmobiliario.")
                Spacer(Modifier.height(4.dp))
                Text("Patrimonio inmobiliario: ${state.realEstate.totalValue.fmtMoney()}",
                    color = Dim, fontSize = 12.sp)
            }
        }
        if (mortgageActive.isNotEmpty()) {
            item { SectionTitle("Hipotecas activas") }
            items(mortgageActive, key = { it.id }) { ActiveLoanCard(it, state, vm) }
        }
        item { SectionTitle("Ofertas hipotecarias") }
        if (mortgageOffers.isEmpty()) {
            item { Text("No hay ofertas. Sube tu reputación a 40+.",
                color = Dim, modifier = Modifier.padding(16.dp)) }
        } else {
            items(mortgageOffers, key = { it.id }) { LoanOfferCard(it, state, vm) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =========== Helpers UI ===========

@Composable
private fun ChipMini(label: String, value: String,
                    color: androidx.compose.ui.graphics.Color = Paper) {
    Column {
        Text(label, color = Dim, fontSize = 9.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = color, fontSize = 12.sp)
    }
}
