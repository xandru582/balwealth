package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Sistema de Formula Manager: circuitos, equipos, pilotos, coches y campeonato.
 *
 * Resumen de gameplay:
 *  - El jugador puede COMPRAR un equipo (alto coste) y pasa a ser su dueño.
 *  - Cada 7 días in-game se corre la siguiente carrera del calendario.
 *  - El equipo del jugador genera ingresos por patrocinio + premios + brand.
 *  - Pagos diarios: salarios de pilotos + mantenimiento del coche.
 *  - Upgrades del coche: motor, aerodinámica, fiabilidad (cuestan dinero).
 *  - Contratar/despedir pilotos del mercado libre.
 *  - Al final de cada temporada (24 carreras) se reparten premios extra.
 */

// =====================================================================
//                              CIRCUITOS
// =====================================================================

@Serializable
data class RaceCircuit(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val lengthKm: Double,
    val laps: Int,
    val corners: Int,
    val difficulty: Int,             // 1..10 — afecta a la varianza de resultados
    val avgTopSpeedKmh: Int,
    val downforceLevel: Int,         // 1..5 — 1=alta velocidad, 5=máxima carga
    val basePrize: Double            // premio base 1er puesto
)

object CircuitCatalog {
    val all: List<RaceCircuit> = listOf(
        RaceCircuit("monaco",       "Mónaco",          "Mónaco",        "🇲🇨", 3.337, 78, 19, 10, 290, 5, 850_000.0),
        RaceCircuit("monza",        "Monza",           "Italia",        "🇮🇹", 5.793, 53, 11,  5, 360, 1, 700_000.0),
        RaceCircuit("spa",          "Spa-Francorchamps", "Bélgica",     "🇧🇪", 7.004, 44, 19,  9, 340, 2, 750_000.0),
        RaceCircuit("silverstone",  "Silverstone",     "Reino Unido",   "🇬🇧", 5.891, 52, 18,  8, 320, 3, 680_000.0),
        RaceCircuit("suzuka",       "Suzuka",          "Japón",         "🇯🇵", 5.807, 53, 18,  9, 310, 3, 720_000.0),
        RaceCircuit("interlagos",   "Interlagos",      "Brasil",        "🇧🇷", 4.309, 71, 15,  7, 305, 3, 660_000.0),
        RaceCircuit("nurburgring",  "Nürburgring",     "Alemania",      "🇩🇪", 5.148, 60, 15,  7, 300, 3, 640_000.0),
        RaceCircuit("catalunya",    "Catalunya",       "España",        "🇪🇸", 4.657, 66, 16,  6, 295, 4, 620_000.0),
        RaceCircuit("imola",        "Imola",           "Italia",        "🇮🇹", 4.909, 63, 19,  8, 300, 4, 640_000.0),
        RaceCircuit("bahrain",      "Sakhir",          "Bahréin",       "🇧🇭", 5.412, 57, 15,  6, 320, 2, 600_000.0),
        RaceCircuit("singapore",    "Marina Bay",      "Singapur",      "🇸🇬", 4.940, 61, 23,  9, 285, 5, 780_000.0),
        RaceCircuit("zandvoort",    "Zandvoort",       "Países Bajos",  "🇳🇱", 4.259, 72, 14,  7, 310, 3, 600_000.0),
        RaceCircuit("austin",       "Circuit of Americas", "EE.UU.",    "🇺🇸", 5.513, 56, 20,  7, 315, 3, 640_000.0),
        RaceCircuit("mexico",       "Hermanos Rodríguez", "México",     "🇲🇽", 4.304, 71, 17,  6, 305, 3, 580_000.0),
        RaceCircuit("yas",          "Yas Marina",      "Emiratos",      "🇦🇪", 5.281, 58, 16,  6, 320, 3, 700_000.0),
        RaceCircuit("hungaroring",  "Hungaroring",     "Hungría",       "🇭🇺", 4.381, 70, 14,  7, 290, 4, 580_000.0)
    )

    fun byId(id: String): RaceCircuit? = all.firstOrNull { it.id == id }
}

// =====================================================================
//                              PILOTOS
// =====================================================================

