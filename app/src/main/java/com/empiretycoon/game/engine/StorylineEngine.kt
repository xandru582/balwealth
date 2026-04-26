package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Motor del arco narrativo principal. Auto-avanza capítulos cuando se
 * cumplen los requisitos, aplica los efectos de las elecciones y resuelve
 * el final cuando el capítulo 12 se completa.
 */
object StorylineEngine {

    /**
     * Comprueba si el capítulo actual cumple sus requisitos para
     * mostrar la cinematica. Si el capítulo actual ya está en
     * `completedChapters`, avanza al siguiente. No aplica efectos.
     */
    fun checkChapterTrigger(state: GameState): GameState {
        val s = state.storyline
        // Si el capítulo actual ya está completado, busca el siguiente
        // capítulo elegible y lo deja como "currentChapterId".
        val currentChap = StoryArc.byId(s.currentChapterId)
        if (currentChap == null) return state

        if (s.completedChapters.contains(s.currentChapterId)) {
            val next = StoryArc.nextOf(s.currentChapterId) ?: return state
            return state.copy(storyline = s.copy(currentChapterId = next.id))
        }
        return state
    }

    /**
     * Determina si el capítulo actual es jugable ahora mismo (cumple
     * requisitos). Útil para la UI: solo se abre el modal si esto es true.
     */
    fun isChapterPlayable(state: GameState): Boolean {
        val s = state.storyline
        if (s.completedChapters.contains(s.currentChapterId)) return false
        val ch = StoryArc.byId(s.currentChapterId) ?: return false
        val req = ch.requirements
        return state.player.level >= req.minLevel &&
            state.company.cash >= req.minCash &&
            state.day - 1 >= req.daysSinceStart &&
            (req.requiredQuestId == null ||
                state.quests.any { it.id == req.requiredQuestId && it.claimed })
    }

    /**
     * Aplica una elección del capítulo actual: aplica los efectos,
     * recalcula karma y avanza al siguiente capítulo (o se queda esperando).
     */
    fun resolveStoryChoice(state: GameState, choiceIndex: Int): GameState {
        val s = state.storyline
        val ch = StoryArc.byId(s.currentChapterId) ?: return state
        val choice = ch.choices.getOrNull(choiceIndex) ?: return state

        var company = state.company
        var player = state.player
        var alignments = HashMap(s.alignments)
        var karmaDelta = 0
        val unlocked = mutableListOf<String>()

        for (e in choice.effects) {
            when (e) {
                is StoryEffect.CashDelta -> company = company.copy(
                    cash = company.cash + e.amount
                )
                is StoryEffect.ReputationDelta -> company = company.copy(
                    reputation = (company.reputation + e.d).coerceIn(0, 100)
                )
                is StoryEffect.HappinessDelta -> player = player.withHappiness(e.d)
                is StoryEffect.KarmaDelta -> karmaDelta += e.d
                is StoryEffect.EnergyDelta -> player = player.withEnergy(e.d)
                is StoryEffect.UnlockAchievement -> unlocked += e.id
                is StoryEffect.GivePerk -> { /* Perk system existe ya, queda como hook */ }
                is StoryEffect.DamageRival -> {
                    val curr = alignments[e.id] ?: 0
                    alignments[e.id] = (curr - e.amount).coerceIn(-100, 100)
                }
                is StoryEffect.AlignWithNPC -> {
                    val curr = alignments[e.id] ?: 0
                    alignments[e.id] = (curr + e.amount).coerceIn(-100, 100)
                }
            }
        }

        val newKarma = (s.karma + karmaDelta).coerceIn(-100, 100)
        val newChoicesMade = s.choicesMade + (s.currentChapterId to choiceIndex)
        val newCompleted = s.completedChapters + s.currentChapterId
        val nextChap = StoryArc.nextOf(s.currentChapterId)

        val newStoryline = s.copy(
            karma = newKarma,
            alignments = alignments,
            choicesMade = newChoicesMade,
            completedChapters = newCompleted,
            currentChapterId = nextChap?.id ?: s.currentChapterId
        )

        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.EVENT,
            title = "Capítulo: ${ch.title}",
            message = choice.consequenceText
        )

        return state.copy(
            company = company,
            player = player,
            storyline = newStoryline,
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /**
     * Si el último capítulo se ha completado y se cumplen los requisitos
     * de algún final, devuélvelo. Si no, null.
     *
     * Prioridad: el primero del catálogo que matche. Endings.kt está ya
     * ordenado de "más exigente" a "más permisivo" para evitar empates.
     */
    fun checkEndingEligible(state: GameState): Ending? {
        val s = state.storyline
        val lastChapterId = StoryArc.chapters.lastOrNull()?.id ?: return null
        if (!s.completedChapters.contains(lastChapterId)) return null

        // Resolver primero el secret y los más exigentes (ordenados manualmente):
        val priority = listOf(
            EndingType.SECRET,
            EndingType.IMMORTAL,
            EndingType.SAINT,
            EndingType.PHILANTROPIST,
            EndingType.REVOLUTIONARY,
            EndingType.TYRANT,
            EndingType.PRISONER,
            EndingType.EXILED,
            EndingType.RECLUSE,
            EndingType.JESTER,
            EndingType.BURNOUT
        )
        for (t in priority) {
            val e = EndingCatalog.byType(t) ?: continue
            if (matches(state, e.requirements)) return e
        }
        return null
    }

    private fun matches(state: GameState, req: EndingRequirement): Boolean {
        val s = state.storyline
        if (state.day - 1 < req.minDays) return false
        if (s.karma < req.karmaMin) return false
        if (s.karma > req.karmaMax) return false
        if (state.company.reputation < req.minReputation) return false

        val pattern = req.choicesPattern
        if (pattern != null) {
            for (entry in pattern) {
                val (chId, idxStr) = entry.split(":", limit = 2).let {
                    if (it.size != 2) return false
                    it[0] to it[1]
                }
                val idx = idxStr.toIntOrNull() ?: return false
                val made = s.choicesMade[chId] ?: return false
                if (made != idx) return false
            }
        }
        // Achievements son opcionales — si están definidos, deben estar
        // todos en achievements.unlocked. Si no hay sistema de achievements
        // poblado todavía, se consideran no cumplidos.
        if (req.achievementsRequired.isNotEmpty()) {
            val unlocked = state.achievements.unlocked
            for (id in req.achievementsRequired) {
                if (!unlocked.contains(id)) return false
            }
        }
        return true
    }

    /**
     * Karma derivado de las elecciones tomadas hasta ahora. Sirve como
     * fallback de auditoría: si por alguna razón el campo `karma` se
     * desincronizara con `choicesMade`, este recálculo te pone al día.
     */
    fun computeKarma(state: GameState): Int {
        val s = state.storyline
        var k = 0
        for ((chId, idx) in s.choicesMade) {
            val ch = StoryArc.byId(chId) ?: continue
            val ch2 = ch.choices.getOrNull(idx) ?: continue
            for (eff in ch2.effects) {
                if (eff is StoryEffect.KarmaDelta) k += eff.d
            }
        }
        return k.coerceIn(-100, 100)
    }
}
