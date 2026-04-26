package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de líneas de producción. Orquesta:
 *  - Asignación de receta a cada edificio de la línea.
 *  - Compra automática de insumos cuando faltan (si la política lo permite).
 *  - Balanceo entre edificios según `BalancingMode`.
 *
 * Diseñado para ser invocado desde el GameEngine en cada tick (o cada
 * cierto número de ticks, p.ej. cada 5s) sin colisionar con `Production`:
 * sólo modifica cosas que `Production` no toca (cambia receta, ajusta
 * autoRestart, llama a marketBuy).
 */
object ProductionLinesEngine {

    /**
     * Crea una línea a partir de un preset y una asignación concreta de
     * edificios. La asignación debe respetar el orden de
     * `preset.requiredBuildingTypes` y los tipos deben coincidir.
     */
    fun createLine(
        state: GameState,
        preset: LinePreset,
        buildingIds: List<String>,
        nameOverride: String? = null
    ): GameState {
        if (buildingIds.size != preset.requiredBuildingTypes.size) {
            return notifyErr(
                state,
                "Línea inválida",
                "La cantidad de edificios no coincide con el preset."
            )
        }
        // Validar que cada edificio existe y es del tipo requerido
        val ownedById = state.company.buildings.associateBy { it.id }
        for ((idx, bid) in buildingIds.withIndex()) {
            val b = ownedById[bid] ?: return notifyErr(
                state,
                "Línea inválida",
                "Edificio no encontrado: $bid"
            )
            val expected = preset.requiredBuildingTypes[idx]
            if (b.type != expected) {
                return notifyErr(
                    state,
                    "Tipo de edificio incorrecto",
                    "${b.type.displayName} no encaja como ${expected.displayName}."
                )
            }
        }
        val recipeMap = preset.recipeChain.zip(buildingIds)
            .associate { (rid, bid) -> bid to rid }
        val line = ProductionLine(
            id = "line_${state.tick}_${state.productionLines.lines.size}",
            name = nameOverride ?: preset.name,
            buildingIds = buildingIds,
            recipeIdsPerBuilding = recipeMap,
            balancingModeName = preset.recommendedBalancing.name,
            enabled = true,
            createdTick = state.tick
        )
        return state.copy(
            productionLines = state.productionLines.upsert(line)
        ).let { applyLineRecipes(it, line) }
    }

    /** Habilita o deshabilita una línea. Si se deshabilita, se respeta lo que tenían. */
    fun toggleLine(state: GameState, lineId: String): GameState =
        state.copy(productionLines = state.productionLines.toggle(lineId))

    /** Borra la línea (no toca recetas asignadas — el jugador puede dejarlas). */
    fun deleteLine(state: GameState, lineId: String): GameState =
        state.copy(productionLines = state.productionLines.remove(lineId))

    /**
     * Avance global de líneas. Llamar tras `Production.advance` desde
     * GameEngine. No realiza producción real, solo orquesta:
     *   - Reaplica la receta correcta al edificio si alguien la cambió.
     *   - Compra insumos si la política lo pide y hay caja.
     *   - Ajusta autoRestart según balancing mode.
     */
    fun tickLines(state: GameState, rng: Random): GameState {
        if (state.productionLines.lines.isEmpty()) return state
        var s = state
        for (line in state.productionLines.lines) {
            if (!line.enabled) continue
            s = applyLineRecipes(s, line)
            s = adjustBalancing(s, line)
            s = autoBuyInputsIfNeeded(s, line, rng)
        }
        return s
    }

    // ---------- Helpers internos ----------

    private fun applyLineRecipes(state: GameState, line: ProductionLine): GameState {
        var s = state
        for (bid in line.buildingIds) {
            val want = line.recipeIdsPerBuilding[bid] ?: continue
            val b = s.company.buildings.firstOrNull { it.id == bid } ?: continue
            if (b.currentRecipeId != want) {
                val newBs = s.company.buildings.map {
                    if (it.id == bid) it.copy(
                        currentRecipeId = want,
                        progressSeconds = 0.0
                    ) else it
                }
                s = s.copy(company = s.company.copy(buildings = newBs))
            }
        }
        return s
    }

    private fun adjustBalancing(state: GameState, line: ProductionLine): GameState {
        val target = when (line.balancingMode) {
            BalancingMode.JUST_IN_TIME -> true
            BalancingMode.BUFFER_HEAVY -> true
            BalancingMode.MAX_THROUGHPUT -> true
        }
        // El motor de líneas siempre quiere autoRestart ON; el balancing real
        // afecta a la "agresividad" de la autocompra (ver autoBuy).
        val newBs = state.company.buildings.map {
            if (it.id in line.buildingIds && it.autoRestart != target) {
                it.copy(autoRestart = target)
            } else it
        }
        return state.copy(company = state.company.copy(buildings = newBs))
    }

    private fun autoBuyInputsIfNeeded(
        state: GameState,
        line: ProductionLine,
        rng: Random
    ): GameState {
        // Sólo autocompramos los insumos del PRIMER edificio de la cadena
        // (el resto se nutre de los outputs anteriores).
        val firstBid = line.buildingIds.firstOrNull() ?: return state
        val policy = state.productionPolicies.policyFor(firstBid)
        if (!policy.autoBuyInputs) return state
        val recipeId = line.recipeIdsPerBuilding[firstBid] ?: return state
        val recipe = AdvancedRecipeCatalog.byId(recipeId) ?: return state

        var s = state
        // Multiplicador "agresividad" según balancing
        val targetStock = when (line.balancingMode) {
            BalancingMode.JUST_IN_TIME -> 1
            BalancingMode.BUFFER_HEAVY -> 4
            BalancingMode.MAX_THROUGHPUT -> 2
        }

        for ((resId, qty) in recipe.inputs) {
            val have = s.inventoryOf(resId)
            val needed = (qty * targetStock) - have
            if (needed <= 0) continue
            // Comprueba caja suficiente
            val unitCost = s.market.buyPriceOf(resId)
            val canAfford = (s.company.cash / unitCost.coerceAtLeast(0.01)).toInt()
            val toBuy = minOf(needed, canAfford).coerceAtLeast(0)
            if (toBuy <= 0) continue
            val before = s
            s = GameEngine.marketBuy(s, resId, toBuy)
            // Si la compra falló (no hay sitio o fondos), no insistas con este recurso
            if (s.company.cash >= before.company.cash) break
        }
        return s
    }

    private fun notifyErr(state: GameState, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.ERROR,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
