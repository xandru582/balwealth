package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * NPCs rivales — empresarios competidores del jugador.
 *
 * Cada rival tiene un arquetipo, un umbral de cash a superar para ser
 * vencido, una colección de pullas (taunts) y un retrato emoji.
 *
 * Cuando el jugador alcanza/duplica el cash del rival, queda derrotado y
 * recibe XP, reputación y un bono de cash.
 */

@Serializable
enum class RivalArchetype(val displayName: String, val emoji: String) {
    OLD_MONEY_BARON   ("Barón Viejo Dinero", "🎩"),
    TECH_DISRUPTOR    ("Disruptor Tech",     "💻"),
    GREEDY_TYCOON     ("Magnate Codicioso",  "🤑"),
    SHADY_DEALER      ("Tratante Turbio",    "🕴"),
    ECO_VISIONARY     ("Visionario Ecológico","🌱"),
    MAFIA_BOSS        ("Capo de la Mafia",   "🍝"),
    INHERITANCE_KID   ("Niño de la Herencia","👶"),
    IDEALIST_FOUNDER  ("Fundador Idealista", "🕊")
}

@Serializable
data class Rival(
    val id: String,
    val name: String,
    val archetype: RivalArchetype,
    val level: Int,
    val cash: Double,                       // umbral de cash a superar
    val reputation: Int,
    val traits: List<String>,
    val taunt: String,                      // frase principal
    val portrait: String,                   // emoji del retrato
    val defeated: Boolean = false,
    val rewardCash: Double = 0.0,
    val rewardXp: Long = 0,
    val rewardReputation: Int = 0,
    val trashTalkLines: List<String> = emptyList()
)

@Serializable
data class RivalsState(
    val active: List<Rival> = emptyList(),
    val defeated: List<Rival> = emptyList(),
    val currentChallenge: String? = null,    // id del rival más cercano al jugador
    val lastTrashTalk: String? = null
)

/**
 * Plantel inicial — 8 rivales únicos en español con personalidad diferenciada.
 * Ordenados por dificultad (cash umbral creciente).
 */
object RivalRoster {