@Serializable
data class RaceDriver(
    val id: String,
    val name: String,
    val nationality: String,
    val flag: String,
    val age: Int,
    val skill: Int,                  // 50..99
    val aggression: Int,             // 30..95
    val consistency: Int,            // 40..98
    val salaryPerDay: Double,
    val morale: Int = 70,            // 0..100
    val contractEndDay: Int = 0,
    val seasonPoints: Int = 0,
    val careerWins: Int = 0,
    val careerPodiums: Int = 0,
    // === Stats detalladas ===
    val careerStarts: Int = 0,
    val careerPoles: Int = 0,
    val careerFastestLaps: Int = 0,
    val careerDNFs: Int = 0,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
    val careerPoints: Int = 0,
    val bestSeasonPoints: Int = 0,
    val championships: Int = 0,
    val avgFinishSum: Int = 0,        // suma para calcular media
    val finishCount: Int = 0,         // veces que terminó (no DNF)
    // === Specialties (1..100) ===
    val rainSkill: Int = 60,
    val streetSkill: Int = 60,
    val highSpeedSkill: Int = 60,
    val qualifyingPace: Int = 60,
    val tyreManagement: Int = 60,
    val overtaking: Int = 60
) {
    /** Rendimiento bruto del piloto (0..100). */
    fun overall(): Int = ((skill * 0.5 + consistency * 0.3 + aggression * 0.2)).toInt()
    /** Posición media de llegada (0 = sin datos). */
    fun avgFinish(): Float = if (finishCount == 0) 0f else avgFinishSum.toFloat() / finishCount
    /** Win-rate (% de victorias sobre starts). */
    fun winRate(): Float = if (careerStarts == 0) 0f else 100f * careerWins / careerStarts
    /** Podium-rate. */
    fun podiumRate(): Float = if (careerStarts == 0) 0f else 100f * careerPodiums / careerStarts
    /** Specialty rating combinado (1..100). */
    fun specialtyAvg(): Int = (rainSkill + streetSkill + highSpeedSkill + qualifyingPace + tyreManagement + overtaking) / 6
}

