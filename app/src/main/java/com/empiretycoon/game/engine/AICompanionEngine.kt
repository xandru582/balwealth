package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max

/**
 * Motor del asistente IA. NO es una IA real: es un agregador de heurísticas
 * sobre el GameState que detecta situaciones interesantes y emite "tips".
 *
 * Ciclo:
 *  - tick(state) se llama cada segundo desde GameEngine.advanceOneSecond.
 *  - Solo procesa cada ANALYSIS_INTERVAL_TICKS (5 min in-game) para no quemar
 *    CPU ni inundar de tips al jugador.
 *  - En cada análisis: limpia tips expirados, evalúa heurísticas, añade nuevos
 *    tips (deduplicados por kind y por kind recientemente descartado).
 *
 * Filosofía:
 *  - Los tips son sugerencias, no spam. Se cap a MAX_ACTIVE_TIPS.
 *  - El tono del mensaje varía con la personalidad (con/sin signature).
 *  - Si el jugador descarta una categoría, no vuelve a aparecer hasta que se
 *    desliza fuera del recentlyDismissedKinds (sliding window).
 *
 * Patrón GameState -> GameState: ningún side effect.
 */
object AICompanionEngine {

    /** Cada cuántos ticks reanalizamos. 5 min in-game = 300 ticks. */
    private const val ANALYSIS_INTERVAL_TICKS: Long = 300L

    /** Cuántos tips puede haber simultáneamente. */
    private const val MAX_ACTIVE_TIPS = 6

    /** Cuánto vive un tip antes de auto-expirar. 2 días in-game. */
    private const val TIP_TTL_TICKS: Long = 1_440L * 2L

    /** Cuántas categorías de tip recordamos como "ya descartadas" recientemente. */
    private const val DISMISS_MEMORY = 8

    fun canUnlock(state: GameState): Boolean = state.player.level >= 3

    fun unlock(
        state: GameState,
        personality: CompanionPersonality,
        name: String
    ): GameState {
        val ai = state.aiCompanion
        if (ai.unlocked) return state
        if (!canUnlock(state)) {
            return notify(state, NotificationKind.ERROR, "🔒 Asistente bloqueado",
                "Necesitas nivel 3 para desbloquear el asistente.")
        }
        val newAi = ai.copy(
            unlocked = true,
            personality = personality,
            name = name.trim()
        )
        val resolvedName = if (newAi.name.isNotBlank()) newAi.name
            else AICompanionState.defaultName(personality)
        return notify(
            state.copy(aiCompanion = newAi),
            NotificationKind.SUCCESS,
            "🤖 ${personality.emoji} ${personality.displayName} a bordo",
            "$resolvedName se une a tu equipo. Te avisará cuando algo merezca tu atención."
        )
    }

    fun setPersonality(state: GameState, personality: CompanionPersonality): GameState {
        val ai = state.aiCompanion
        if (!ai.unlocked || ai.personality == personality) return state
        return state.copy(aiCompanion = ai.copy(personality = personality))
    }

    fun rename(state: GameState, newName: String): GameState {
        val ai = state.aiCompanion
        if (!ai.unlocked) return state
        return state.copy(aiCompanion = ai.copy(name = newName.trim()))
    }

    /** Tick frecuente. Sale rápido si no toca reanalizar. */
    fun tick(state: GameState): GameState {
        val ai = state.aiCompanion
        if (!ai.unlocked) return state
        if (state.tick - ai.lastAnalysisTick < ANALYSIS_INTERVAL_TICKS) return state
        return analyze(state)
    }

    /** Cierra un tip como "ya he hecho lo que sugería" — sube mood. */
    fun acknowledge(state: GameState, tipId: String): GameState {
        val ai = state.aiCompanion
        if (ai.tips.none { it.id == tipId }) return state
        val newTips = ai.tips.filter { it.id != tipId }
        return state.copy(aiCompanion = ai.copy(
            tips = newTips,
            tipsApplied = ai.tipsApplied + 1,
            mood = (ai.mood + 2).coerceAtMost(100)
        ))
    }