    val initial: List<Rival> = listOf(
        Rival(
            id = "rival_marta",
            name = "Marta Echeverría",
            archetype = RivalArchetype.INHERITANCE_KID,
            level = 4,
            cash = 25_000.0,
            reputation = 35,
            traits = listOf("Heredera", "Caprichosa", "Sin esfuerzo"),
            taunt = "Mi abuelo construyó esto. Tú aún estás aprendiendo.",
            portrait = "👶",
            rewardCash = 2_500.0,
            rewardXp = 200,
            rewardReputation = 2,
            trashTalkLines = listOf(
                "¿Sigues con tu chiringuito? Qué tierno.",
                "Yo no trabajo. Heredo. Dos cosas distintas.",
                "Mi padre se reiría si viera tus números."
            )
        ),
        Rival(
            id = "rival_lucia",
            name = "Lucía \"Tech\" Romero",
            archetype = RivalArchetype.TECH_DISRUPTOR,
            level = 8,
            cash = 80_000.0,
            reputation = 55,
            traits = listOf("Ingeniera", "Disruptiva", "Visionaria"),
            taunt = "Tu modelo es analógico. Yo ya estoy en la nube.",
            portrait = "💻",
            rewardCash = 8_000.0,
            rewardXp = 400,
            rewardReputation = 3,
            trashTalkLines = listOf(
                "Mientras tú contratas, yo automatizo.",
                "¿Aún haces hojas de cálculo? Qué retro.",
                "Acabo de cerrar una serie B. Y tú… ¿qué?"
            )
        ),
        Rival(
            id = "rival_ricardo",
            name = "Don Ricardo Mendoza",
            archetype = RivalArchetype.OLD_MONEY_BARON,
            level = 14,
            cash = 250_000.0,
            reputation = 70,
            traits = listOf("Aristócrata", "Tradicional", "Con contactos"),
            taunt = "El dinero antiguo no compite con advenedizos.",
            portrait = "🎩",
            rewardCash = 25_000.0,
            rewardXp = 800,
            rewardReputation = 5,
            trashTalkLines = listOf(
                "Mi familia ya era rica cuando la suya pedía pan.",
                "Cómprate modales antes que cuota de mercado.",
                "El club no acepta a cualquiera, joven."
            )
        ),
        Rival(
            id = "rival_tiburon",
            name = "El Tiburón",
            archetype = RivalArchetype.SHADY_DEALER,
            level = 18,
            cash = 600_000.0,
            reputation = 40,
            traits = listOf("Sospechoso", "Sin escrúpulos", "Bien conectado"),
            taunt = "En mi terreno, los huesos no se entierran… se invierten.",
            portrait = "🕴",
            rewardCash = 60_000.0,
            rewardXp = 1_400,
            rewardReputation = 4,
            trashTalkLines = listOf(
                "Si te metes en mis aguas, no salgas.",
                "Te conozco. Te he visto en bares oscuros.",
                "El que paga, manda. Y aquí no pagas tú."
            )
        ),
        Rival(
            id = "rival_vega",
            name = "Vega Industries",
            archetype = RivalArchetype.GREEDY_TYCOON,
            level = 22,
            cash = 1_500_000.0,
            reputation = 60,
            traits = listOf("Despiadado", "Eficiente", "Insaciable"),
            taunt = "El mercado es mío. Tú solo eres ruido.",
            portrait = "🤑",
            rewardCash = 150_000.0,
            rewardXp = 2_500,
            rewardReputation = 6,
            trashTalkLines = listOf(
                "Cómprate competencia o cómprate paciencia.",
                "Hoy te trituro y mañana te olvido.",
                "Tu margen es ridículo. Cierra ya."
            )
        ),
        Rival(
            id = "rival_aleria",
            name = "Aleria Verde",
            archetype = RivalArchetype.ECO_VISIONARY,
            level = 25,
            cash = 3_500_000.0,
            reputation = 85,
            traits = listOf("Sostenible", "Carismática", "Idealista"),
            taunt = "Tu fábrica contamina. La mía cura el planeta.",
            portrait = "🌱",
            rewardCash = 350_000.0,
            rewardXp = 4_000,
            rewardReputation = 8,
            trashTalkLines = listOf(
                "Cuando crezcas, igual te haces verde como yo.",
                "Mi huella de carbono es negativa. ¿La tuya?",
                "El futuro es renovable. Lo tuyo, no."
            )
        ),
        Rival(
            id = "rival_donsavio",
            name = "Don Savio Corleone",
            archetype = RivalArchetype.MAFIA_BOSS,
            level = 30,
            cash = 8_000_000.0,
            reputation = 50,
            traits = listOf("Tradicional", "Familiar", "Implacable"),
            taunt = "Te haré una oferta que no podrás rechazar.",
            portrait = "🍝",
            rewardCash = 800_000.0,
            rewardXp = 7_500,
            rewardReputation = 7,
            trashTalkLines = listOf(
                "Esta ciudad es de la famiglia. Recuérdalo.",
                "El que no paga el respeto, paga otra cosa.",
                "Hablamos en el Cesare. Trae el dinero."
            )
        ),
        Rival(
            id = "rival_aurelio",
            name = "Aurelio Cruz",
            archetype = RivalArchetype.IDEALIST_FOUNDER,
            level = 35,
            cash = 20_000_000.0,
            reputation = 95,
            traits = listOf("Mítico", "Filántropo", "Inspirador"),
            taunt = "He construido un imperio sin perder los valores.",
            portrait = "🕊",
            rewardCash = 2_000_000.0,
            rewardXp = 15_000,
            rewardReputation = 12,
            trashTalkLines = listOf(
                "Algún día sabrás lo que es construir con propósito.",
                "El éxito sin ética es solo ruido.",
                "Te respeto, pero aún no estás a la altura."
            )
        )
    )

    val byId: Map<String, Rival> = initial.associateBy { it.id }

    fun freshState(): RivalsState = RivalsState(
        active = initial,
        defeated = emptyList(),
        currentChallenge = initial.firstOrNull()?.id,
        lastTrashTalk = null
    )

    /** Devuelve una pulla aleatoria del rival actual del jugador (o null si no hay). */
    fun trashTalkFor(state: RivalsState, rng: Random): String? {
        val current = state.active.find { it.id == state.currentChallenge }
            ?: state.active.firstOrNull()
            ?: return null
        val lines = current.trashTalkLines
        if (lines.isEmpty()) return current.taunt
        val line = lines.random(rng)
        return "${current.name} (${current.archetype.displayName}): \"$line\""
    }
}
