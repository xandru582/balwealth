package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Asistente IA in-game (v17 — AICompanionEngine).
 *
 * Filosofía: NO es una "IA" remota ni habla con servidores. Es una capa de
 * heurísticas locales sobre el GameState que detecta situaciones recurrentes
 * (caja baja antes de nóminas, reputación cayendo, stake a punto de
 * desbloquearse, desastre sin mitigar…) y emite "tips" priorizados.
 *
 * Todo es @Serializable con defaults → saves antiguos cargan sin romperse.
 */

/** Personalidad del asistente. Modula el tono de los mensajes. */
@Serializable
enum class CompanionPersonality(
    val displayName: String,
    val emoji: String,
    val tagline: String
) {
    PROFESSOR("Profesora", "🎓", "Datos, datos y más datos. Si no se mide, no se mejora."),
    HUSTLER("Tiburón", "🦈", "Aquí no se duerme. Cada minuto cuenta."),
    ZEN_MASTER("Maestro Zen", "🧘", "Equilibrio antes que beneficio. La ruta lenta también gana."),
    GAMBLER("El Apostador", "🎲", "La fortuna favorece a los valientes — y a los preparados."),
    MENTOR("Mentor", "🧓", "Tranquilo, lo importante es no perder de vista lo que ya tienes.")
}

/** Categorías de tip — no se muestra al jugador, solo para deduplicar. */
@Serializable
enum class CompanionTipKind {
    PAYROLL_WARNING,
    REPUTATION_DROP,
    CRYPTO_RUG_RISK,
    DAILY_CHALLENGE_NEAR,
    IDLE_CASH,
    UPGRADE_REMINDER,
    STAKE_UNLOCK_SOON,
    DISASTER_PENDING,
    HEAT_WARNING,
    BALWEALTH_TYRANT,
    PRESTIGE_READY,
    ENERGY_LOW,
    DEBT_PRESSURE,
    GENERIC_ENCOURAGE
}

/** Un tip activo en el panel del asistente. */
@Serializable
data class CompanionTip(
    val id: String,
    val kind: CompanionTipKind,
    val title: String,
    val body: String,
    /** 0..100. Más alto = más arriba en la lista. */
    val priority: Int = 50,
    val createdAtTick: Long = 0L,
    val expiresAtTick: Long = 0L,
    /** Si el tip enlaza a una pantalla específica (id de SubScreen). */
    val actionRoute: String? = null
)

/** Estado serializable del asistente. */
@Serializable
data class AICompanionState(
    /** El jugador tiene que desbloquear el asistente explícitamente. */
    val unlocked: Boolean = false,
    val personality: CompanionPersonality = CompanionPersonality.MENTOR,
    /** Nombre custom; vacío → se usa default por personalidad. */
    val name: String = "",
    /** Tips activos en el panel. */
    val tips: List<CompanionTip> = emptyList(),
    /**
     * Categorías cerradas recientemente — evita que el companion repita el
     * mismo aviso cada 5 min. Sliding window: los más viejos salen.
     */
    val recentlyDismissedKinds: List<String> = emptyList(),
    /** Tick del último análisis para no reanalizar cada segundo. */
    val lastAnalysisTick: Long = 0L,
    /** Humor del companion 0..100. Sube si el jugador atiende los tips. */
    val mood: Int = 70,
    /** Estadística lifetime — total de tips cerrados como "hecho". */
    val tipsApplied: Int = 0,
    /** Estadística lifetime — total de tips descartados. */
    val tipsDismissed: Int = 0
) {
    val displayName: String
        get() = if (name.isNotBlank()) name else defaultName(personality)

    val moodEmoji: String
        get() = when {
            mood >= 80 -> "😄"
            mood >= 60 -> "🙂"
            mood >= 40 -> "😐"
            mood >= 20 -> "😞"
            else -> "😤"
        }

    val moodLabel: String
        get() = when {
            mood >= 80 -> "Encantado"
            mood >= 60 -> "Contento"
            mood >= 40 -> "Neutral"
            mood >= 20 -> "Decepcionado"
            else -> "Frustrado"
        }

    companion object {
        fun defaultName(p: CompanionPersonality): String = when (p) {
            CompanionPersonality.PROFESSOR -> "Iris"
            CompanionPersonality.HUSTLER -> "Hugo"
            CompanionPersonality.ZEN_MASTER -> "Sora"
            CompanionPersonality.GAMBLER -> "Ace"
            CompanionPersonality.MENTOR -> "Marta"
        }
    }
}

private fun defaultName(p: CompanionPersonality) = AICompanionState.defaultName(p)
