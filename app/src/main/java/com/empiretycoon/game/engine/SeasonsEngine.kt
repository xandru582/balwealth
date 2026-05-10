package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Motor de Event Seasons. Pure GameState -> GameState. No toca render
 * directamente — solo expone la temporada activa y sus modificadores
 * via state.seasons.activeModifiers. Otros engines pueden leerlos.
 *
 * Ciclo:
 *  - tickDaily ejecutado al cambio de día in-game.
 *  - Detecta cambio de temporada por cycleDay.
 *  - Cuando se completa una temporada (último día), entrega la reward
 *    correspondiente si es la primera vez.
 */
object SeasonsEngine {

    /** Llamar al cambio de día in-game (1.440 ticks). */
    fun tickDaily(state: GameState): GameState {
        val s = state.seasons
        val nowDay = state.day
        if (nowDay == s.lastTickedDay) return state

        val cycleDay = ((nowDay - 1) % SeasonsCatalog.CYCLE_DAYS) + 1
        val newActive = SeasonsCatalog.seasonForDay(cycleDay)
        val prevActive = s.activeSeason

        var working = state.copy(seasons = s.copy(
            cycleDay = cycleDay,
            activeSeasonName = newActive.name,
            lastTickedDay = nowDay
        ))

        // Si la temporada cambió, notificamos.
        if (prevActive != newActive) {
            working = notify(working, NotificationKind.INFO,
                "${newActive.emoji} Temporada: ${newActive.displayName}",
                newActive.tagline)

            // Si la temporada saliente no era OFFSEASON, marcamos completada
            // y entregamos la reward (solo la primera vez lifetime).
            if (prevActive != SeasonId.OFFSEASON) {
                working = grantSeasonReward(working, prevActive)
            }
        }

        return working
    }

    private fun grantSeasonReward(state: GameState, season: SeasonId): GameState {
        val s = state.seasons
        // ¿Ya tiene la reward de esta temporada? (1 por temporada lifetime)
        val alreadyClaimed = s.claimedRewards.any { it.seasonId == season }
        val newCount = (s.completedCount[season.name] ?: 0) + 1
        val newCounts = s.completedCount + (season.name to newCount)
        if (alreadyClaimed) {
            return state.copy(seasons = s.copy(completedCount = newCounts))
        }

        val template = SeasonsCatalog.rewardTemplate(season)
        val newCash = state.company.cash + template.cashBonus
        val newPlayer = state.player.addXp(template.xpBonus)
        val notif = "${template.title}: +${"%,.0f".format(template.cashBonus)} € · +${template.xpBonus} XP."

        return notify(
            state.copy(
                company = state.company.copy(cash = newCash),
                player = newPlayer,
                seasons = s.copy(
                    completedCount = newCounts,
                    claimedRewards = (s.claimedRewards + template).takeLast(20)
                )
            ),
            NotificationKind.SUCCESS,
            "🎁 Recompensa de temporada",
            notif
        )
    }

    /** Helper: días que faltan para que termine la temporada actual. */
    fun daysLeftInActiveSeason(state: GameState): Int {
        val cycleDay = state.seasons.cycleDay
        val active = state.seasons.activeSeason
        val end = active.startDay + active.durationDays - 1
        return (end - cycleDay).coerceAtLeast(0)
    }

    /** Helper: días hasta una temporada futura concreta. */
    fun daysUntil(state: GameState, target: SeasonId): Int {
        val cycleDay = state.seasons.cycleDay
        val targetStart = target.startDay
        return if (targetStart >= cycleDay) targetStart - cycleDay
            else SeasonsCatalog.CYCLE_DAYS - cycleDay + targetStart
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
