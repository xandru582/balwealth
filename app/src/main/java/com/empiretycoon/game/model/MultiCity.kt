package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * MultiCity — comercio internacional (v17 — siguientes tandas de MEJORAS.md).
 *
 * El jugador desbloquea 5 mercados extranjeros con economías propias:
 *  - Cada ciudad tiene sus multiplicadores de precio sobre el `Market` base
 *    (e.g. Dubai paga el petróleo 0,7× pero las joyas 0,85×).
 *  - Aranceles a la importación (al enviar producto allí) y a la exportación
 *    (al cobrar la venta).
 *  - Impuesto local sobre beneficio neto.
 *  - Tipos de cambio simplificados: factor multiplicativo que distorsiona
 *    el precio de venta — modela la fortaleza/debilidad relativa.
 *  - Volatilidad propia: cuánto se mueven los multiplicadores cada día.
 *
 * Modelo simplificado de comercio:
 *  1. Abre una ruta HOME → Ciudad (one-time fee = 30× el daily cost).
 *  2. Hace un envío: paga arancel de importación + saca el inventario de casa.
 *  3. Tras `transitTicks` ticks llega → se vende automáticamente al precio
 *     remoto = base × priceMul × demand × FX. Sobre eso se aplica
 *     exportTariff y localTax.
 *  4. El cash neto entra a la empresa.
 *
 * Filosofía: NO duplicamos inventario por ciudad — el "inventario remoto" no
 * existe, los envíos viajan sellados y se liquidan al llegar. Esto evita
 * complejidad y exploits.
 *
 * Todo @Serializable con defaults — saves antiguos cargan sin romperse.
 */

/** Identificadores de ciudad. HOME es la del jugador (no exportable a sí misma). */
@Serializable
enum class CityId(val displayName: String, val emoji: String, val country: String) {
    HOME("Tu ciudad", "🏠", ""),
    NEO_TOKYO("Neo Tokio", "🗼", "Asia tech"),
    DUBAI_CITY("Dubai City", "🏙️", "Golfo / petroquímica"),
    LAGOS_BAY("Lagos Bay", "🌴", "África emergente"),
    NEW_COAST("New Coast", "🌆", "Norteamérica"),
    BERLIN_NEU("Berlín Nuevo", "🏛️", "Centro-Europa industrial")
}

/** Mercado de una ciudad. */
@Serializable
data class CityMarket(
    val id: CityId,
    /** Multiplicador de precio por recurso (1.0 = igual al base global). */
    val priceMultipliers: Map<String, Double> = emptyMap(),
    /** Demanda por recurso 0.3..2.0 — multiplica el precio de venta efectivo. */
    val demand: Map<String, Double> = emptyMap(),
    /** Tipo de cambio relativo (factor sobre EUR). 1.05 = la moneda local es algo más fuerte. */
    val exchangeRate: Double = 1.0,
    /** Arancel de importación. Aplicado al enviar mercancía a esta ciudad. */
    val importTariff: Double = 0.0,
    /** Arancel de exportación. Aplicado al cobrar la venta. */
    val exportTariff: Double = 0.0,
    /** Impuesto local sobre beneficio neto al vender allí. */
    val localTax: Double = 0.20,
    /** Volatilidad diaria de los multiplicadores (0..0.20). */
    val volatility: Double = 0.05,
    /** Sentimiento "boom/bust" de la ciudad — multiplica todos los precios temporalmente. */
    val sentiment: Double = 1.0
) {
    /** Precio efectivo de venta para un recurso, antes de aranceles e impuesto. */
    fun effectiveSellPrice(basePrice: Double, resourceId: String): Double {
        val mul = priceMultipliers[resourceId] ?: 1.0
        val dem = demand[resourceId] ?: 1.0
        return basePrice * mul * dem * exchangeRate * sentiment
    }
}

/** Ruta logística entre dos ciudades. */
@Serializable
data class CityRoute(
    val id: String,
    val from: CityId,
    val to: CityId,
    /** Coste diario de mantenimiento mientras esté abierta. */
    val dailyCost: Double,
    /** Ticks de tránsito (aprox 1.440 = 1 día in-game). */
    val transitTicks: Long,
    /** Capacidad máxima por envío. */
    val capacityPerShipment: Int = 1_000,
    /** Si está abierta. Apertura cuesta 30× dailyCost (1 mes de prima). */
    val open: Boolean = false
)

/** Envío en tránsito. Se liquida al llegar. */
@Serializable
data class CityShipment(
    val id: String,
    val routeId: String,
    val resourceId: String,
    val qty: Int,
    /** Coste interno de la mercancía al salir (ya pagado). */
    val baseCostAtDeparture: Double,
    /** Importación pagada al enviar (ya cobrada). */
    val importTariffPaid: Double,
    val arrivesAtTick: Long,
    val from: CityId,
    val to: CityId
) {
    fun ticksLeft(now: Long): Long = (arrivesAtTick - now).coerceAtLeast(0L)
}

/** Resumen lifetime de un envío liquidado (para historial). */
@Serializable
data class CityShipmentResult(
    val resourceId: String,
    val qty: Int,
    val to: CityId,
    val gross: Double,
    val exportTariff: Double,
    val localTax: Double,
    val net: Double,
    val day: Int
)

/** Estado serializable del subsistema MultiCity. */
@Serializable
data class MultiCityState(
    val unlocked: Boolean = false,
    val cities: List<CityMarket> = emptyList(),
    val routes: List<CityRoute> = emptyList(),
    val shipments: List<CityShipment> = emptyList(),
    /** Últimos 30 envíos liquidados. */
    val history: List<CityShipmentResult> = emptyList(),
    val lastDailyTick: Long = 0L,
    val totalShipped: Long = 0L,
    val totalRevenue: Double = 0.0,
    val totalNet: Double = 0.0
)