object DriverPool {
    /** Pool de 32 pilotos con specialties únicas (rain/street/highSpeed/quali/tyre/overtaking). */
    val all: List<RaceDriver> = listOf(
        RaceDriver("d_velocci",  "Marco Velocci",     "Italia",       "🇮🇹", 27, 96, 80, 95, 38_000.0,
            rainSkill = 92, streetSkill = 95, highSpeedSkill = 90, qualifyingPace = 96, tyreManagement = 88, overtaking = 86),
        RaceDriver("d_haldur",   "Erik Haldur",       "Noruega",      "🇳🇴", 24, 94, 75, 92, 32_000.0,
            rainSkill = 95, streetSkill = 80, highSpeedSkill = 92, qualifyingPace = 90, tyreManagement = 85, overtaking = 82),
        RaceDriver("d_castell",  "Diego Castell",     "España",       "🇪🇸", 29, 92, 88, 88, 28_000.0,
            rainSkill = 80, streetSkill = 90, highSpeedSkill = 88, qualifyingPace = 88, tyreManagement = 82, overtaking = 92),
        RaceDriver("d_okabe",    "Hiro Okabe",        "Japón",        "🇯🇵", 26, 91, 70, 95, 26_000.0,
            rainSkill = 88, streetSkill = 85, highSpeedSkill = 86, qualifyingPace = 92, tyreManagement = 95, overtaking = 75),
        RaceDriver("d_blanchet", "Antoine Blanchet",  "Francia",      "🇫🇷", 31, 90, 78, 90, 25_000.0,
            rainSkill = 86, streetSkill = 88, highSpeedSkill = 84, qualifyingPace = 86, tyreManagement = 90, overtaking = 80),
        RaceDriver("d_lindqvist","Sven Lindqvist",    "Suecia",       "🇸🇪", 28, 89, 72, 91, 22_000.0,
            rainSkill = 92, streetSkill = 78, highSpeedSkill = 88, qualifyingPace = 84, tyreManagement = 88, overtaking = 76),
        RaceDriver("d_oliveira", "Bruno Oliveira",    "Brasil",       "🇧🇷", 25, 88, 92, 80, 21_000.0,
            rainSkill = 95, streetSkill = 82, highSpeedSkill = 84, qualifyingPace = 80, tyreManagement = 70, overtaking = 95),
        RaceDriver("d_mclark",   "James McClark",     "Reino Unido",  "🇬🇧", 30, 87, 76, 89, 20_000.0,
            rainSkill = 94, streetSkill = 78, highSpeedSkill = 86, qualifyingPace = 84, tyreManagement = 86, overtaking = 80),
        RaceDriver("d_rodríguez","Felipe Rodríguez",  "México",       "🇲🇽", 32, 86, 84, 82, 18_000.0,
            rainSkill = 75, streetSkill = 86, highSpeedSkill = 82, qualifyingPace = 80, tyreManagement = 78, overtaking = 88),
        RaceDriver("d_kowalski", "Janusz Kowalski",   "Polonia",      "🇵🇱", 26, 85, 70, 88, 17_000.0,
            rainSkill = 80, streetSkill = 76, highSpeedSkill = 88, qualifyingPace = 82, tyreManagement = 84, overtaking = 72),
        RaceDriver("d_martens",  "Lars Martens",      "Países Bajos", "🇳🇱", 23, 84, 73, 85, 16_000.0,
            rainSkill = 84, streetSkill = 72, highSpeedSkill = 84, qualifyingPace = 80, tyreManagement = 80, overtaking = 75),
        RaceDriver("d_chen",     "Wei Chen",          "China",        "🇨🇳", 25, 83, 75, 86, 15_000.0,
            rainSkill = 78, streetSkill = 80, highSpeedSkill = 82, qualifyingPace = 82, tyreManagement = 85, overtaking = 75),
        RaceDriver("d_stiller",  "Klaus Stiller",     "Alemania",     "🇩🇪", 33, 82, 68, 92, 14_000.0,
            rainSkill = 88, streetSkill = 75, highSpeedSkill = 86, qualifyingPace = 78, tyreManagement = 92, overtaking = 70),
        RaceDriver("d_petrov",   "Alexei Petrov",     "Rusia",        "🇷🇺", 28, 81, 80, 80, 13_000.0,
            rainSkill = 82, streetSkill = 76, highSpeedSkill = 80, qualifyingPace = 78, tyreManagement = 75, overtaking = 84),
        RaceDriver("d_anders",   "Mikkel Andersen",   "Dinamarca",    "🇩🇰", 24, 80, 72, 84, 12_000.0,
            rainSkill = 80, streetSkill = 72, highSpeedSkill = 78, qualifyingPace = 80, tyreManagement = 82, overtaking = 72),
        RaceDriver("d_silva",    "Rafael Silva",      "Portugal",     "🇵🇹", 27, 79, 78, 82, 11_500.0,
            rainSkill = 76, streetSkill = 80, highSpeedSkill = 76, qualifyingPace = 78, tyreManagement = 78, overtaking = 80),
        RaceDriver("d_doyle",    "Connor Doyle",      "Irlanda",      "🇮🇪", 26, 78, 81, 79, 11_000.0,
            rainSkill = 88, streetSkill = 74, highSpeedSkill = 76, qualifyingPace = 76, tyreManagement = 70, overtaking = 82),
        RaceDriver("d_kovac",    "Marek Kovač",       "Eslovaquia",   "🇸🇰", 29, 77, 70, 86, 10_500.0,
            rainSkill = 78, streetSkill = 75, highSpeedSkill = 78, qualifyingPace = 74, tyreManagement = 84, overtaking = 70),
        RaceDriver("d_papadakis","Yannis Papadakis",  "Grecia",       "🇬🇷", 31, 76, 73, 84, 10_000.0,
            rainSkill = 75, streetSkill = 78, highSpeedSkill = 74, qualifyingPace = 72, tyreManagement = 80, overtaking = 72),
        RaceDriver("d_bodenova", "Karin Bodenova",    "Chequia",      "🇨🇿", 25, 75, 76, 80, 9_500.0,
            rainSkill = 80, streetSkill = 76, highSpeedSkill = 72, qualifyingPace = 76, tyreManagement = 76, overtaking = 74),
        RaceDriver("d_alsina",   "Pol Alsina",        "España",       "🇪🇸", 22, 74, 80, 75, 9_000.0,
            rainSkill = 70, streetSkill = 78, highSpeedSkill = 72, qualifyingPace = 78, tyreManagement = 68, overtaking = 82),
        RaceDriver("d_hassan",   "Karim Hassan",      "Egipto",       "🇪🇬", 26, 73, 72, 80, 8_500.0,
            rainSkill = 65, streetSkill = 78, highSpeedSkill = 75, qualifyingPace = 72, tyreManagement = 76, overtaking = 72),
        RaceDriver("d_walden",   "Tom Walden",        "Australia",    "🇦🇺", 28, 72, 78, 78, 8_000.0,
            rainSkill = 78, streetSkill = 70, highSpeedSkill = 76, qualifyingPace = 72, tyreManagement = 72, overtaking = 78),
        RaceDriver("d_okonkwo",  "Chuka Okonkwo",     "Nigeria",      "🇳🇬", 24, 71, 84, 72, 7_500.0,
            rainSkill = 72, streetSkill = 72, highSpeedSkill = 70, qualifyingPace = 70, tyreManagement = 65, overtaking = 86),
        RaceDriver("d_park",     "Jin-ho Park",       "Corea",        "🇰🇷", 23, 70, 70, 78, 7_000.0,
            rainSkill = 72, streetSkill = 70, highSpeedSkill = 72, qualifyingPace = 72, tyreManagement = 75, overtaking = 68),
        RaceDriver("d_fairbank", "Henry Fairbank",    "EE.UU.",       "🇺🇸", 30, 69, 75, 76, 6_500.0,
            rainSkill = 65, streetSkill = 68, highSpeedSkill = 78, qualifyingPace = 70, tyreManagement = 72, overtaking = 75),
        RaceDriver("d_rosenberg","Eitan Rosenberg",   "Israel",       "🇮🇱", 27, 68, 70, 77, 6_000.0,
            rainSkill = 70, streetSkill = 72, highSpeedSkill = 68, qualifyingPace = 68, tyreManagement = 75, overtaking = 70),
        RaceDriver("d_jorge",    "Lucas Jorge",       "Argentina",    "🇦🇷", 25, 67, 80, 70, 5_500.0,
            rainSkill = 75, streetSkill = 70, highSpeedSkill = 68, qualifyingPace = 70, tyreManagement = 65, overtaking = 80),
        RaceDriver("d_yip",      "Carmen Yip",        "Hong Kong",    "🇭🇰", 23, 66, 70, 76, 5_000.0,
            rainSkill = 68, streetSkill = 76, highSpeedSkill = 65, qualifyingPace = 70, tyreManagement = 72, overtaking = 68),
        RaceDriver("d_thunder",  "Bo Thunderbear",    "Canadá",       "🇨🇦", 26, 64, 78, 72, 4_500.0,
            rainSkill = 78, streetSkill = 65, highSpeedSkill = 70, qualifyingPace = 65, tyreManagement = 65, overtaking = 78),
        RaceDriver("d_amir",     "Saif Al-Amir",      "Catar",        "🇶🇦", 22, 60, 65, 70, 3_500.0,
            rainSkill = 55, streetSkill = 70, highSpeedSkill = 65, qualifyingPace = 62, tyreManagement = 68, overtaking = 60),
        RaceDriver("d_rookie",   "Sam Greenfield",    "Nueva Zelanda","🇳🇿", 21, 58, 68, 65, 2_800.0,
            rainSkill = 60, streetSkill = 60, highSpeedSkill = 62, qualifyingPace = 60, tyreManagement = 58, overtaking = 65)
    )

