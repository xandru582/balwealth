package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Contrato B2B: una empresa cliente solicita una serie de unidades de uno o
 * varios recursos a un precio acordado, con un plazo y bonificación/multa.
 *
 * - [items]: cantidades requeridas por recurso.
 * - [paymentPerUnit]: precio acordado por unidad y por recurso.
 * - [deadlineSeconds]: plazo total desde la creación.
 * - [bonusOnTime]: pago extra al cumplir antes de plazo.
 * - [penaltyMissed]: multa si expira sin completar.
 * - [deliveredQty]: cuántas unidades llevamos entregadas por recurso.
 * - [accepted]: el jugador ha aceptado y aparece en su lista activa.
 * - [completed] / [expired]: estados terminales mutuamente excluyentes.
 */
@Serializable
data class Contract(
    val id: String,
    val clientName: String,
    /** Logo del cliente como emoji corporativo. */
    val clientLogo: String,
    val items: Map<String, Int>,
    val paymentPerUnit: Map<String, Double>,
    val deadlineSeconds: Long,
    val penaltyMissed: Double,
    val bonusOnTime: Double,
    val accepted: Boolean = false,
    val deliveredQty: Map<String, Int> = emptyMap(),
    val completed: Boolean = false,
    val expired: Boolean = false,
    /** Tick en el que se creó el contrato (para calcular expiración). */
    val createdAtTick: Long = 0L,
    /** Tier requerido (reputación / 20). */
    val tier: Int = 1
) {
    val totalRequested: Int get() = items.values.sum()

    val totalDelivered: Int get() = deliveredQty.values.sum()

    val progress: Float
        get() = if (totalRequested == 0) 0f
        else (totalDelivered.toFloat() / totalRequested.toFloat()).coerceIn(0f, 1f)

    val totalPaymentEstimate: Double
        get() = items.entries.sumOf { (id, qty) ->
            (paymentPerUnit[id] ?: 0.0) * qty
        }

    fun deadlineTick(): Long = createdAtTick + deadlineSeconds

    fun secondsLeft(currentTick: Long): Long =
        max(0L, deadlineTick() - currentTick)

    fun isFulfilled(): Boolean = items.all { (id, qty) ->
        (deliveredQty[id] ?: 0) >= qty
    }
}

/**
 * Estado serializable del subsistema de contratos.
 */
@Serializable
data class ContractsState(
    val offers: List<Contract> = emptyList(),
    val accepted: List<Contract> = emptyList(),
    val completedTotal: Int = 0,
    val expiredTotal: Int = 0,
    val totalEarnings: Double = 0.0,
    /** Último tick en el que se refrescaron las ofertas. */
    val lastRefreshTick: Long = 0L
)

/**
 * Generador procedural de contratos B2B. Los contratos se ajustan al tier
 * desbloqueado por la reputación de la empresa.
 */
object ContractGenerator {

    private val clientNames: List<Pair<String, String>> = listOf(
        "Acme Corp." to "🏭",
        "TitanGroup" to "🏢",
        "PymeCo" to "🏪",
        "Globex" to "🌐",
        "VertexLogistics" to "🚚",
        "AurumRetail" to "🛍️",
        "NeoCloud" to "☁️",
        "MeridianFoods" to "🍞",
        "OmegaMotors" to "🚗",
        "BrightTech" to "💡",
        "Helios Energy" to "🔋",
        "PrimeYachts" to "⛵",
        "OakWood Furn." to "🪑",
        "SilkRoad Trade" to "🧵",
        "BastionSteel" to "⚙️",
        "Polaris Mining" to "⛏️",
        "Vita Diary" to "🥛",
        "Quark Chips" to "🧠",
        "ChronoBuilders" to "🏗️",
        "SunsetCheese" to "🧀"
    )