    /** Descarta un tip — baja mood y memoriza la categoría. */
    fun dismiss(state: GameState, tipId: String): GameState {
        val ai = state.aiCompanion
        val tip = ai.tips.find { it.id == tipId } ?: return state
        val newTips = ai.tips.filter { it.id != tipId }
        val mem = (ai.recentlyDismissedKinds + tip.kind.name).takeLast(DISMISS_MEMORY)
        return state.copy(aiCompanion = ai.copy(
            tips = newTips,
            recentlyDismissedKinds = mem,
            tipsDismissed = ai.tipsDismissed + 1,
            mood = (ai.mood - 1).coerceAtLeast(0)
        ))
    }

    fun clearAll(state: GameState): GameState {
        val ai = state.aiCompanion
        return state.copy(aiCompanion = ai.copy(tips = emptyList()))
    }

    // ===================== Análisis =====================

    private fun analyze(state: GameState): GameState {
        val now = state.tick
        val ai = state.aiCompanion

        // 1) Tips activos = los actuales que no expiraron.
        val stillActive = ai.tips.filter { it.expiresAtTick > now }
        val activeKinds = stillActive.map { it.kind }.toSet()
        val recentlyDismissed = ai.recentlyDismissedKinds.toSet()

        // 2) Recolectamos candidatos de las heurísticas.
        val candidates = mutableListOf<CompanionTip>()

        runHeuristic(state, CompanionTipKind.PAYROLL_WARNING, candidates) { s ->
            if (!shouldWarnPayroll(s)) null else makeTip(s,
                CompanionTipKind.PAYROLL_WARNING,
                title = "💸 Nóminas próximas",
                body = "Quedan menos de un día y la caja está justa para pagar al equipo. Considera vender stock, cobrar contratos pendientes o pedir un préstamo.",
                priority = 90,
                actionRoute = "hr"
            )
        }

        runHeuristic(state, CompanionTipKind.REPUTATION_DROP, candidates) { s ->
            if (s.company.reputation > 30) null else makeTip(s,
                CompanionTipKind.REPUTATION_DROP,
                title = "📉 Reputación baja",
                body = "Tu reputación es ${s.company.reputation}/100. Eso bloquea contratos B2B mejores y candidatos premium. Mira si puedes hacer donaciones o aceptar misiones que la suban.",
                priority = 60,
                actionRoute = "story"
            )
        }

        runHeuristic(state, CompanionTipKind.CRYPTO_RUG_RISK, candidates) { s ->
            if (!s.crypto.unlocked) return@runHeuristic null
            // Buscar token donde tenemos posición y el sentimiento está muy negativo
            val risky = s.crypto.tokens.firstOrNull { tok ->
                val h = s.crypto.holding(tok.symbol)
                h != null && h.amount > 0.000001 && tok.sentiment <= -0.5 && !tok.rugged
            } ?: return@runHeuristic null
            makeTip(s,
                CompanionTipKind.CRYPTO_RUG_RISK,
                title = "🪙 Cripto en rojo: ${risky.symbol}",
                body = "El sentimiento de ${risky.symbol} está muy negativo (${"%.2f".format(risky.sentiment)}). Si su perfil es de scam, podría rugpullear pronto. Considera salir parcial.",
                priority = 80,
                actionRoute = "crypto"
            )
        }

        runHeuristic(state, CompanionTipKind.DAILY_CHALLENGE_NEAR, candidates) { s ->
            val near = s.dailyChallenges.daily.firstOrNull { c ->
                !c.completed && !c.claimed && c.target > 0 && c.progress >= (c.target * 0.8).toLong()
            } ?: return@runHeuristic null
            val percent = (near.percent * 100).toInt()
            makeTip(s,
                CompanionTipKind.DAILY_CHALLENGE_NEAR,
                title = "🎯 Reto al $percent%",
                body = "\"${near.title}\" está casi hecho (${near.progress}/${near.target}). Empuja un poco y mantén la racha viva.",
                priority = 55,
                actionRoute = "daily"
            )
        }

        runHeuristic(state, CompanionTipKind.IDLE_CASH, candidates) { s ->
            val idle = s.company.cash
            // Solo molesta si hay mucho dinero ocioso Y no hay deuda viva
            val hasActiveDebt = s.loans.active.any { !it.defaulted && !it.isPaidOff }
            if (idle < 500_000.0 || hasActiveDebt) null else makeTip(s,
                CompanionTipKind.IDLE_CASH,
                title = "💼 Caja parada",
                body = "Tienes ${"%,.0f".format(idle)} € durmiendo. La inflación se los come. Mira la bolsa, los inmuebles o el stake en SAFE para ponerlos a producir.",
                priority = 35,
                actionRoute = "wealth"
            )
        }

        runHeuristic(state, CompanionTipKind.STAKE_UNLOCK_SOON, candidates) { s ->
            if (!s.crypto.unlocked) return@runHeuristic null
            val unlocking = s.crypto.holdings.firstOrNull { h ->
                h.staked > 0 && h.stakeUnlockTick > 0 &&
                    s.tick < h.stakeUnlockTick &&
                    (h.stakeUnlockTick - s.tick) <= 1_440L
            } ?: return@runHeuristic null
            makeTip(s,
                CompanionTipKind.STAKE_UNLOCK_SOON,
                title = "🔓 Stake casi liberado",
                body = "Tu stake en ${unlocking.symbol} se libera en menos de 24h in-game. Decide si haces unstake al desbloqueo o renuevas.",
                priority = 45,
                actionRoute = "crypto"
            )
        }

        runHeuristic(state, CompanionTipKind.DISASTER_PENDING, candidates) { s ->
            val pending = s.disasters.active.firstOrNull { it.phase == DisasterPhase.PENDING_RESPONSE }
                ?: return@runHeuristic null
            makeTip(s,
                CompanionTipKind.DISASTER_PENDING,
                title = "🌪️ Desastre con ventana abierta",
                body = "${pending.title} está en fase de respuesta. Tienes plazo limitado para mitigar. Cualquier acción reduce el impacto.",
                priority = 95,
                actionRoute = "disasters"
            )
        }

        runHeuristic(state, CompanionTipKind.HEAT_WARNING, candidates) { s ->
            if (!s.heists.unlocked || s.heists.heat < 65) null else makeTip(s,
                CompanionTipKind.HEAT_WARNING,
                title = "👮 Heat alto",
                body = "Tu heat es ${s.heists.heat}/100. Por encima de 70 hay riesgo real de embargo. Frena los heists o espera a que decaiga.",
                priority = 70,
                actionRoute = "heists"
            )
        }

        runHeuristic(state, CompanionTipKind.ENERGY_LOW, candidates) { s ->
            if (s.player.energy > 20 || s.player.maxEnergy <= 0) null else makeTip(s,
                CompanionTipKind.ENERGY_LOW,
                title = "🔋 Energía baja",
                body = "Te quedan ${s.player.energy}/${s.player.maxEnergy} ⚡. Para entrenar o trabajar más, mejor descansa primero.",
                priority = 30,
                actionRoute = "player"
            )
        }

        runHeuristic(state, CompanionTipKind.DEBT_PRESSURE, candidates) { s ->
            val totalDebt = s.loans.totalDebt
            if (totalDebt < 100_000.0 || totalDebt < s.company.cash * 2.0) null else makeTip(s,
                CompanionTipKind.DEBT_PRESSURE,
                title = "🏦 Deuda creciente",
                body = "Tienes ${"%,.0f".format(totalDebt)} € en préstamos vivos vs ${"%,.0f".format(s.company.cash)} en caja. Si no controlas, la siguiente cuota dolerá.",
                priority = 65,
                actionRoute = "banking"
            )
        }

        runHeuristic(state, CompanionTipKind.UPGRADE_REMINDER, candidates) { s ->
            // Si hay mucha caja Y todos los edificios están en nivel bajo
            val avgLevel = if (s.company.buildings.isEmpty()) 0.0
                else s.company.buildings.sumOf { it.level }.toDouble() / s.company.buildings.size
            if (s.company.buildings.isEmpty() ||
                s.company.cash < 200_000.0 ||
                avgLevel >= 5.0) null else makeTip(s,
                CompanionTipKind.UPGRADE_REMINDER,
                title = "🏗️ Hora de mejorar",
                body = "Tus edificios están a nivel medio ${"%.1f".format(avgLevel)} y tienes liquidez. Subir niveles multiplica producción y márgen.",
                priority = 40,
                actionRoute = "empire"
            )
        }

        // 3) Filtramos: descartamos candidatos cuya kind ya esté activa o
        //    recientemente descartada.
        val newOnes = candidates
            .filter { it.kind !in activeKinds }
            .filter { it.kind.name !in recentlyDismissed }

        // 4) Componemos lista ordenada por prioridad descendente, con cap.
        val merged = (stillActive + newOnes)
            .sortedByDescending { it.priority }
            .take(MAX_ACTIVE_TIPS)

        return state.copy(aiCompanion = ai.copy(
            tips = merged,
            lastAnalysisTick = now
        ))
    }