    fun byId(id: String): RaceDriver? = all.firstOrNull { it.id == id }
}

// =====================================================================
//                          PATROCINADORES
// =====================================================================

@Serializable
enum class SponsorTier(val label: String, val color: Long, val emoji: String) {
    BRONZE("Bronce",   0xFFCD7F32, "🥉"),
    SILVER("Plata",    0xFFC0C0C0, "🥈"),
    GOLD("Oro",        0xFFFFD700, "🥇"),
    PLATINUM("Platino",0xFFE5E4E2, "💎"),
    TITANIUM("Titanio",0xFF7FFFD4, "👑")
}

@Serializable
data class Sponsor(
    val id: String,
    val brand: String,
    val sector: String,
    val country: String,
    val flag: String,
    val tier: SponsorTier,
    val baseDailyPay: Double,
    val winBonus: Double,            // bonus único por victoria
    val podiumBonus: Double,         // bonus único por podio
    val pointsBonus: Double,         // pago por punto al final de cada carrera
    val minBrandRequired: Int,       // brand value del equipo mínimo
    val contractDays: Int,           // duración del contrato
    val signOnFee: Double            // prima de firma (capital adelantado, no se devuelve)
)

object SponsorCatalog {
    val all: List<Sponsor> = listOf(
        // === TITANIUM (5) — solo equipos top ===
        Sponsor("sp_apex_aero",   "Aerodynamiq",      "Aeroespacial",     "Estados Unidos",  "🇺🇸", SponsorTier.TITANIUM, 220_000.0, 350_000.0, 120_000.0, 8_500.0, 90, 365, 5_000_000.0),
        Sponsor("sp_quantum",     "QuantumChip",      "Semiconductores",  "Taiwán",          "🇹🇼", SponsorTier.TITANIUM, 200_000.0, 300_000.0, 100_000.0, 7_500.0, 88, 365, 4_500_000.0),
        Sponsor("sp_globalfin",   "Global Capital",   "Finanzas",         "Suiza",           "🇨🇭", SponsorTier.TITANIUM, 180_000.0, 280_000.0,  90_000.0, 7_000.0, 85, 365, 4_000_000.0),
        Sponsor("sp_rolex",       "ChronoLux",        "Relojería",        "Suiza",           "🇨🇭", SponsorTier.TITANIUM, 195_000.0, 320_000.0, 110_000.0, 7_800.0, 87, 365, 4_300_000.0),
        Sponsor("sp_petromax",    "PetroMax",         "Energía",          "Catar",           "🇶🇦", SponsorTier.TITANIUM, 240_000.0, 380_000.0, 130_000.0, 9_000.0, 92, 365, 5_500_000.0),

        // === PLATINUM (5) ===
        Sponsor("sp_techfront",   "TechFront",        "Tecnología",       "Reino Unido",     "🇬🇧", SponsorTier.PLATINUM, 130_000.0, 200_000.0, 70_000.0, 5_500.0, 75, 240, 2_500_000.0),
        Sponsor("sp_voltaire",    "Voltaire Energy",  "Energía",          "Francia",         "🇫🇷", SponsorTier.PLATINUM, 125_000.0, 190_000.0, 65_000.0, 5_300.0, 73, 240, 2_300_000.0),
        Sponsor("sp_stellaris",   "Stellaris Bank",   "Banca",            "Italia",          "🇮🇹", SponsorTier.PLATINUM, 140_000.0, 210_000.0, 75_000.0, 5_700.0, 78, 240, 2_700_000.0),
        Sponsor("sp_xenon",       "XenonPharma",      "Farmacia",         "Alemania",        "🇩🇪", SponsorTier.PLATINUM, 120_000.0, 180_000.0, 60_000.0, 5_000.0, 72, 240, 2_200_000.0),
        Sponsor("sp_aurora",      "Aurora Telecom",   "Telecom",          "Países Bajos",    "🇳🇱", SponsorTier.PLATINUM, 135_000.0, 195_000.0, 68_000.0, 5_400.0, 76, 240, 2_400_000.0),

        // === GOLD (6) ===
        Sponsor("sp_helios",      "Helios Drinks",    "Bebidas",          "Brasil",          "🇧🇷", SponsorTier.GOLD,      80_000.0, 120_000.0, 40_000.0, 3_500.0, 60, 180, 1_200_000.0),
        Sponsor("sp_velocidade",  "VelocidadeCar",    "Automoción",       "Brasil",          "🇧🇷", SponsorTier.GOLD,      85_000.0, 130_000.0, 45_000.0, 3_700.0, 62, 180, 1_300_000.0),
        Sponsor("sp_neptune",     "Neptune Apparel",  "Moda",             "Italia",          "🇮🇹", SponsorTier.GOLD,      75_000.0, 110_000.0, 38_000.0, 3_300.0, 58, 180, 1_100_000.0),
        Sponsor("sp_orcaair",     "Orca Airways",     "Aerolínea",        "Singapur",        "🇸🇬", SponsorTier.GOLD,      90_000.0, 140_000.0, 48_000.0, 4_000.0, 65, 180, 1_500_000.0),
        Sponsor("sp_kasai",       "Kasai Logistics",  "Logística",        "Japón",           "🇯🇵", SponsorTier.GOLD,      78_000.0, 115_000.0, 42_000.0, 3_500.0, 60, 180, 1_200_000.0),
        Sponsor("sp_finstadt",    "Finstadt Ins.",    "Seguros",          "Suecia",          "🇸🇪", SponsorTier.GOLD,      82_000.0, 125_000.0, 44_000.0, 3_700.0, 63, 180, 1_400_000.0),

        // === SILVER (5) ===
        Sponsor("sp_burgermex",   "BurgerMex",        "Restauración",     "México",          "🇲🇽", SponsorTier.SILVER,    45_000.0,  60_000.0, 22_000.0, 1_800.0, 40, 120, 500_000.0),
        Sponsor("sp_alpha",       "Alpha Sportswear", "Ropa deportiva",   "EE.UU.",          "🇺🇸", SponsorTier.SILVER,    50_000.0,  70_000.0, 25_000.0, 2_000.0, 45, 120, 600_000.0),
        Sponsor("sp_garaje",      "Garaje del Pueblo","Talleres",         "España",          "🇪🇸", SponsorTier.SILVER,    42_000.0,  55_000.0, 20_000.0, 1_700.0, 38, 120, 450_000.0),
        Sponsor("sp_bytefoods",   "ByteFoods",        "Delivery",         "EE.UU.",          "🇺🇸", SponsorTier.SILVER,    48_000.0,  65_000.0, 24_000.0, 1_900.0, 42, 120, 550_000.0),
        Sponsor("sp_cervezor",    "Cervezor",         "Cervecera",        "Países Bajos",    "🇳🇱", SponsorTier.SILVER,    47_000.0,  62_000.0, 23_000.0, 1_900.0, 41, 120, 530_000.0),

        // === BRONZE (3) ===
        Sponsor("sp_chillout",    "Chillout Soda",    "Refrescos local",  "Polonia",         "🇵🇱", SponsorTier.BRONZE,    20_000.0,  25_000.0, 10_000.0,   800.0, 20,  90, 150_000.0),
        Sponsor("sp_localbank",   "LocalBank",        "Banca local",      "Portugal",        "🇵🇹", SponsorTier.BRONZE,    22_000.0,  28_000.0, 11_000.0,   900.0, 22,  90, 170_000.0),
        Sponsor("sp_friendsbet",  "FriendsBet",       "Apuestas",         "Irlanda",         "🇮🇪", SponsorTier.BRONZE,    25_000.0,  32_000.0, 12_000.0, 1_000.0, 25,  90, 200_000.0)
    )