    /** Pool de recursos pedibles según el tier. */
    private fun pool(tier: Int): List<Resource> {
        val cats = when (tier) {
            1 -> setOf(ResourceCategory.RAW, ResourceCategory.FOOD)
            2 -> setOf(ResourceCategory.RAW, ResourceCategory.FOOD, ResourceCategory.MATERIAL)
            3 -> setOf(ResourceCategory.MATERIAL, ResourceCategory.COMPONENT, ResourceCategory.FOOD)
            4 -> setOf(ResourceCategory.COMPONENT, ResourceCategory.GOOD, ResourceCategory.SERVICE)
            else -> setOf(ResourceCategory.GOOD, ResourceCategory.LUXURY, ResourceCategory.SERVICE)
        }
        return ResourceCatalog.all.filter { it.category in cats }
    }

    /**
     * Crea un nuevo contrato razonable para el estado dado.
     * El precio por unidad se sitúa entre 1.05x y 1.30x del precio actual de
     * mercado (premium B2B). Los lotes son mayores con tier alto.
     */
    fun createNew(state: GameState, rng: Random): Contract {
        val rep = state.company.reputation
        val tier = (rep / 20).coerceIn(1, 5)
        val (clientName, logo) = clientNames.random(rng)
        val pool = pool(tier).ifEmpty { ResourceCatalog.all }

        // 1-3 ítems por contrato, según tier
        val itemCount = when {
            tier <= 1 -> 1
            tier == 2 -> if (rng.nextDouble() < 0.55) 1 else 2
            tier == 3 -> if (rng.nextDouble() < 0.45) 2 else 1
            else -> if (rng.nextDouble() < 0.35) 3 else 2
        }

        val chosen = mutableListOf<Resource>()
        val remaining = pool.toMutableList()
        repeat(min(itemCount, remaining.size)) {
            val idx = rng.nextInt(remaining.size)
            chosen += remaining.removeAt(idx)
        }

        val items = HashMap<String, Int>()
        val paymentPerUnit = HashMap<String, Double>()

        for (r in chosen) {
            // tamaño del lote: depende del precio base y tier
            val baseSize = when {
                r.basePrice <= 8.0 -> 60..200
                r.basePrice <= 30.0 -> 30..120
                r.basePrice <= 100.0 -> 12..40
                r.basePrice <= 500.0 -> 4..15
                r.basePrice <= 3_000.0 -> 1..6
                else -> 1..3
            }
            val sizeMin = baseSize.first
            val sizeMax = baseSize.last
            val tierMul = 1.0 + (tier - 1) * 0.35
            val qty = ((rng.nextInt(sizeMin, sizeMax + 1)) * tierMul).toInt().coerceAtLeast(1)
            items[r.id] = qty

            val marketPrice = state.market.priceOf(r.id)
            val premium = rng.nextDouble(1.05, 1.32)
            paymentPerUnit[r.id] = marketPrice * premium
        }

        // plazo: 1-4 días in-game, escala con tier (más volumen, más tiempo)
        val baseDays = (1 + rng.nextInt(3)) + (tier / 2)
        val deadline = baseDays * 1_440L

        // bonus 5-15% del total, multa 8-25%
        val total = items.entries.sumOf { (id, q) -> (paymentPerUnit[id] ?: 0.0) * q }
        val bonus = total * rng.nextDouble(0.05, 0.16)
        val penalty = total * rng.nextDouble(0.08, 0.26)

        val id = "ctr_${state.tick}_${rng.nextInt(100_000)}"
        return Contract(
            id = id,
            clientName = clientName,
            clientLogo = logo,
            items = items,
            paymentPerUnit = paymentPerUnit,
            deadlineSeconds = deadline,
            penaltyMissed = penalty,
            bonusOnTime = bonus,
            createdAtTick = state.tick,
            tier = tier
        )
    }

    /**
     * Refresca el listado de ofertas: 3-8 contratos activos. Mantiene los
     * existentes que aún no han caducado y rellena con nuevos.
     */
    fun refreshOffers(state: GameState, contracts: ContractsState, rng: Random): ContractsState {
        val targetCount = 3 + rng.nextInt(6) // 3..8
        val keep = contracts.offers.filter { !it.expired && !it.completed }
            .take(targetCount / 2)
        val toAdd = targetCount - keep.size
        val newOnes = (0 until toAdd).map { createNew(state, rng) }
        return contracts.copy(
            offers = keep + newOnes,
            lastRefreshTick = state.tick
        )
    }
}
