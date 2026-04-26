package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de relaciones con NPCs. Permite mejorar amistad por interacción
 * directa, regalar (con coste), y simular encuentros aleatorios que
 * añaden frases memorables a la agenda.
 */
object NPCEngine {

    /** Mejora la amistad con un NPC. Si no se conocía, lo añade al estado. */
    fun improveRelationship(state: GameState, npcId: String, amount: Int): GameState {
        val npc = NPCCatalog.byId(npcId) ?: return state
        val s = state.npcs
        val rel = s.known[npcId] ?: NPCRelationship(
            npcId = npcId,
            quote = npc.signatureLine
        )
        val newRel = rel.copy(
            friendshipLevel = (rel.friendshipLevel + amount).coerceIn(0, 100),
            lastInteractionTick = state.tick,
            encounters = rel.encounters + 1
        )
        return state.copy(
            npcs = s.copy(known = s.known + (npcId to newRel))
        )
    }

    /**
     * Regalar a un NPC. Cuesta dinero corporativo y mejora la amistad
     * proporcionalmente al coste.
     */
    fun gift(state: GameState, npcId: String, gift: String, cost: Double): GameState {
        if (state.company.cash < cost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "No puedes regalar $gift, faltan ${"%,.0f".format(cost)} €.")
        }
        val npc = NPCCatalog.byId(npcId) ?: return state
        val s = state.npcs
        val rel = s.known[npcId] ?: NPCRelationship(npcId = npcId, quote = npc.signatureLine)
        val bump = when {
            cost >= 5_000 -> 25
            cost >= 1_000 -> 12
            cost >= 200 -> 6
            else -> 2
        }
        val newRel = rel.copy(
            friendshipLevel = (rel.friendshipLevel + bump).coerceIn(0, 100),
            lastInteractionTick = state.tick,
            encounters = rel.encounters + 1,
            gifted = rel.gifted + 1,
            quote = "Gracias por el ${gift.lowercase()}. Te debo una."
        )
        val company = state.company.copy(cash = state.company.cash - cost)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Regalo entregado",
            message = "${npc.name} agradece el detalle."
        )
        return state.copy(
            company = company,
            npcs = s.copy(known = s.known + (npcId to newRel)),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /**
     * Encuentro aleatorio: el motor central llama a esto periódicamente.
     * Elige un NPC del catálogo, le añade un encuentro y una frase
     * generada de forma sencilla a partir del tono.
     */
    fun encounter(state: GameState, npcId: String, rng: Random): GameState {
        val npc = NPCCatalog.byId(npcId) ?: return state
        val s = state.npcs
        val rel = s.known[npcId] ?: NPCRelationship(npcId = npcId, quote = npc.signatureLine)
        val quote = pickQuote(npc, rng)
        val newRel = rel.copy(
            lastInteractionTick = state.tick,
            encounters = rel.encounters + 1,
            quote = quote,
            friendshipLevel = (rel.friendshipLevel + 1).coerceIn(0, 100)
        )
        val notif = GameNotification(
            id = System.nanoTime() + npcId.hashCode().toLong(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "Te encuentras a ${npc.name}",
            message = "\"$quote\""
        )
        return state.copy(
            npcs = s.copy(known = s.known + (npcId to newRel)),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /**
     * Banco simple de quotes por tono. Lo justo para sentir variedad sin
     * inflar el binario. La firma del NPC siempre es la primera opción.
     */
    private fun pickQuote(npc: NPC, rng: Random): String {
        val pool = when (npc.voiceTone) {
            NPCTone.FORMAL -> listOf(
                npc.signatureLine,
                "Permítame insistir: el tiempo es nuestro activo más caro.",
                "Honorable, ¿concederá usted unos minutos?",
                "Las formas también son fondo.",
                "Le confieso que su trayectoria me inspira respeto."
            )
            NPCTone.FRIENDLY -> listOf(
                npc.signatureLine,
                "¿Cómo va eso, jefe? Cuando puedas te invito a una caña.",
                "Me alegro de verte. En serio, no es por compromiso.",
                "Te he traído pan, está calentito.",
                "Si necesitas algo, ya sabes dónde encontrarme."
            )
            NPCTone.AGGRESSIVE -> listOf(
                npc.signatureLine,
                "No sé si te has dado cuenta, pero te queda poco margen.",
                "El que avisa no es traidor. Yo aviso una vez.",
                "Tu tiempo se acaba más rápido que tu suerte.",
                "Lo bueno de los rivales es que se les puede medir."
            )
            NPCTone.SARCASTIC -> listOf(
                npc.signatureLine,
                "Vaya, mira quién tenemos por aquí. ¿También te has perdido?",
                "Qué casualidad coincidir contigo. Sospechosa, diría yo.",
                "He oído que estás triunfando. Felicidades, supongo.",
                "Las mejores ideas las tienes en mi tasca, eso te lo digo."
            )
            NPCTone.FLATTERING -> listOf(
                npc.signatureLine,
                "Su excelencia, qué placer cruzármelo de nuevo.",
                "Cada vez que le veo, su empresa parece más grande.",
                "Es usted la persona perfecta para esto.",
                "Permítame decirle que su olfato es legendario."
            )
            NPCTone.COLD -> listOf(
                npc.signatureLine,
                "Nada que no se solucione con papeles en regla.",
                "Tendrá usted un momento. Yo los tengo cronometrados.",
                "Si no me llama, mejor.",
                "Buenos días."
            )
        }
        return pool.random(rng)
    }

    /**
     * Disparador del motor: con baja probabilidad escoge un NPC al azar
     * (preferentemente uno ya conocido) para un encuentro casual.
     * Devuelve el estado tal cual si no hay encuentro.
     */
    fun maybeRandomEncounter(state: GameState, rng: Random): GameState {
        if (rng.nextDouble() > 0.18) return state // 18% por día
        val known = state.npcs.known.keys.toList()
        val candidate = if (known.isNotEmpty() && rng.nextDouble() < 0.7) {
            known.random(rng)
        } else {
            NPCCatalog.all.random(rng).id
        }
        return encounter(state, candidate, rng)
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