    fun byId(id: String): Sponsor? = all.firstOrNull { it.id == id }
}

@Serializable
data class ActiveSponsorship(
    val sponsorId: String,
    val signedOnDay: Int,
    val expiresOnDay: Int,
    val totalEarned: Double = 0.0,
    val winsDuringContract: Int = 0,
    val podiumsDuringContract: Int = 0
)

// =====================================================================
//                          PERSONAL TÉCNICO
// =====================================================================

@Serializable
enum class StaffRole(val label: String, val emoji: String) {
    RACE_ENGINEER("Ingeniero de Pista", "👨‍🔧"),
    STRATEGIST("Estratega", "🧠"),
    AERODYNAMICIST("Aerodinámico", "✈️"),
    POWER_UNIT("Jefe de Motor", "⚙️"),
    TYRE_SPECIALIST("Especialista de Neumáticos", "🛞")
}

@Serializable
data class TechStaff(
    val id: String,
    val name: String,
    val nationality: String,
    val flag: String,
    val role: StaffRole,
    val rating: Int,                     // 50..99
    val salaryPerDay: Double,
    val hiredOnDay: Int = 0
) {
    /** Bonus que aplica al equipo según rol y rating. */
    fun bonusMultiplier(): Double = 1.0 + (rating - 50) * 0.003   // +0.003 por cada punto sobre 50
}

