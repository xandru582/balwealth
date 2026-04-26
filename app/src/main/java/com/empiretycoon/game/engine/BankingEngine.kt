package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor bancario puro: produce GameState -> GameState.
 * Las funciones leen el campo `loans` (LoansState) que añade el patch de integración
 * a GameState. Mientras no esté presente, devuelven `state` sin tocar.
 */
object BankingEngine {

    private const val OFFER_REFRESH_TICKS: Long = 1_440L * 2  // 2 días in-game

    // ----------------------- Acciones del jugador -----------------------

    fun takeLoan(state: GameState, offerId: String): GameState {
        val loans = state.loans
        val offer = loans.offers.find { it.id == offerId }
            ?: return notify(state, NotificationKind.WARNING, "Oferta no disponible",
                "La oferta de préstamo ya no existe.")

        if (state.company.reputation < offer.requiredReputation) {
            return notify(state, NotificationKind.ERROR, "Reputación insuficiente",
                "Necesitas ${offer.requiredReputation} de reputación.")
        }

        // Para hipotecas: requiere colateral (valor inmobiliario)
        if (offer.type == LoanType.MORTGAGE) {
            val collateralNeeded = offer.amount * offer.requiredCollateralValue
            if (state.realEstate.totalValue < collateralNeeded) {
                return notify(state, NotificationKind.ERROR, "Sin colateral",
                    "Necesitas ${"%,.0f".format(collateralNeeded)} en inmuebles.")
            }
        }

        val newLoan = ActiveLoan(
            id = "ac_${state.tick}_${state.loans.active.size}_${(0..9_999).random()}",
            type = offer.type,
            principal = offer.amount,
            remainingPrincipal = offer.amount,
            interestRateAPR = offer.interestRateAPR,
            termDays = offer.termDays,
            daysElapsed = 0,
            dailyPayment = offer.estimatedDailyPayment(),
            missedPayments = 0,
            defaulted = false,
            lenderName = offer.lenderName
        )
        val net = offer.amount - offer.fixedFee
        val company = state.company.copy(cash = state.company.cash + net)
        val newLoans = loans.copy(
            active = loans.active + newLoan,
            offers = loans.offers - offer
        )
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Préstamo concedido",
            message = "${offer.type.displayName}: ${"%,.0f".format(net)} € recibidos."
        )
        return state.copy(
            company = company,
            loans = newLoans,
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    fun repayLoan(state: GameState, loanId: String, amount: Double): GameState {
        val loans = state.loans
        val loan = loans.active.find { it.id == loanId } ?: return state
        if (loan.defaulted) return notify(state, NotificationKind.WARNING,
            "Préstamo en mora", "Contacta al banco antes de continuar.")
        // FIX BUG-05-coerce-crash: si cash es negativo, min(cash, principal) es negativo
        // y coerceIn(0.0, negativo) lanza IllegalArgumentException porque min > max.
        // Aseguramos un upperBound válido siempre.
        val maxPayable = min(state.company.cash, loan.remainingPrincipal).coerceAtLeast(0.0)
        val pay = amount.coerceIn(0.0, maxPayable)
        if (pay <= 0.0) return notify(state, NotificationKind.WARNING,
            "Importe inválido", "Introduce una cantidad mayor que cero.")
        val updated = loan.copy(remainingPrincipal = loan.remainingPrincipal - pay)
        val activeNew = if (updated.remainingPrincipal <= 0.01) {
            loans.active - loan
        } else {
            loans.active.map { if (it.id == loanId) updated else it }
        }
        val company = state.company.copy(cash = state.company.cash - pay)
        val msg = if (updated.remainingPrincipal <= 0.01)
            "Préstamo liquidado: ${"%,.0f".format(pay)} €."
        else
            "Pago aplicado: ${"%,.0f".format(pay)} €. Queda ${"%,.0f".format(updated.remainingPrincipal)} €."
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "Pago de préstamo",
            message = msg
        )
        return state.copy(
            company = company,
            loans = loans.copy(active = activeNew),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    fun refreshOffers(state: GameState, rng: Random): GameState {
        val cashFlow = estimateCashFlow(state)
        val offers = LoanOfferGenerator.generate(state.company.reputation, cashFlow, rng)
        return state.copy(
            loans = state.loans.copy(offers = offers, lastOffersTick = state.tick)
        )
    }

    // ----------------------- Tick diario de préstamos -----------------------

    /**
     * Llamar 1 vez al día. Cobra cuotas, contabiliza intereses,
     * marca defaults y aplica penalizaciones de reputación.
     */
    fun tickLoans(state: GameState): GameState {
        val loans = state.loans
        if (loans.active.isEmpty()) return state

        var company = state.company
        var totalInterest = 0.0
        var defaultsHappened = 0
        val notifications = mutableListOf<GameNotification>()
        val newActive = mutableListOf<ActiveLoan>()

        for (l in loans.active) {
            if (l.defaulted) { newActive += l; continue }
            if (l.isPaidOff) continue
            if (l.daysElapsed >= l.termDays) {
                // pago final
                val finalPay = l.remainingPrincipal
                if (company.cash >= finalPay) {
                    company = company.copy(cash = company.cash - finalPay)
                    totalInterest += 0.0
                } else {
                    val miss = (l.missedPayments + 1)
                    if (miss >= LoanPenalties.DEFAULT_AT_MISSES) {
                        defaultsHappened++
                        company = company.copy(
                            reputation = (company.reputation - LoanPenalties.DEFAULT_REP_PENALTY)
                                .coerceIn(0, 100)
                        )
                        newActive += l.copy(defaulted = true, missedPayments = miss)
                        notifications += notify(
                            "Impago",
                            "${l.type.displayName} de ${l.lenderName} marcado como impagado.",
                            NotificationKind.ERROR
                        )
                        continue
                    } else {
                        newActive += l.copy(missedPayments = miss)
                        notifications += notify(
                            "Cuota impagada",
                            "Falló el último pago de ${l.lenderName} ($miss/${LoanPenalties.DEFAULT_AT_MISSES}).",
                            NotificationKind.WARNING
                        )
                        continue
                    }
                }
                continue  // termina sin añadir a newActive
            }

            // pago diario regular
            val dailyRate = l.interestRateAPR / 365.0
            val interest = l.remainingPrincipal * dailyRate
            val basePay = l.dailyPayment
            val penalty = if (l.missedPayments > 0) LoanPenalties.DAILY_PENALTY_MULT else 1.0
            val pay = basePay * penalty

            if (company.cash >= pay) {
                company = company.copy(cash = company.cash - pay)
                val principalReduce = (pay - interest).coerceAtLeast(0.0)
                val newRemaining = (l.remainingPrincipal - principalReduce).coerceAtLeast(0.0)
                totalInterest += interest
                val updated = l.copy(
                    remainingPrincipal = newRemaining,
                    daysElapsed = l.daysElapsed + 1,
                    missedPayments = max(0, l.missedPayments - 1)
                )
                if (updated.remainingPrincipal > 0.01) newActive += updated
                else notifications += notify(
                    "Préstamo finalizado",
                    "${l.type.displayName} de ${l.lenderName} pagado por completo.",
                    NotificationKind.SUCCESS
                )
            } else {
                val miss = l.missedPayments + 1
                if (miss >= LoanPenalties.DEFAULT_AT_MISSES) {
                    defaultsHappened++
                    company = company.copy(
                        reputation = (company.reputation - LoanPenalties.DEFAULT_REP_PENALTY)
                            .coerceIn(0, 100)
                    )
                    newActive += l.copy(defaulted = true, missedPayments = miss)
                    notifications += notify(
                        "Impago",
                        "${l.type.displayName} marcado como impagado.",
                        NotificationKind.ERROR
                    )
                } else {
                    newActive += l.copy(missedPayments = miss)
                    notifications += notify(
                        "Sin fondos",
                        "Cuota de ${l.lenderName} impagada ($miss/${LoanPenalties.DEFAULT_AT_MISSES}).",
                        NotificationKind.WARNING
                    )
                }
            }
        }

        val newLoans = loans.copy(
            active = newActive,
            totalLifetimeInterest = loans.totalLifetimeInterest + totalInterest,
            totalDefaults = loans.totalDefaults + defaultsHappened
        )
        val newNotifs = (state.notifications + notifications).takeLast(40)
        return state.copy(company = company, loans = newLoans, notifications = newNotifs)
    }

    // ----------------------- Helpers -----------------------

    /** Refresca ofertas si llevan demasiado tiempo paradas. */
    fun maybeRefreshOffers(state: GameState, rng: Random): GameState {
        val last = state.loans.lastOffersTick
        if (last < 0 || (state.tick - last) >= OFFER_REFRESH_TICKS) {
            return refreshOffers(state, rng)
        }
        return state
    }

    private fun estimateCashFlow(state: GameState): Double {
        val rentNet = state.realEstate.dailyNet
        val payroll = -state.company.totalSalaries / 30.0
        return rentNet + payroll
    }

    private fun notify(title: String, msg: String, kind: NotificationKind): GameNotification =
        GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = notify(title, msg, kind)
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
