package com.empiretycoon.game.engine

import com.empiretycoon.game.model.GameNotification
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.NotificationKind
import com.empiretycoon.game.model.PerksState
import com.empiretycoon.game.model.PerkCatalog
import com.empiretycoon.game.model.RivalsState
import com.empiretycoon.game.model.Skill
import com.empiretycoon.game.model.SkillEffect
import com.empiretycoon.game.model.SkillTreeCatalog
import com.empiretycoon.game.model.SkillTreeState

/**
 * Motor del árbol de habilidades.
 *
 * Mantiene una API funcional `GameState -> GameState` (consistente con el
 * resto del proyecto). Asume que `state.skillTree`, `state.perks` y
 * `state.rivals` existen como nuevos campos en GameState (ver patches).
 */
object SkillEngine {

    /**
     * Intenta desbloquear una habilidad. Devuelve el estado modificado
     * (con notificación de error si no se cumplen requisitos).
     */
    fun unlockSkill(state: GameState, skillId: String): GameState {
        val skill = SkillTreeCatalog.byId[skillId] ?: return notify(
            state, NotificationKind.ERROR, "Habilidad", "ID desconocido."
        )
        val tree = state.skillTree
        if (tree.has(skillId)) return state

        if (tree.availablePoints < skill.cost) return notify(
            state, NotificationKind.WARNING, "Sin puntos",
            "Necesitas ${skill.cost} punto(s) para ${skill.name}."
        )

        val missing = skill.prerequisites.filterNot { it in tree.unlockedSkills }
        if (missing.isNotEmpty()) return notify(
            state, NotificationKind.WARNING, "Requisitos",
            "Falta desbloquear ${missing.size} habilidad(es) previas."
        )

        val newTree = tree.copy(
            unlockedSkills = tree.unlockedSkills + skillId,
            availablePoints = tree.availablePoints - skill.cost
        )

        // efectos one-shot (cash inicial, etc.) los aplicamos en el momento
        var newState = state.copy(skillTree = newTree)
        newState = applyOneShotEffects(newState, skill.effects)
        return notify(
            newState, NotificationKind.SUCCESS,
            "Habilidad desbloqueada",
            "${skill.emoji} ${skill.name}"
        )
    }

    /**
     * Calcula los puntos otorgados al subir de nivel.
     * Regla: +1 por nivel + 1 extra cada 5 niveles.
     */
    fun grantSkillPointsOnLevel(
        state: GameState,
        prevLevel: Int,
        newLevel: Int
    ): Pair<GameState, Int> {
        if (newLevel <= prevLevel) return state to 0
        var earned = 0
        for (lvl in (prevLevel + 1)..newLevel) {
            earned += 1
            if (lvl % 5 == 0) earned += 1
        }
        val tree = state.skillTree.copy(
            availablePoints = state.skillTree.availablePoints + earned,
            totalEarnedPoints = state.skillTree.totalEarnedPoints + earned
        )
        return state.copy(skillTree = tree) to earned
    }

    /**
     * Agrega todos los efectos del jugador (skills + perks) en un mapa
     * por clave canónica.
     */
    fun aggregateEffects(state: GameState): Map<String, Double> {
        val effects = mutableListOf<SkillEffect>()
        for (id in state.skillTree.unlockedSkills) {
            SkillTreeCatalog.byId[id]?.effects?.let(effects::addAll)
        }
        for (id in state.perks.ownedPerks) {
            PerkCatalog.byId[id]?.effects?.let(effects::addAll)
        }
        return aggregate(effects)
    }

    /** Versión sin estado: agrega de una lista cualquiera de efectos. */
    fun aggregate(effects: List<SkillEffect>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        var happinessFloor = 0
        val flags = mutableSetOf<String>()
        for (e in effects) {
            when (e) {
                is SkillEffect.ProductionBonus  -> add(out, SkillEffect.KEY_PRODUCTION, e.pct)
                is SkillEffect.MarketSellBonus  -> add(out, SkillEffect.KEY_MARKET_SELL, e.pct)
                is SkillEffect.MarketBuyDiscount-> add(out, SkillEffect.KEY_MARKET_BUY, e.pct)
                is SkillEffect.ResearchSpeedup  -> add(out, SkillEffect.KEY_RESEARCH, e.pct)
                is SkillEffect.EnergyRegen      -> add(out, SkillEffect.KEY_ENERGY_REGEN, e.perTick)
                is SkillEffect.HappinessFloor   -> happinessFloor = maxOf(happinessFloor, e.floor)
                is SkillEffect.EmployeeSalaryReduction -> add(out, SkillEffect.KEY_SALARY_REDUCTION, e.pct)
                is SkillEffect.RealEstateRent   -> add(out, SkillEffect.KEY_REAL_ESTATE, e.pct)
                is SkillEffect.StockGain        -> add(out, SkillEffect.KEY_STOCK_GAIN, e.pct)
                is SkillEffect.EventLuck        -> add(out, SkillEffect.KEY_EVENT_LUCK, e.pct)
                is SkillEffect.TickXp           -> add(out, SkillEffect.KEY_TICK_XP, e.amount)
                is SkillEffect.CashStartBonus   -> add(out, SkillEffect.KEY_CASH_START, e.cash)
                is SkillEffect.MaxEnergyBonus   -> add(out, SkillEffect.KEY_MAX_ENERGY, e.amount.toDouble())
                is SkillEffect.BuildingDiscount -> add(out, SkillEffect.KEY_BUILDING_DISCOUNT, e.pct)
                is SkillEffect.CustomFlag       -> flags += e.name
            }
        }
        out[SkillEffect.KEY_HAPPINESS_FLOOR] = happinessFloor.toDouble()
        // los flags no se suman: 1.0 si está, 0.0 si no.
        flags.forEach { out["flag_$it"] = 1.0 }
        return out
    }

    fun hasFlag(effects: Map<String, Double>, name: String): Boolean =
        (effects["flag_$name"] ?: 0.0) > 0.0

    // -------------------------------------------------- helpers

    /**
     * Aplica los efectos que solo tienen sentido como "one-shot": cash al
     * inicio, energía máxima (que sí recalculamos al sumar), etc.
     * El resto se consultan dinámicamente por aggregateEffects.
     */
    private fun applyOneShotEffects(state: GameState, effects: List<SkillEffect>): GameState {
        var s = state
        for (e in effects) {
            when (e) {
                is SkillEffect.CashStartBonus -> {
                    s = s.copy(company = s.company.copy(cash = s.company.cash + e.cash))
                }
                is SkillEffect.MaxEnergyBonus -> {
                    val p = s.player.copy(
                        maxEnergy = s.player.maxEnergy + e.amount,
                        energy = s.player.energy + e.amount
                    )
                    s = s.copy(player = p)
                }
                else -> Unit
            }
        }
        return s
    }

    private fun add(map: MutableMap<String, Double>, key: String, value: Double) {
        map[key] = (map[key] ?: 0.0) + value
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