object StaffPool {
    val all: List<TechStaff> = listOf(
        // RACE ENGINEER
        TechStaff("st_brunelli",  "Lorenzo Brunelli",  "Italia",        "🇮🇹", StaffRole.RACE_ENGINEER, 95, 22_000.0),
        TechStaff("st_walsh",     "Aiden Walsh",       "Reino Unido",   "🇬🇧", StaffRole.RACE_ENGINEER, 90, 18_000.0),
        TechStaff("st_huang",     "Mei Huang",         "China",         "🇨🇳", StaffRole.RACE_ENGINEER, 85, 14_000.0),
        TechStaff("st_pereira",   "João Pereira",      "Portugal",      "🇵🇹", StaffRole.RACE_ENGINEER, 75,  9_000.0),
        TechStaff("st_smit",      "Bram Smit",         "Países Bajos",  "🇳🇱", StaffRole.RACE_ENGINEER, 65,  6_000.0),
        // STRATEGIST
        TechStaff("st_caron",     "Émilie Caron",      "Francia",       "🇫🇷", StaffRole.STRATEGIST,    96, 24_000.0),
        TechStaff("st_weber",     "Tobias Weber",      "Alemania",      "🇩🇪", StaffRole.STRATEGIST,    88, 17_000.0),
        TechStaff("st_okafor",    "Adaeze Okafor",     "Nigeria",       "🇳🇬", StaffRole.STRATEGIST,    80, 12_000.0),
        TechStaff("st_finn",      "Eero Finn",         "Finlandia",     "🇫🇮", StaffRole.STRATEGIST,    72,  8_500.0),
        TechStaff("st_almeida",   "Sara Almeida",      "Brasil",        "🇧🇷", StaffRole.STRATEGIST,    62,  5_500.0),
        // AERODYNAMICIST
        TechStaff("st_kohl",      "Helmut Kohl",       "Alemania",      "🇩🇪", StaffRole.AERODYNAMICIST,94, 21_000.0),
        TechStaff("st_lefevre",   "Camille Lefèvre",   "Francia",       "🇫🇷", StaffRole.AERODYNAMICIST,87, 16_000.0),
        TechStaff("st_kim",       "Soo-jin Kim",       "Corea",         "🇰🇷", StaffRole.AERODYNAMICIST,78, 11_000.0),
        TechStaff("st_marin",     "Jordi Marín",       "España",        "🇪🇸", StaffRole.AERODYNAMICIST,68,  7_500.0),
        // POWER UNIT
        TechStaff("st_yamamoto",  "Tatsuya Yamamoto",  "Japón",         "🇯🇵", StaffRole.POWER_UNIT,    96, 25_000.0),
        TechStaff("st_morgan",    "Eric Morgan",       "EE.UU.",        "🇺🇸", StaffRole.POWER_UNIT,    85, 15_000.0),
        TechStaff("st_lukic",     "Stefan Lukić",      "Serbia",        "🇷🇸", StaffRole.POWER_UNIT,    73,  9_500.0),
        TechStaff("st_baranov",   "Igor Baranov",      "Rusia",         "🇷🇺", StaffRole.POWER_UNIT,    63,  6_500.0),
        // TYRE
        TechStaff("st_rossi",     "Chiara Rossi",      "Italia",        "🇮🇹", StaffRole.TYRE_SPECIALIST,90, 16_000.0),
        TechStaff("st_kowalska",  "Magda Kowalska",    "Polonia",       "🇵🇱", StaffRole.TYRE_SPECIALIST,80, 11_000.0),
        TechStaff("st_torres",    "Andrés Torres",     "Argentina",     "🇦🇷", StaffRole.TYRE_SPECIALIST,68,  7_000.0)
    )

    fun byId(id: String): TechStaff? = all.firstOrNull { it.id == id }
    fun byRole(role: StaffRole): List<TechStaff> = all.filter { it.role == role }
}

// =====================================================================
//                          RECORDS DE CIRCUITO
// =====================================================================

@Serializable
data class CircuitRecord(
    val circuitId: String,
    val lapRecordHolder: String? = null,         // driverId
    val lapRecordTimeSec: Double = 0.0,
    val lapRecordSeason: Int = 0,
    val lastWinner: String? = null,              // driverId
    val lastWinnerTeam: String? = null,          // teamId
    val winsByDriver: Map<String, Int> = emptyMap(),
    val winsByTeam: Map<String, Int> = emptyMap()
)

// =====================================================================
//                          HALL OF FAME
// =====================================================================

