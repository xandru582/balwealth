package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Sistema de vehículos: catálogo, garaje y stats.
 *
 * Marcas ficticias para evitar IP (Iberi, Tronix, Aure, Vanguard, Wraith,
 * Velocra, Saetta, Volt, Heritage). Cada coche tiene stats que afectan
 * conducción y prestigio del jugador.
 */
enum class CarBrand(val displayName: String, val tier: Int /* 1-5 */) {
    IBERI("Iberi", 1),               // económico
    TRONIX("Tronix", 1),             // urbano
    AURE("Auré", 2),                 // compacto premium
    VANGUARD("Vanguard", 3),         // sedán ejecutivo
    HERITAGE("Heritage", 3),         // clásico
    VOLT("Volt", 4),                 // eléctrico tech
    WRAITH("Wraith", 5),             // lujo
    VELOCRA("Velocra", 4),           // deportivo
    SAETTA("Saetta", 5)              // hipercoche
}

/** Tipo de carrocería — afecta sprite. */
enum class CarBodyType {
    HATCHBACK, SEDAN, SUV, COUPE, CONVERTIBLE, SUPERCAR, LIMO, ELECTRIC_POD, CLASSIC, TRUCK
}

/** Modelo concreto de coche. */
data class CarModel(
    val id: String,
    val brand: CarBrand,
    val displayName: String,
    val body: CarBodyType,
    val price: Double,
    val topSpeed: Float,           // 1.0 = avatar a pie. Coches: 2.5 - 12.0
    val handling: Float,           // 0.5 - 1.5 — qué tan rápido gira
    val prestige: Int,             // 0-100 cuánta reputación da poseerlo
    val happinessBoost: Int,       // bonus al subirse
    val description: String,
    val primaryColor: Long,        // ARGB Long
    val secondaryColor: Long,
    val accentColor: Long,
    val hasSpoiler: Boolean = false,
    val hasLEDLights: Boolean = false,
    val isConvertible: Boolean = false,
    val isClassic: Boolean = false,
    val isElectric: Boolean = false,
    val emoji: String = "🚗"
)