/**
 * Catálogo: estado fresco con 5 ciudades + HOME, multipliers y rutas
 * predeterminados desde HOME a cada destino.
 */
object MultiCityCatalog {

    private const val DAY_TICKS = 1_440L

    private fun homeCity() = CityMarket(
        id = CityId.HOME,
        priceMultipliers = emptyMap(),  // se referencia el Market base
        demand = emptyMap(),
        exchangeRate = 1.0,
        importTariff = 0.0,
        exportTariff = 0.0,
        localTax = 0.20,
        volatility = 0.0
    )

    private fun neoTokyo() = CityMarket(
        id = CityId.NEO_TOKYO,
        priceMultipliers = mapOf(
            "smartphone" to 1.45, "circuit" to 1.50, "software" to 1.30,
            "battery" to 1.25, "car" to 1.10, "yacht" to 0.95,
            "oil" to 1.05, "rice" to 0.0  // resource no existe — se ignora
        ).filter { it.value > 0.0 },
        demand = mapOf(
            "smartphone" to 1.6, "circuit" to 1.5, "software" to 1.4
        ),
        exchangeRate = 0.92,
        importTariff = 0.10,
        exportTariff = 0.06,
        localTax = 0.18,
        volatility = 0.04
    )

    private fun dubaiCity() = CityMarket(
        id = CityId.DUBAI_CITY,
        priceMultipliers = mapOf(
            "oil" to 0.65, "jewelry" to 0.80, "yacht" to 0.70,
            "smartphone" to 1.10, "car" to 1.20, "software" to 1.05,
            "circuit" to 1.10
        ),
        demand = mapOf(
            "yacht" to 1.8, "jewelry" to 1.7, "car" to 1.4
        ),
        exchangeRate = 1.10,
        importTariff = 0.04,
        exportTariff = 0.02,
        localTax = 0.05,        // paraíso fiscal
        volatility = 0.06
    )

    private fun lagosBay() = CityMarket(
        id = CityId.LAGOS_BAY,
        priceMultipliers = mapOf(
            "oil" to 0.55, "iron_ore" to 0.50, "coal" to 0.55,
            "wood_log" to 0.60, "wheat" to 0.70, "seed" to 0.65,
            "smartphone" to 1.40, "car" to 1.65, "software" to 1.20,
            "consulting" to 1.10, "battery" to 1.30
        ),
        demand = mapOf(
            "smartphone" to 1.3, "battery" to 1.5, "consulting" to 1.4
        ),
        exchangeRate = 0.85,
        importTariff = 0.18,
        exportTariff = 0.08,
        localTax = 0.22,
        volatility = 0.10
    )

    private fun newCoast() = CityMarket(
        id = CityId.NEW_COAST,
        priceMultipliers = mapOf(
            "software" to 1.40, "consulting" to 1.50, "smartphone" to 1.20,
            "car" to 1.15, "circuit" to 1.20, "engine" to 1.10,
            "oil" to 1.05, "yacht" to 1.10, "jewelry" to 1.05
        ),
        demand = mapOf(
            "software" to 1.5, "consulting" to 1.6, "smartphone" to 1.3
        ),
        exchangeRate = 0.95,
        importTariff = 0.08,
        exportTariff = 0.05,
        localTax = 0.24,
        volatility = 0.05
    )

    private fun berlinNeu() = CityMarket(
        id = CityId.BERLIN_NEU,
        priceMultipliers = mapOf(
            "steel" to 0.85, "gear" to 0.85, "engine" to 0.88,
            "car" to 1.05, "iron_ingot" to 0.90, "plank" to 0.95,
            "circuit" to 1.05, "smartphone" to 1.10, "software" to 1.10
        ),
        demand = mapOf(
            "car" to 1.4, "engine" to 1.3, "circuit" to 1.2
        ),
        exchangeRate = 1.0,
        importTariff = 0.06,
        exportTariff = 0.05,
        localTax = 0.25,
        volatility = 0.03
    )

    private fun freshCities(): List<CityMarket> = listOf(
        homeCity(),
        neoTokyo(),
        dubaiCity(),
        lagosBay(),
        newCoast(),
        berlinNeu()
    )

    private fun freshRoutes(): List<CityRoute> {
        // Rutas HOME ↔ cada ciudad. Empezamos solo HOME → ciudad para no doblar
        // y simplificar la UI; la vuelta puede añadirse en futuras iteraciones.
        fun route(to: CityId, daily: Double, days: Int): List<CityRoute> = listOf(
            CityRoute(
                id = "r_home_${to.name.lowercase()}",
                from = CityId.HOME, to = to,
                dailyCost = daily,
                transitTicks = days * DAY_TICKS,
                capacityPerShipment = 1_000
            )
        )
        return route(CityId.NEO_TOKYO, 4_500.0, 3) +
            route(CityId.DUBAI_CITY, 3_500.0, 2) +
            route(CityId.LAGOS_BAY, 5_000.0, 4) +
            route(CityId.NEW_COAST, 4_000.0, 3) +
            route(CityId.BERLIN_NEU, 1_800.0, 1)
    }

    fun freshState(): MultiCityState = MultiCityState(
        unlocked = false,
        cities = freshCities(),
        routes = freshRoutes(),
        shipments = emptyList()
    )

    fun cityById(state: MultiCityState, id: CityId): CityMarket? =
        state.cities.find { it.id == id }
}
