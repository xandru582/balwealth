package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Motor del sistema de prestigio (renacimiento). Las funciones son puras.
 */
object PrestigeEngine {

    /**
     * Puntos de prestigio que el jugador ganaría si renaciera ahora mismo.
     * Crece logarítmicamente con la riqueza acumulada y el progreso del
     * imperio para evitar inflación con grandes balances.
     *
     * Formula: floor( ln(1 + cash/1e6) * sqrt(buildings + 1) * (1 + level/20) ).
     */
    fun computePoints(state: GameState): Long {
        val cash = max(0.0, state.company.cash + state.realEstate.totalValue)
        val base = ln(1.0 + cash / 1_000_000.0)
        val nBuildings = state.company.buildings.size.toDouble()
        val empireFactor = (nBuildings + 1.0).pow(0.5)
        val levelFactor = 1.0 + state.player.level / 20.0
        val raw = base * empireFactor * levelFactor
        return raw.toLong().coerceAtLeast(0L)
    }

    /**
     * Aplica los efectos *iniciales* de los perks comprados al estado.
     * Se invoca una sola vez tras un renacer (o al cargar un save legacy).
     * No multiplica por 2 si se llama varias veces porque marca el flag
     * `__perks_applied` en el progreso de logros.
     */
    fun applyPerks(state: GameState): GameState {
        // marcador para idempotencia
        val already = state.achievements.progressMap["__perks_applied"] == 1L
        if (already) return state

        var company = state.company
        var player = state.player

        val perks = state.prestige.perks.mapNotNull { PrestigePerkCatalog.byId(it) }

        // dinero inicial
        var startBonus = 0.0
        for (p in perks) {
            if (p.id == "perk_starter_50k") startBonus += 50_000.0
            if (p.id == "perk_starter_500k") startBonus += 500_000.0
        }
        if (startBonus > 0) {
            company = company.copy(cash = company.cash + startBonus)
        }

        // capacidad de almacenamiento
        if (perks.any { it.id == "perk_storage_2x" }) {
            company = company.copy(storageCapacity = company.storageCapacity * 2)
        }

        // empresa con nivel inicial
        if (perks.any { it.id == "perk_starter_level" }) {
            company = company.copy(level = max(company.level, 3))
        }

        val newProgress = HashMap(state.achievements.progressMap)
        newProgress["__perks_applied"] = 1L
        val ach = state.achievements.copy(progressMap = newProgress)

        return state.copy(company = company, player = player, achievements = ach)
    }

    /**
     * Renacimiento: reseta gran parte del juego pero conserva el prestigio
     * (puntos, nivel, perks) y métricas vitalicias. Re-aplica perks iniciales.
     */
    fun rebirth(state: GameState): GameState {
        val newPoints = computePoints(state)
        val updatedPrestige = state.prestige.copy(
            prestigeLevel = state.prestige.prestigeLevel + 1,
            prestigePoints = state.prestige.prestigePoints + newPoints,
            totalPointsEarned = state.prestige.totalPointsEarned + newPoints,
            lifetimeCash = state.prestige.lifetimeCash + state.company.cash,
            lifetimeProductionUnits =
                state.prestige.lifetimeProductionUnits + state.company.inventoryCount()
        )

        // resetear marcadores internos de progreso para que applyPerks vuelva a actuar.
        val resetProgress = state.achievements.progressMap
            .filterKeys { !it.startsWith("__") }
        val carriedAchievements = state.achievements.copy(progressMap = resetProgress)

        val freshState = GameState(
            tick = 0,
            lastRealTimeMs = System.currentTimeMillis(),
            player = Player(name = state.player.name),
            company = Company(name = state.company.name, slogan = state.company.slogan),
            market = Market.fresh(),
            research = ResearchState(),
            realEstate = RealEstatePortfolio(),
            stocks = StockCatalog.starter(),
            holdings = StockHoldings(),
            quests = QuestCatalog.all,
            notifications = listOf(
                GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = "Renacimiento",
                    message = "+$newPoints puntos de prestigio. Tu imperio comienza de nuevo."
                )
            ),
            rngSeed = System.currentTimeMillis(),
            achievements = carriedAchievements,
            prestige = updatedPrestige
        )
        return applyPerks(freshState)
    }

    /**
     * Compra un perk si hay puntos suficientes y aún no se posee.
     */
    fun buyPerk(state: GameState, perkId: String): GameState {
        val p = PrestigePerkCatalog.byId(perkId) ?: return state
        if (state.prestige.owns(perkId)) return state
        if (state.prestige.prestigePoints < p.cost) {
            val n = GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.WARNING,
                title = "Puntos insuficientes",
                message = "Necesitas ${p.cost} puntos de prestigio para ${p.name}."
            )
            return state.copy(
                notifications = (state.notifications + n).takeLast(40)
            )
        }
        val newPrestige = state.prestige.copy(
            prestigePoints = state.prestige.prestigePoints - p.cost,
            perks = state.prestige.perks + perkId
        )
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Perk adquirido",
            message = "${p.emoji} ${p.name} activo."
        )
        return state.copy(
            prestige = newPrestige,
            notifications = (state.notifications + notif).takeLast(40)
        )
    }
}