object CarCatalog {
    val all: List<CarModel> = listOf(
        // -------- ECONÓMICOS / TIER 1 --------
        CarModel("ib_pequeñin", CarBrand.IBERI, "Iberi Pequeñín", CarBodyType.HATCHBACK,
            price = 6_500.0, topSpeed = 2.8f, handling = 1.1f, prestige = 1, happinessBoost = 2,
            description = "El primer coche de muchos. Pequeño, ágil, fiable.",
            primaryColor = 0xFFE53935, secondaryColor = 0xFFB71C1C, accentColor = 0xFF263238,
            emoji = "🚗"),

        CarModel("ib_familiar", CarBrand.IBERI, "Iberi Familiar", CarBodyType.SEDAN,
            price = 14_500.0, topSpeed = 3.2f, handling = 1.0f, prestige = 4, happinessBoost = 3,
            description = "Espacioso, práctico, sin pretensiones.",
            primaryColor = 0xFF1E88E5, secondaryColor = 0xFF1565C0, accentColor = 0xFF263238),

        CarModel("tx_minicity", CarBrand.TRONIX, "Tronix MiniCity", CarBodyType.HATCHBACK,
            price = 9_800.0, topSpeed = 2.9f, handling = 1.3f, prestige = 3, happinessBoost = 3,
            description = "Urbano por excelencia. Aparca en cualquier hueco.",
            primaryColor = 0xFFFFEB3B, secondaryColor = 0xFFFBC02D, accentColor = 0xFF263238),

        CarModel("tx_mocasin", CarBrand.TRONIX, "Tronix Mocasín", CarBodyType.SEDAN,
            price = 18_000.0, topSpeed = 3.4f, handling = 1.0f, prestige = 6, happinessBoost = 4,
            description = "Confort suave para el día a día.",
            primaryColor = 0xFF607D8B, secondaryColor = 0xFF455A64, accentColor = 0xFFFFFFFF),

        // -------- COMPACTO PREMIUM / TIER 2 --------
        CarModel("au_a3", CarBrand.AURE, "Auré A3 Sport", CarBodyType.HATCHBACK,
            price = 28_000.0, topSpeed = 4.2f, handling = 1.3f, prestige = 18, happinessBoost = 8,
            description = "El pasaporte a la gama premium.",
            primaryColor = 0xFFFFFFFF, secondaryColor = 0xFFE0E0E0, accentColor = 0xFF263238,
            hasLEDLights = true),

        CarModel("au_q5", CarBrand.AURE, "Auré Q5 SUV", CarBodyType.SUV,
            price = 52_000.0, topSpeed = 4.5f, handling = 0.95f, prestige = 28, happinessBoost = 10,
            description = "SUV elegante para la familia exigente.",
            primaryColor = 0xFF263238, secondaryColor = 0xFF1B1B1B, accentColor = 0xFFB0BEC5,
            hasLEDLights = true,
            emoji = "🚙"),

        // -------- VANGUARD / TIER 3 EJECUTIVO --------
        CarModel("vg_executive", CarBrand.VANGUARD, "Vanguard Executive", CarBodyType.SEDAN,
            price = 78_000.0, topSpeed = 5.0f, handling = 1.0f, prestige = 40, happinessBoost = 14,
            description = "El sedán que dice 'he llegado'.",
            primaryColor = 0xFF1A237E, secondaryColor = 0xFF0D47A1, accentColor = 0xFFFFD700,
            hasLEDLights = true),

        CarModel("vg_signature", CarBrand.VANGUARD, "Vanguard Signature SUV", CarBodyType.SUV,
            price = 118_000.0, topSpeed = 4.8f, handling = 0.9f, prestige = 50, happinessBoost = 16,
            description = "SUV ejecutivo con presencia imponente.",
            primaryColor = 0xFF1B1B1B, secondaryColor = 0xFF0D0D0D, accentColor = 0xFFFFD166,
            hasLEDLights = true,
            emoji = "🚙"),

        CarModel("vg_limo", CarBrand.VANGUARD, "Vanguard Limousine", CarBodyType.LIMO,
            price = 240_000.0, topSpeed = 4.6f, handling = 0.8f, prestige = 65, happinessBoost = 22,
            description = "Llega como un magnate.",
            primaryColor = 0xFF000000, secondaryColor = 0xFF1B1B1B, accentColor = 0xFFFFD700,
            hasLEDLights = true),

        // -------- HERITAGE / CLÁSICOS / TIER 3 --------
        CarModel("hr_classic_60", CarBrand.HERITAGE, "Heritage Classic '60", CarBodyType.CLASSIC,
            price = 95_000.0, topSpeed = 3.8f, handling = 0.85f, prestige = 35, happinessBoost = 18,
            description = "Un clásico de coleccionista. La gente girará la cabeza.",
            primaryColor = 0xFF8B0000, secondaryColor = 0xFF5D0000, accentColor = 0xFFFFFFFF,
            isClassic = true),

        CarModel("hr_roadster_70", CarBrand.HERITAGE, "Heritage Roadster '70", CarBodyType.CONVERTIBLE,
            price = 140_000.0, topSpeed = 4.5f, handling = 1.1f, prestige = 42, happinessBoost = 24,
            description = "Descapotable de los 70. Estilo atemporal.",
            primaryColor = 0xFFFFD700, secondaryColor = 0xFFE6A23C, accentColor = 0xFF8B4513,
            isClassic = true,
            isConvertible = true,
            emoji = "🏎️"),

        // -------- VOLT / ELÉCTRICOS / TIER 4 --------
        CarModel("vt_model_s", CarBrand.VOLT, "Volt Model S Plaid", CarBodyType.SEDAN,
            price = 110_000.0, topSpeed = 6.2f, handling = 1.4f, prestige = 45, happinessBoost = 18,
            description = "Aceleración demencial. Cero emisiones.",
            primaryColor = 0xFFFFFFFF, secondaryColor = 0xFFE0E0E0, accentColor = 0xFF1A237E,
            hasLEDLights = true,
            isElectric = true,
            emoji = "⚡"),

        CarModel("vt_cyberbox", CarBrand.VOLT, "Volt Cyberbox", CarBodyType.TRUCK,
            price = 95_000.0, topSpeed = 5.5f, handling = 0.9f, prestige = 55, happinessBoost = 25,
            description = "Diseño angular brutal. Rompe esquemas.",
            primaryColor = 0xFFB0BEC5, secondaryColor = 0xFF78909C, accentColor = 0xFF263238,
            hasLEDLights = true,
            isElectric = true,
            emoji = "🚙"),

        CarModel("vt_pod", CarBrand.VOLT, "Volt CityPod", CarBodyType.ELECTRIC_POD,
            price = 35_000.0, topSpeed = 4.0f, handling = 1.6f, prestige = 22, happinessBoost = 8,
            description = "Cápsula urbana autónoma. El futuro hoy.",
            primaryColor = 0xFF03A9F4, secondaryColor = 0xFF0288D1, accentColor = 0xFFFFFFFF,
            hasLEDLights = true,
            isElectric = true),

        // -------- VELOCRA / DEPORTIVOS / TIER 4 --------
        CarModel("vc_aspid", CarBrand.VELOCRA, "Velocra Áspid", CarBodyType.COUPE,
            price = 160_000.0, topSpeed = 7.5f, handling = 1.5f, prestige = 60, happinessBoost = 28,
            description = "Deportivo italiano puro. Sonido envolvente.",
            primaryColor = 0xFFD32F2F, secondaryColor = 0xFFB71C1C, accentColor = 0xFF000000,
            hasSpoiler = true,
            hasLEDLights = true,
            emoji = "🏎️"),

        CarModel("vc_furia", CarBrand.VELOCRA, "Velocra Furia GT", CarBodyType.SUPERCAR,
            price = 320_000.0, topSpeed = 9.5f, handling = 1.4f, prestige = 75, happinessBoost = 35,
            description = "GT pura sangre. 0-100 en 2.6 segundos.",
            primaryColor = 0xFFFFA000, secondaryColor = 0xFFFF6F00, accentColor = 0xFF000000,
            hasSpoiler = true,
            hasLEDLights = true,
            emoji = "🏎️"),

        CarModel("vc_descapotable", CarBrand.VELOCRA, "Velocra Mistral", CarBodyType.CONVERTIBLE,
            price = 280_000.0, topSpeed = 7.8f, handling = 1.5f, prestige = 65, happinessBoost = 32,
            description = "Convertible deportivo. Para quien le gusta sentir el viento.",
            primaryColor = 0xFFE91E63, secondaryColor = 0xFFAD1457, accentColor = 0xFFFFFFFF,
            hasLEDLights = true,
            isConvertible = true,
            emoji = "🏎️"),

        // -------- WRAITH / LUJO / TIER 5 --------
        CarModel("wt_phantom", CarBrand.WRAITH, "Wraith Phantom", CarBodyType.LIMO,
            price = 580_000.0, topSpeed = 5.5f, handling = 0.85f, prestige = 90, happinessBoost = 40,
            description = "El estándar mundial del lujo. Las puertas se abren al revés.",
            primaryColor = 0xFF000000, secondaryColor = 0xFF1B1B1B, accentColor = 0xFFC0A062,
            hasLEDLights = true,
            emoji = "🚗"),

        CarModel("wt_drophead", CarBrand.WRAITH, "Wraith Drophead", CarBodyType.CONVERTIBLE,
            price = 720_000.0, topSpeed = 5.8f, handling = 0.9f, prestige = 92, happinessBoost = 45,
            description = "Lujo descapotable. La cumbre.",
            primaryColor = 0xFFFFFFFF, secondaryColor = 0xFFE0E0E0, accentColor = 0xFFC0A062,
            hasLEDLights = true,
            isConvertible = true,
            emoji = "🏎️"),

        // -------- SAETTA / HIPERCOCHES / TIER 5 --------
        CarModel("st_lux_one", CarBrand.SAETTA, "Saetta Lux One", CarBodyType.SUPERCAR,
            price = 1_800_000.0, topSpeed = 11.0f, handling = 1.6f, prestige = 95, happinessBoost = 50,
            description = "1500cv en una caja de carbono. Solo 50 unidades en el mundo.",
            primaryColor = 0xFF1B5E20, secondaryColor = 0xFF003300, accentColor = 0xFFFFD700,
            hasSpoiler = true,
            hasLEDLights = true,
            emoji = "🏎️"),

        CarModel("st_imperator", CarBrand.SAETTA, "Saetta Imperator W16", CarBodyType.SUPERCAR,
            price = 3_500_000.0, topSpeed = 12.0f, handling = 1.5f, prestige = 100, happinessBoost = 60,
            description = "El coche de producción más rápido jamás fabricado. 490 km/h.",
            primaryColor = 0xFF000000, secondaryColor = 0xFF1A1A1A, accentColor = 0xFFFFA000,
            hasSpoiler = true,
            hasLEDLights = true,
            emoji = "🏎️"),

        CarModel("st_volcano", CarBrand.SAETTA, "Saetta Volcano AWD", CarBodyType.SUPERCAR,
            price = 2_400_000.0, topSpeed = 11.5f, handling = 1.7f, prestige = 98, happinessBoost = 55,
            description = "Hipercoche eléctrico de 1900cv. Aceleración brutal.",
            primaryColor = 0xFFFF3D00, secondaryColor = 0xFFE65100, accentColor = 0xFF000000,
            hasSpoiler = true,
            hasLEDLights = true,
            isElectric = true,
            emoji = "🏎️"),

        // -------- ESPECIALES --------
        CarModel("hr_tractor", CarBrand.HERITAGE, "Tractor Granjero", CarBodyType.TRUCK,
            price = 12_000.0, topSpeed = 1.8f, handling = 0.6f, prestige = 0, happinessBoost = 6,
            description = "No es bonito, no es rápido, pero te encanta.",
            primaryColor = 0xFF1B5E20, secondaryColor = 0xFF003300, accentColor = 0xFFFFEB3B,
            emoji = "🚜"),

        CarModel("vg_taxi", CarBrand.VANGUARD, "Vanguard Taxi", CarBodyType.SEDAN,
            price = 22_000.0, topSpeed = 3.6f, handling = 1.0f, prestige = 5, happinessBoost = 4,
            description = "El icónico taxi de la ciudad. Genera ingresos pasivos.",
            primaryColor = 0xFFFFEB3B, secondaryColor = 0xFFFBC02D, accentColor = 0xFF263238,
            emoji = "🚕")
    )

    fun byId(id: String): CarModel? = all.find { it.id == id }
    fun byBrand(brand: CarBrand): List<CarModel> = all.filter { it.brand == brand }
    fun affordable(cash: Double): List<CarModel> = all.filter { it.price <= cash }.sortedBy { it.price }
}

/** Coche poseído (instancia con kilómetros, customizado). */
@Serializable
data class OwnedCar(
    val instanceId: String,
    val modelId: String,
    val nickname: String? = null,
    val kilometers: Double = 0.0,
    val purchasedAtTick: Long = 0L,
    val customColor: Long? = null,    // override del color primario
    val plateNumber: String = "ABC-1234"
) {
    fun model(): CarModel = CarCatalog.byId(modelId) ?: CarCatalog.all.first()
}

/** Garaje del jugador. */
@Serializable
data class GarageState(
    val cars: List<OwnedCar> = emptyList(),
    val currentlyDrivingId: String? = null,
    val maxSlots: Int = 3
) {
    val isDriving: Boolean get() = currentlyDrivingId != null
    fun current(): OwnedCar? = cars.find { it.instanceId == currentlyDrivingId }
    val canFitMore: Boolean get() = cars.size < maxSlots
}