    // ===================== Helpers =====================

    /** Wrap utilitario para una heurística. */
    private inline fun runHeuristic(
        state: GameState,
        kind: CompanionTipKind,
        sink: MutableList<CompanionTip>,
        block: (GameState) -> CompanionTip?
    ) {
        val tip = block(state) ?: return
        // Defensivo: si la heurística devolvió un tip de kind distinta al esperado,
        // confiamos igual en el valor del tip (las heurísticas pueden encadenar).
        sink.add(tip)
    }

    /** Construye un tip aplicando el tono de la personalidad. */
    private fun makeTip(
        state: GameState,
        kind: CompanionTipKind,
        title: String,
        body: String,
        priority: Int = 50,
        actionRoute: String? = null
    ): CompanionTip {
        val now = state.tick
        val flavored = applyPersonality(state.aiCompanion.personality, body)
        return CompanionTip(
            id = "tip_${now}_${kind.name}_${System.nanoTime()}",
            kind = kind,
            title = title,
            body = flavored,
            priority = priority,
            createdAtTick = now,
            expiresAtTick = now + TIP_TTL_TICKS,
            actionRoute = actionRoute
        )
    }

    private fun applyPersonality(p: CompanionPersonality, text: String): String {
        // Añade un breve apunte al final según personalidad. No reescribimos
        // el cuerpo entero: solo flavor, así el contenido sigue siendo claro.
        return when (p) {
            CompanionPersonality.PROFESSOR ->
                "$text\n\n— Iris (los datos no opinan, observan)."
            CompanionPersonality.HUSTLER ->
                "$text\n\n— Hugo (ya, ya — al lío)."
            CompanionPersonality.ZEN_MASTER ->
                "$text\n\n— Sora (respira y elige; no hay urgencia que valga la salud)."
            CompanionPersonality.GAMBLER ->
                "$text\n\n— Ace (la fortuna premia al que está atento, jefe)."
            CompanionPersonality.MENTOR ->
                "$text\n\n— Marta (sin presiones, solo te recuerdo lo que ya sabes)."
        }
    }

    // ===================== Heurísticas individuales =====================

    private fun shouldWarnPayroll(s: GameState): Boolean {
        val emps = s.company.employees
        if (emps.isEmpty()) return false
        val totalSalaries = emps.sumOf { it.monthlySalary }
        // El día in-game termina cada 1.440 ticks. Si quedan < 800 (~13h reales
        // con speed=1) y no hay liquidez para 1.2x el coste, avisamos.
        val ticksToNextDay = 1_440L - (s.tick % 1_440L)
        return ticksToNextDay <= 800L && s.company.cash < totalSalaries * 1.2
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

    // Suprimimos warning de "max" no usado por dependencia futura — para no sacar
    // el import si añadimos más heurísticas.
    @Suppress("unused")
    private val keepImports = max(0, 0)
}
