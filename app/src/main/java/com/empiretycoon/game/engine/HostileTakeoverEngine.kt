package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de Adquisiciones Hostiles. Pure GameState -> GameState.
 *
 * Permite al jugador "absorber" un rival pagando un premium del 20% sobre
 * el 51% de su cash. La RNG decide la defensa que activa el rival:
 *   - 30%: NONE (adquisición limpia).
 *   - 25%: POISON_PILL (recargo del 25% al final).
 *   - 25%: WHITE_KNIGHT (la operación falla, refund 80%).
 *   - 20%: GOLDEN_PARACHUTE (la operación va, rival escapa con 30%).
 *
 * Si la operación tiene éxito, el rival entra en `defeated` con sus
 * recompensas estándar más un bonus por adquisición (10% del coste).
 *
 * Restricciones:
 *  - Reputación >= 60.
 *  - Cash >= base cost (al lanzar). Para Poison Pill se requiere cash extra
 *    al resolver: si no llega, se ejecuta WHITE_KNIGHT en su lugar.
 *  - Cooldown de 3 días entre takeovers.
 */
object HostileTakeoverEngine {

    /** Comprueba si el jugador puede lanzar una OPA contra un rival. */
    fun canLaunch(state: GameState, rivalId: String): Pair<Boolean, String?> {
        if (state.hostileTakeover.isOnCooldown(state.tick)) {
            val left = state.hostileTakeover.ticksLeftCooldown(state.tick) / 60L
            return false to "Cooldown activo: faltan $left minutos in-game"
        }
        if (state.company.reputation < HostileTakeoverConstants.MIN_REPUTATION) {
            return false to "Reputación insuficiente (necesitas ${HostileTakeoverConstants.MIN_REPUTATION})"
        }
        val rival = state.rivals.active.find { it.id == rivalId && !it.defeated }
            ?: return false to "Rival no disponible"
        val cost = HostileTakeoverConstants.baseCost(rival.cash)
        if (state.company.cash < cost) {
            return false to "Necesitas ${"%,.0f".format(cost)} € en caja"
        }
        return true to null
    }

    /** Coste base que verá el jugador antes de lanzar. */
    fun previewCost(state: GameState, rivalId: String): Double {
        val rival = state.rivals.active.find { it.id == rivalId } ?: return 0.0
        return HostileTakeoverConstants.baseCost(rival.cash)
    }