@Serializable
data class ChampionEntry(
    val season: Int,
    val driverChampionId: String,
    val driverChampionPoints: Int,
    val constructorChampionId: String,
    val constructorChampionPoints: Int,
    val playerOwnedTeamId: String?               // si el jugador tenía equipo, cuál
)

// =====================================================================
//                              EQUIPOS
// =====================================================================

@Serializable
data class RaceCar(
    val engine: Int,         // 1..100
    val aero: Int,           // 1..100
    val reliability: Int,    // 1..100
    val tyres: Int = 50,
    val totalUpgradeSpend: Double = 0.0
) {
    fun overall(): Int = ((engine + aero + reliability + tyres) / 4)
}

@Serializable
data class RacingTeam(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val founded: Int,
    val primaryColor: Long,         // 0xFFRRGGBB
    val secondaryColor: Long,
    val car: RaceCar,
    val driver1Id: String?,
    val driver2Id: String?,
    val budget: Double,
    val sponsorIncomePerDay: Double,
    val brandValue: Int,             // 1..100, sube con buenos resultados
    val seasonPoints: Int = 0,
    val championshipsWon: Int = 0,
    val price: Double,                // precio para comprar (si no es del jugador)
    val ownedByPlayer: Boolean = false
) {
    fun totalDailyCost(): Double {
        val d1 = driver1Id?.let { DriverPool.byId(it)?.salaryPerDay } ?: 0.0
        val d2 = driver2Id?.let { DriverPool.byId(it)?.salaryPerDay } ?: 0.0
        return d1 + d2 + 6_000.0   // mantenimiento del coche
    }
}

object TeamCatalog {
    val starter: List<RacingTeam> = listOf(
        // TOP TIER
        RacingTeam("apex",        "Apex Racing",        "Italia",       "🇮🇹", 1948,
            0xFFD32F2F, 0xFFFFEB3B,
            RaceCar(engine = 92, aero = 90, reliability = 85),
            "d_velocci", "d_castell", 12_500_000.0, 180_000.0, 95, 0, 7,
            price = 320_000_000.0),
        RacingTeam("velocity",    "Velocity GP",        "Reino Unido",  "🇬🇧", 1962,
            0xFF1565C0, 0xFFFFFFFF,
            RaceCar(engine = 90, aero = 92, reliability = 87),
            "d_haldur", "d_mclark", 10_800_000.0, 165_000.0, 92, 0, 6,
            price = 300_000_000.0),
        RacingTeam("hypersonic",  "Hypersonic Motors",  "Alemania",     "🇩🇪", 1980,
            0xFFC0C0C0, 0xFF000000,
            RaceCar(engine = 95, aero = 87, reliability = 90),
            "d_stiller", "d_blanchet", 11_200_000.0, 170_000.0, 90, 0, 5,
            price = 290_000_000.0),
        // MID TIER
        RacingTeam("crown",       "Crown Engineering",  "EE.UU.",       "🇺🇸", 2001,
            0xFFFFA000, 0xFF263238,
            RaceCar(engine = 80, aero = 82, reliability = 80),
            "d_oliveira", "d_lindqvist", 5_400_000.0, 95_000.0, 70, 0, 1,
            price = 95_000_000.0),
        RacingTeam("vanguard",    "Vanguard Racing",    "Francia",      "🇫🇷", 1985,
            0xFF1B5E20, 0xFFFFFFFF,
            RaceCar(engine = 78, aero = 84, reliability = 78),
            "d_okabe", "d_kowalski", 4_900_000.0, 88_000.0, 68, 0, 1,
            price = 85_000_000.0),
        RacingTeam("drift",       "Drift Dynamics",     "Japón",        "🇯🇵", 1995,
            0xFF6A1B9A, 0xFFE91E63,
            RaceCar(engine = 82, aero = 78, reliability = 76),
            "d_chen", "d_park", 4_500_000.0, 82_000.0, 65, 0, 0,
            price = 75_000_000.0),
        // BOTTOM TIER (asequibles)
        RacingTeam("rookiespeed", "RookieSpeed",        "España",       "🇪🇸", 2015,
            0xFFFF5722, 0xFFFFFFFF,
            RaceCar(engine = 65, aero = 60, reliability = 70),
            "d_rodríguez", "d_silva", 1_800_000.0, 35_000.0, 40, 0, 0,
            price = 18_000_000.0),
        RacingTeam("garageforce", "Garage Force",       "Polonia",      "🇵🇱", 2020,
            0xFF4CAF50, 0xFF263238,
            RaceCar(engine = 58, aero = 55, reliability = 68),
            "d_petrov", "d_anders", 1_200_000.0, 28_000.0, 32, 0, 0,
            price = 12_000_000.0),
        RacingTeam("phoenix",     "Phoenix Garage",     "Países Bajos", "🇳🇱", 2022,
            0xFFE91E63, 0xFFFFEB3B,
            RaceCar(engine = 55, aero = 52, reliability = 65),
            "d_martens", "d_doyle", 900_000.0, 22_000.0, 28, 0, 0,
            price = 9_500_000.0),
        RacingTeam("starline",    "Starline Motorsport","Australia",    "🇦🇺", 2018,
            0xFF03A9F4, 0xFFFFEB3B,
            RaceCar(engine = 60, aero = 58, reliability = 66),
            "d_walden", "d_papadakis", 1_400_000.0, 30_000.0, 35, 0, 0,
            price = 14_000_000.0)
    )
}

