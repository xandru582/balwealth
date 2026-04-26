package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Nóminas + costes fijos. Se aplican cada día in-game (1.440 ticks).
 * Si no hay liquidez se reducen lealtades y reputación.
 */
object Payroll {

    data class Result(
        val company: Company,
        val notifications: List<GameNotification> = emptyList()
    )

    fun applyDaily(company: Company, day: Int): Result {
        if (company.employees.isEmpty()) return Result(company)

        val dailyCost = company.totalSalaries / 30.0  // salario mensual / 30 días
        val notifs = mutableListOf<GameNotification>()

        return if (company.cash >= dailyCost) {
            Result(company.copy(cash = company.cash - dailyCost))
        } else {
            // impago: lealtad -0.1 a todos, reputación -1
            val payable = company.cash.coerceAtLeast(0.0)
            val newEmps = company.employees.map { it.withLoyalty(it.loyalty - 0.1) }
            val newRep = (company.reputation - 1).coerceAtLeast(0)
            notifs += GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.WARNING,
                title = "Nóminas incompletas",
                message = "No hay liquidez para pagar a todo el equipo. Lealtad y reputación bajan."
            )
            Result(
                company.copy(
                    cash = company.cash - payable,
                    employees = newEmps,
                    reputation = newRep
                ),
                notifs
            )
        }
    }

    /** Algunos empleados con lealtad muy baja dimiten. */
    fun churn(company: Company, rng: kotlin.random.Random): Result {
        val quitters = company.employees.filter { it.loyalty < 0.2 && rng.nextDouble() < 0.25 }
        if (quitters.isEmpty()) return Result(company)
        val notifs = quitters.map {
            GameNotification(
                id = System.nanoTime() + it.id.hashCode().toLong(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.WARNING,
                title = "Baja de empleado",
                message = "${it.name} ha dejado la empresa por descontento."
            )
        }
        val kept = company.employees - quitters.toSet()
        return Result(company.copy(employees = kept), notifs)
    }
}
