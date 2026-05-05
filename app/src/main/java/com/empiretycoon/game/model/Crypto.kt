package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Mercado de criptomonedas ficticias. Volatilidad agresiva, rugpulls reales.
 *
 * Filosofía:
 *  - 6 tokens con perfiles distintos (azul-chip vs meme vs scam).
 *  - Random walk con drift+vol diarios (tipo GARCH simplificado).
 *  - Rugpull: cada token tiene `rugChancePerDay`. Si dispara, precio cae 90%
 *    en 1 tick y posición del jugador queda con 10% del valor.
 *  - Staking: bloqueas tokens X días → APY 30-200%.
 *  - Mining: dedica empleados → genera token/día.
 *
 *  Drama > realismo. El objetivo es que cada login sea una sorpresa.
 */

/** Perfil de token. Determina volatilidad base y comportamiento. */
@Serializable
enum class CryptoProfile { BLUECHIP, STABLE, GROWTH, MEME, SHITCOIN, RUGCANDIDATE }

/** Definición estática del token (catálogo). */
@Serializable
data class CryptoToken(
    val symbol: String,
    val name: String,
    val emoji: String,
    val profile: CryptoProfile,
    val initialPrice: Double,
    /** Volatilidad diaria 0..1. Se aplica como sigma del random walk. */
    val volatility: Double,
    /** Drift diario (esperanza). Negativo en shitcoins. */
    val drift: Double,
    /** Probabilidad por día de un rugpull. 0 = nunca. */
    val rugChancePerDay: Double,
    /** Cap de stake por jugador (en cantidad de token). */
    val stakeCap: Double,
    /** APY del stake (anual decimal). */
    val stakeApy: Double,
    /** Coste energía/empleado para minar 1 token. */
    val miningDifficulty: Double
)

/** Estado dinámico de un token. */
@Serializable
data class CryptoState(
    val symbol: String,
    val price: Double,
    /** Histórico últimos 60 ticks (1 entrada cada 10s = 10 minutos in-game). */
    val history: List<Double>,
    /** Días desde el último gran movimiento (>±15%). */
    val daysSinceShock: Int = 0,
    /** Si rugpull ya ocurrió, no se vuelve a disparar. */
    val rugged: Boolean = false,
    /** Sentimiento -1..+1. Modula drift. */
    val sentiment: Double = 0.0
)

/** Posición del jugador en un token. */
@Serializable
data class CryptoHolding(
    val symbol: String,
    /** Cantidad líquida. */
    val amount: Double = 0.0,
    /** Cantidad en stake. */
    val staked: Double = 0.0,
    /** Tick en el que termina el lock. */
    val stakeUnlockTick: Long = 0L,
    /** Cantidad asignada a mining (cantidad acumulada esperando claim). */
    val miningPending: Double = 0.0,
    /** Empleados asignados a minar este token. */
    val minersAssigned: Int = 0,
    /** Coste medio de adquisición (€/token), para PnL. */
    val avgCost: Double = 0.0
)

/** Estado global del subsistema crypto. */
@Serializable
data class CryptoMarketState(
    val unlocked: Boolean = false,
    val tokens: List<CryptoState> = emptyList(),
    val holdings: List<CryptoHolding> = emptyList(),
    /** Últimas 30 noticias del feed cripto (whale moves, rugpulls, listings). */
    val newsFeed: List<CryptoNewsItem> = emptyList(),
    /** Tick del último avance diario (para no doble-aplicar). */
    val lastDailyTick: Long = 0L,
    /** Total acumulado de ganancias realizadas (lifetime). */
    val realizedPnl: Double = 0.0,
    /** Cuántos rugpulls ha vivido el jugador (estadística). */
    val rugpullsSurvived: Int = 0,
    /** True si el jugador ha completado el tutorial inicial. */
    val tutorialDone: Boolean = false
) {
    fun token(symbol: String): CryptoState? = tokens.find { it.symbol == symbol }
    fun holding(symbol: String): CryptoHolding? = holdings.find { it.symbol == symbol }
    fun holdingOrEmpty(symbol: String): CryptoHolding =
        holding(symbol) ?: CryptoHolding(symbol = symbol)
}

@Serializable
data class CryptoNewsItem(
    val tick: Long,
    val timestamp: Long,
    val symbol: String,
    val title: String,
    val body: String,
    /** RUMOR / WHALE / LISTING / RUGPULL / PUMP / DUMP */
    val kind: String
)

/**
 * Catálogo. Nombres totalmente ficticios — cualquier parecido es coincidencia.
 */
object CryptoCatalog {
    val tokens = listOf(
        CryptoToken(
            symbol = "BLU",
            name = "Bluestone",
            emoji = "💎",
            profile = CryptoProfile.BLUECHIP,
            initialPrice = 18_500.0,
            volatility = 0.04,
            drift = 0.0008,
            rugChancePerDay = 0.0,
            stakeCap = 50.0,
            stakeApy = 0.08,
            miningDifficulty = 220.0
        ),
        CryptoToken(
            symbol = "GLD",
            name = "GoldChain",
            emoji = "🥇",
            profile = CryptoProfile.STABLE,
            initialPrice = 1_980.0,
            volatility = 0.015,
            drift = 0.0002,
            rugChancePerDay = 0.0,
            stakeCap = 500.0,
            stakeApy = 0.05,
            miningDifficulty = 35.0
        ),
        CryptoToken(
            symbol = "MOON",
            name = "Moonshot",
            emoji = "🚀",
            profile = CryptoProfile.GROWTH,
            initialPrice = 240.0,
            volatility = 0.10,
            drift = 0.0015,
            rugChancePerDay = 0.0008,
            stakeCap = 2_000.0,
            stakeApy = 0.18,
            miningDifficulty = 6.0
        ),
        CryptoToken(
            symbol = "PUMP",
            name = "PumpKing",
            emoji = "🐶",
            profile = CryptoProfile.MEME,
            initialPrice = 0.12,
            volatility = 0.25,
            drift = -0.0010,
            rugChancePerDay = 0.0030,
            stakeCap = 200_000.0,
            stakeApy = 0.85,
            miningDifficulty = 0.005
        ),
        CryptoToken(
            symbol = "SAFE",
            name = "SafeYield",
            emoji = "🛡️",
            profile = CryptoProfile.STABLE,
            initialPrice = 1.00,
            volatility = 0.005,
            drift = 0.00005,
            rugChancePerDay = 0.0001,
            stakeCap = 1_000_000.0,
            stakeApy = 0.12,
            miningDifficulty = 0.02
        ),
        CryptoToken(
            symbol = "RUG",
            name = "RugBoy",
            emoji = "💩",
            profile = CryptoProfile.RUGCANDIDATE,
            initialPrice = 0.0040,
            volatility = 0.45,
            drift = 0.0050,
            rugChancePerDay = 0.0150,
            stakeCap = 10_000_000.0,
            stakeApy = 2.00,
            miningDifficulty = 0.0001
        )
    )

    fun freshState(): CryptoMarketState = CryptoMarketState(
        unlocked = false,
        tokens = tokens.map {
            CryptoState(
                symbol = it.symbol,
                price = it.initialPrice,
                history = listOf(it.initialPrice)
            )
        },
        holdings = emptyList()
    )

    fun byMatching(symbol: String): CryptoToken? = tokens.find { it.symbol == symbol }
}