    /**
     * Lanza la OPA. Resuelve en el momento (RNG decide defensa). Devuelve un
     * GameState con el resultado aplicado y notificación apropiada.
     */
    fun launchTakeover(state: GameState, rivalId: String, rng: Random = Random.Default): GameState {
        val (ok, reason) = canLaunch(state, rivalId)
        if (!ok) {
            return notify(state, NotificationKind.ERROR, "🚫 OPA bloqueada",
                reason ?: "No se puede lanzar ahora.")
        }
        val rival = state.rivals.active.find { it.id == rivalId } ?: return state
        val baseCost = HostileTakeoverConstants.baseCost(rival.cash)

        // RNG: elige defensa
        val roll = rng.nextDouble()
        var defense = when {
            roll < 0.30 -> TakeoverDefense.NONE
            roll < 0.55 -> TakeoverDefense.POISON_PILL
            roll < 0.80 -> TakeoverDefense.WHITE_KNIGHT
            else        -> TakeoverDefense.GOLDEN_PARACHUTE
        }

        // Coste y outcome según defensa
        var costPaid = baseCost
        var outcome = TakeoverOutcome.SUCCESS
        var refund = 0.0

        when (defense) {
            TakeoverDefense.NONE -> {
                outcome = TakeoverOutcome.SUCCESS
            }
            TakeoverDefense.POISON_PILL -> {
                val extra = baseCost * HostileTakeoverConstants.POISON_PILL_MARKUP
                if (state.company.cash >= baseCost + extra) {
                    costPaid = baseCost + extra
                    outcome = TakeoverOutcome.SUCCESS
                } else {
                    // Cash insuficiente para sortear el poison pill → fail con refund parcial.
                    defense = TakeoverDefense.WHITE_KNIGHT
                    outcome = TakeoverOutcome.FAILED_REFUND
                    refund = baseCost * HostileTakeoverConstants.WHITE_KNIGHT_REFUND
                }
            }
            TakeoverDefense.WHITE_KNIGHT -> {
                outcome = TakeoverOutcome.FAILED_REFUND
                refund = baseCost * HostileTakeoverConstants.WHITE_KNIGHT_REFUND
            }
            TakeoverDefense.GOLDEN_PARACHUTE -> {
                outcome = TakeoverOutcome.SUCCESS_PARTIAL_LOSS
                // El coste no cambia, pero el rival se lleva una parte como bonus.
            }
        }

        // Actualiza cash de la empresa
        val cashAfter = state.company.cash - costPaid + refund
        var company = state.company.copy(cash = cashAfter.coerceAtLeast(0.0))

        // Si la operación tiene éxito, mueve rival a defeated y aplica recompensas
        var rivals = state.rivals
        var player = state.player
        var notif: GameNotification? = null
        when (outcome) {
            TakeoverOutcome.SUCCESS, TakeoverOutcome.SUCCESS_PARTIAL_LOSS -> {
                val acquisitionBonus = costPaid * 0.10
                val cashGain = rival.rewardCash + acquisitionBonus
                company = company.copy(
                    cash = company.cash + cashGain,
                    reputation = (company.reputation + rival.rewardReputation + 3).coerceIn(0, 100)
                )
                player = player.addXp(rival.rewardXp + 500)
                rivals = state.rivals.copy(
                    active = state.rivals.active.filterNot { it.id == rival.id },
                    defeated = state.rivals.defeated + rival.copy(defeated = true),
                    currentChallenge = state.rivals.active
                        .filterNot { it.id == rival.id || it.defeated }
                        .minByOrNull { it.cash }?.id
                )
                val title = "🏴‍☠️ ${rival.name} absorbido"
                val msg = if (outcome == TakeoverOutcome.SUCCESS_PARTIAL_LOSS)
                    "${defense.emoji} ${defense.displayName}: la OPA va, pero ${rival.name} escapa con un ${(HostileTakeoverConstants.GOLDEN_PARACHUTE_LOSS * 100).toInt()}% del coste como paracaídas. " +
                        "Has pagado ${"%,.0f".format(costPaid)} €, recuperas ${"%,.0f".format(cashGain)} €."
                else
                    "${defense.emoji} ${defense.displayName}. Coste total: ${"%,.0f".format(costPaid)} €. " +
                        "Bonus de adquisición: ${"%,.0f".format(cashGain)} €. " +
                        "+${rival.rewardXp + 500} XP, +${rival.rewardReputation + 3} rep."
                notif = GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = title,
                    message = msg
                )
            }
            TakeoverOutcome.FAILED_REFUND -> {
                val title = "🛡 OPA bloqueada por ${rival.name}"
                val msg = "${defense.emoji} ${defense.displayName}: otro inversor contraoferta y la operación cae. " +
                    "Has perdido ${"%,.0f".format(costPaid - refund)} € (refund del 80%)."
                notif = GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.WARNING,
                    title = title,
                    message = msg
                )
            }
        }

        // Registra historial y aplica cooldown
        val record = TakeoverRecord(
            rivalId = rival.id,
            rivalName = rival.name,
            costPaid = costPaid,
            defense = defense,
            outcome = outcome,
            atTick = state.tick
        )
        val newHostileTakeover = state.hostileTakeover.copy(
            history = (state.hostileTakeover.history + record).takeLast(20),
            cooldownUntilTick = state.tick + HostileTakeoverConstants.COOLDOWN_TICKS
        )

        val newNotifications = notif?.let { (state.notifications + it).takeLast(40) }
            ?: state.notifications

        return state.copy(
            company = company,
            player = player,
            rivals = rivals,
            hostileTakeover = newHostileTakeover,
            notifications = newNotifications
        )
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