// =====================================================================
//                          CALENDARIO + RESULTADOS
// =====================================================================

@Serializable
data class CalendarRace(
    val raceIndex: Int,
    val circuitId: String,
    val raceDay: Int                 // día absoluto in-game en que se corre
)

@Serializable
data class RaceResult(
    val raceIndex: Int,
    val circuitId: String,
    val day: Int,
    /** Lista ordenada (1º al 20º) de pares (driverId, teamId). */
    val finishOrder: List<Pair<String, String>>,
    /** Mejor vuelta (driverId). */
    val fastestLapDriver: String?,
    /** Pole position (driverId). */
    val poleDriver: String?,
    /** Driver del jugador y posición final (1=ganador, 0=DNF). */
    val playerDriverFinish: Map<String, Int> = emptyMap()
)

// =====================================================================
//                          ESTADO GLOBAL
// =====================================================================

@Serializable
data class RacingState(
    val unlocked: Boolean = false,
    val ownedTeamId: String? = null,
    val teams: List<RacingTeam> = emptyList(),
    val drivers: List<RaceDriver> = emptyList(),
    val calendar: List<CalendarRace> = emptyList(),
    val nextRaceIndex: Int = 0,
    val currentSeason: Int = 1,
    val racesThisSeason: Int = 0,
    val resultsHistory: List<RaceResult> = emptyList(),
    val lastSimulatedDay: Int = 0,
    // === Nuevos sistemas ===
    val activeSponsorships: List<ActiveSponsorship> = emptyList(),
    val hiredStaff: List<String> = emptyList(),       // staff IDs contratados
    val circuitRecords: List<CircuitRecord> = emptyList(),
    val hallOfFame: List<ChampionEntry> = emptyList(),
    val totalSponsorEarnings: Double = 0.0
) {
    fun ownedTeam(): RacingTeam? = teams.find { it.id == ownedTeamId }
    fun nextRace(): CalendarRace? = calendar.getOrNull(nextRaceIndex)

    fun driverStandings(top: Int = 10): List<RaceDriver> =
        drivers.sortedByDescending { it.seasonPoints }.take(top)

    fun constructorStandings(top: Int = 10): List<RacingTeam> =
        teams.sortedByDescending { it.seasonPoints }.take(top)

    /** Recompensas totales por sponsorships activos (suma daily). */
    fun totalSponsorDailyIncome(): Double {
        return activeSponsorships.mapNotNull { SponsorCatalog.byId(it.sponsorId)?.baseDailyPay }.sum()
    }

    /** Coste diario total de personal técnico contratado. */
    fun totalStaffDailyCost(): Double {
        return hiredStaff.mapNotNull { StaffPool.byId(it)?.salaryPerDay }.sum()
    }

    /** Hired staff por rol (sólo el de mayor rating si hay varios del mismo rol). */
    fun bestStaffOf(role: StaffRole): TechStaff? {
        return hiredStaff.mapNotNull { StaffPool.byId(it) }
            .filter { it.role == role }
            .maxByOrNull { it.rating }
    }

    fun recordOf(circuitId: String): CircuitRecord? =
        circuitRecords.find { it.circuitId == circuitId }

    /** Tabla de medallas por nacionalidad (suma careerWins de pilotos). */
    fun nationsMedalTable(): List<Triple<String, String, Int>> {
        // (flag, country, totalWins)
        val byCountry = drivers.groupBy { it.nationality to it.flag }
            .mapValues { (_, list) -> list.sumOf { it.careerWins } }
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
        return byCountry.map { (countryAndFlag, wins) ->
            Triple(countryAndFlag.second, countryAndFlag.first, wins)
        }
    }

    /** Top 10 pilotos all-time por victorias acumuladas. */
    fun allTimeWinsLeaders(): List<RaceDriver> =
        drivers.filter { it.careerWins > 0 }.sortedByDescending { it.careerWins }.take(10)

    /** Top 10 equipos por championships ganados. */
    fun teamHallOfFame(): List<RacingTeam> =
        teams.sortedByDescending { it.championshipsWon }.take(10)
}

object RacingConstants {
    /** Puntos F1 estándar para el top 10. */
    val POINTS_PER_POSITION: List<Int> = listOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)
    const val FASTEST_LAP_BONUS = 1
    const val POLE_POSITION_BONUS = 1   // pequeño bonus
    const val RACES_PER_SEASON = 16
    const val DAYS_BETWEEN_RACES = 7    // 1 carrera por semana in-game
}
