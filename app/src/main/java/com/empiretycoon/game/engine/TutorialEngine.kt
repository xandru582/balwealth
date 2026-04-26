package com.empiretycoon.game.engine

import com.empiretycoon.game.model.AdvanceCondition
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.TutorialScript
import com.empiretycoon.game.model.TutorialState
import com.empiretycoon.game.model.TutorialStep

/**
 * Motor del tutorial. Compara dos snapshots consecutivas de `GameState`
 * y avanza al siguiente paso si la condición del paso actual se cumple.
 *
 * Es puro: no realiza efectos secundarios.
 */
object TutorialEngine {

    /** Orden lineal de pasos (excluyendo FINISHED, que es terminal). */
    private val ORDER: List<TutorialStep> = TutorialStep.values().toList()

    /**
     * Llama a esto tras CADA mutación del estado para evaluar el avance.
     * Si el tutorial está saltado o ya terminó, devuelve `next` sin tocar.
     */
    fun checkAdvance(prev: GameState, next: GameState): GameState {
        val tutorial = next.tutorial
        if (tutorial.skipped || tutorial.isFinished) return next

        val spec = TutorialScript.specOf(tutorial.currentStep)
        val advanced = when (spec.advanceCondition) {
            AdvanceCondition.TAP_PRIMARY -> false  // se gestiona desde la UI
            AdvanceCondition.TAP_TAB -> false      // se gestiona desde la UI
            AdvanceCondition.BUILT_BUILDING ->
                next.company.buildings.size > prev.company.buildings.size
            AdvanceCondition.RECIPE_ASSIGNED ->
                hasNewRecipeAssignment(prev, next)
            AdvanceCondition.EMPLOYEE_HIRED ->
                next.company.employees.size > prev.company.employees.size
            AdvanceCondition.WORKER_ASSIGNED ->
                next.company.totalWorkers > prev.company.totalWorkers
            AdvanceCondition.MARKET_TX ->
                marketChanged(prev, next)
            AdvanceCondition.RESEARCH_STARTED ->
                prev.research.inProgressId == null && next.research.inProgressId != null
            AdvanceCondition.STAT_TRAINED ->
                prev.player.stats.total < next.player.stats.total
            AdvanceCondition.STOCK_BOUGHT ->
                stocksIncreased(prev, next)
            AdvanceCondition.PROPERTY_BOUGHT ->
                next.realEstate.owned.size > prev.realEstate.owned.size
        }

        if (!advanced) return next
        return next.copy(tutorial = advanceStep(tutorial))
    }

    /** Avance manual usado por la UI (TAP_PRIMARY, TAP_TAB, etc.). */
    fun advanceManually(state: GameState, fromStep: TutorialStep? = null): GameState {
        val tut = state.tutorial
        if (tut.skipped || tut.isFinished) return state
        if (fromStep != null && tut.currentStep != fromStep) return state
        return state.copy(tutorial = advanceStep(tut))
    }

    /** Marca un paso como completado sin obligar al avance global. */
    fun markCompleted(state: GameState, step: TutorialStep): GameState {
        val tut = state.tutorial
        if (tut.completedSteps.contains(step)) return state
        return state.copy(
            tutorial = tut.copy(completedSteps = tut.completedSteps + step)
        )
    }

    /** El jugador ha pulsado "Saltar tutorial". */
    fun skip(state: GameState): GameState =
        state.copy(
            tutorial = state.tutorial.copy(
                skipped = true,
                currentStep = TutorialStep.FINISHED
            )
        )

    /** Reinicia el tutorial al primer paso. */
    fun restart(state: GameState): GameState =
        state.copy(tutorial = TutorialState())

    /** Incrementa el contador de cierres del coachmark sin avanzar. */
    fun bumpDismiss(state: GameState): GameState =
        state.copy(
            tutorial = state.tutorial.copy(
                dismissCount = state.tutorial.dismissCount + 1
            )
        )

    // ---------- Helpers privados ----------

    private fun advanceStep(tut: TutorialState): TutorialState {
        val idx = ORDER.indexOf(tut.currentStep)
        val next = if (idx >= 0 && idx < ORDER.lastIndex) ORDER[idx + 1]
        else TutorialStep.FINISHED
        return tut.copy(
            currentStep = next,
            completedSteps = tut.completedSteps + tut.currentStep
        )
    }

    private fun hasNewRecipeAssignment(prev: GameState, next: GameState): Boolean {
        val prevAssigned = prev.company.buildings.count { it.currentRecipeId != null }
        val nextAssigned = next.company.buildings.count { it.currentRecipeId != null }
        return nextAssigned > prevAssigned
    }

    private fun marketChanged(prev: GameState, next: GameState): Boolean {
        // FIX BUG-08-07: el check anterior se disparaba con payroll/eventos/producción
        // (cualquier delta de cash + inventario lo activaba). Ahora exigimos:
        // - Cambio simultáneo en CASH (positivo o negativo) Y
        // - Cantidad TOTAL de inventario diferente (no solo composición)
        // - Y que el delta de cash NO coincida con un patrón típico de payroll
        //   (los payrolls se aplican cada 1.440 ticks).
        if (prev.company.cash == next.company.cash) return false
        val prevTotal = prev.company.inventory.values.sum()
        val nextTotal = next.company.inventory.values.sum()
        if (prevTotal == nextTotal) return false
        // Si el tick saltó por boundary diaria, ignorar (probable payroll/dividendos)
        if (next.tick % 1_440L == 0L) return false
        return true
    }

    private fun stocksIncreased(prev: GameState, next: GameState): Boolean {
        val prevTotal = prev.holdings.shares.values.sum()
        val nextTotal = next.holdings.shares.values.sum()
        return nextTotal > prevTotal
    }
}
