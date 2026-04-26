package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Categoría temática de la noticia, usada para filtros y colorimetría. */
enum class NewsCategory(val emoji: String, val displayName: String) {
    MARKET("📊", "Mercado"),
    POLITICS("🏛️", "Política"),
    TECH("💡", "Tecnología"),
    WEATHER("🌦️", "Clima"),
    CELEBRITY("⭐", "Famosos"),
    SCANDAL("🔥", "Escándalo"),
    INTERNATIONAL("🌍", "Internacional"),
    LOCAL("🏘️", "Local")
}

/** Severidad de la noticia: afecta el impacto en precios y la prominencia. */
enum class NewsSeverity(val displayName: String, val multiplier: Double) {
    TRIVIAL("Trivial", 0.4),
    MINOR("Menor", 0.8),
    MAJOR("Importante", 1.3),
    BREAKING("Última hora", 1.9)
}

/**
 * Noticia generada en tiempo de juego. Cada noticia puede aplicar un impacto
 * acotado a un conjunto de recursos y tiene una duración (en días in-game).
 */
@Serializable
data class NewsItem(
    val id: Long,
    val headline: String,
    val body: String,
    val category: NewsCategory,
    val emoji: String,
    /** Tick de juego en el que se generó la noticia. */
    val timestamp: Long,
    val severity: NewsSeverity,
    val affectedResources: List<String>,
    /** Impacto multiplicativo aplicado al factor del recurso (ej. +0.20 = +20%). */
    val priceImpact: Double,
    val durationDays: Int,
    /** Marcado true cuando ya se ha aplicado al mercado (para no repetir). */
    val applied: Boolean = false,
    /** Día in-game en el que la noticia expira y deja de surtir efecto. */
    val expiresAtDay: Int = 0
)

/**
 * Estado serializable del feed de noticias. Mantiene un histórico acotado
 * y sólo las activas (no caducadas) se aplican al mercado.
 */
@Serializable
data class NewsFeed(
    val items: List<NewsItem> = emptyList(),
    val unreadCount: Int = 0
) {
    fun active(currentDay: Int): List<NewsItem> =
        items.filter { it.expiresAtDay >= currentDay }

    fun markAllRead(): NewsFeed = copy(unreadCount = 0)
}

/**
 * Plantilla generadora de noticias. Las plantillas se sortean según la fase
 * económica y la categoría: por ejemplo en CRASH/RECESSION se priorizan las
 * MARKET y POLITICS negativas; en BOOM las TECH y CELEBRITY positivas.
 */
data class NewsTemplate(
    val id: String,
    val headlinePattern: String,
    val bodyPattern: String,
    val category: NewsCategory,
    val emoji: String,
    /** Pesos de severidad: TRIVIAL, MINOR, MAJOR, BREAKING. */
    val severityWeights: List<Double> = listOf(0.20, 0.45, 0.30, 0.05),
    val affectedResources: List<String> = emptyList(),
    /** Rango de impacto sobre el factor de precio (puede ser negativo). */
    val priceImpactRange: ClosedFloatingPointRange<Double> = -0.05..0.05,
    val durationRangeDays: IntRange = 2..6,
    /** Fases en las que esta plantilla es más probable. Vacío = todas. */
    val favoredPhases: Set<EconomicPhase> = emptySet()
) {
    fun rollSeverity(rng: Random): NewsSeverity {
        val total = severityWeights.sum()
        var roll = rng.nextDouble() * total
        val list = NewsSeverity.values()
        for (i in list.indices) {
            roll -= severityWeights.getOrElse(i) { 0.0 }
            if (roll <= 0.0) return list[i]
        }
        return NewsSeverity.MINOR
    }

    fun rollImpact(severity: NewsSeverity, rng: Random): Double {
        val base = rng.nextDouble(priceImpactRange.start, priceImpactRange.endInclusive)
        return base * severity.multiplier
    }
}

/**
 * Catálogo estático de plantillas de noticias en español. Más de 40 plantillas
 * cubriendo todas las categorías y la mayor parte del catálogo de recursos.
 */
object NewsTemplates {

    val templates: List<NewsTemplate> = listOf(
        // ----- WEATHER / AGRICULTURA / FOOD -----
        NewsTemplate(
            id = "drought_crops",
            headlinePattern = "Sequía severa afecta cosechas",
            bodyPattern = "Una ola de calor sin precedentes ha reducido drásticamente la producción de cereales y lácteos.",
            category = NewsCategory.WEATHER, emoji = "☀️",
            affectedResources = listOf("wheat", "milk", "seed"),
            priceImpactRange = 0.10..0.22,
            durationRangeDays = 4..8,
            severityWeights = listOf(0.05, 0.25, 0.55, 0.15)
        ),
        NewsTemplate(
            id = "flood_logistics",
            headlinePattern = "Inundaciones colapsan logística agrícola",
            bodyPattern = "Riadas en zonas productoras paralizan el transporte de grano y agua embotellada.",
            category = NewsCategory.WEATHER, emoji = "🌊",
            affectedResources = listOf("wheat", "flour", "water"),
            priceImpactRange = 0.06..0.16,
            durationRangeDays = 2..5
        ),
        NewsTemplate(
            id = "frost_dairy",
            headlinePattern = "Heladas castigan al sector lácteo",
            bodyPattern = "Las temperaturas bajo cero reducen la producción láctea en grandes cooperativas.",
            category = NewsCategory.WEATHER, emoji = "❄️",
            affectedResources = listOf("milk", "cheese"),
            priceImpactRange = 0.08..0.18
        ),
        NewsTemplate(
            id = "good_harvest",
            headlinePattern = "Cosecha excelente: trigo y harina caen",
            bodyPattern = "Las estimaciones triplican las previsiones y el grano abarata la cesta básica.",
            category = NewsCategory.WEATHER, emoji = "🌾",
            affectedResources = listOf("wheat", "flour", "bread"),
            priceImpactRange = (-0.16)..(-0.06),
            favoredPhases = setOf(EconomicPhase.NORMAL, EconomicPhase.RECOVERY, EconomicPhase.BOOM)
        ),

        // ----- ENERGÍA Y MATERIAS PRIMAS -----
        NewsTemplate(
            id = "oil_discovery",
            headlinePattern = "Descubren un nuevo yacimiento de petróleo",
            bodyPattern = "Un megayacimiento offshore promete reducir la dependencia y los precios.",
            category = NewsCategory.INTERNATIONAL, emoji = "🛢️",
            affectedResources = listOf("oil", "plastic"),
            priceImpactRange = (-0.18)..(-0.08),
            durationRangeDays = 5..10
        ),
        NewsTemplate(
            id = "oil_embargo",
            headlinePattern = "Embargo internacional al crudo",
            bodyPattern = "Tensiones geopolíticas dispararán los precios del petróleo en los próximos días.",
            category = NewsCategory.INTERNATIONAL, emoji = "⚠️",
            affectedResources = listOf("oil", "plastic"),
            priceImpactRange = 0.12..0.28,
            severityWeights = listOf(0.05, 0.20, 0.50, 0.25),
            favoredPhases = setOf(EconomicPhase.RECESSION, EconomicPhase.CRASH, EconomicPhase.DEPRESSION)
        ),
        NewsTemplate(
            id = "miners_strike",
            headlinePattern = "Huelga general de mineros",
            bodyPattern = "Las minas paralizadas elevan los precios del hierro y el carbón.",
            category = NewsCategory.POLITICS, emoji = "⛏️",
            affectedResources = listOf("iron_ore", "coal"),
            priceImpactRange = 0.15..0.30,
            severityWeights = listOf(0.05, 0.25, 0.50, 0.20)
        ),
        NewsTemplate(
            id = "copper_crisis",
            headlinePattern = "Crisis del cobre golpea a la electrónica",
            bodyPattern = "Escasez de cobre encarece circuitos y baterías en cadena.",
            category = NewsCategory.MARKET, emoji = "🔌",
            affectedResources = listOf("circuit", "battery"),
            priceImpactRange = 0.08..0.20
        ),
        NewsTemplate(
            id = "renewable_boom",
            headlinePattern = "Boom de energías renovables",
            bodyPattern = "Las renovables abaratan el coste energético y benefician a fabricantes.",
            category = NewsCategory.TECH, emoji = "🔋",
            affectedResources = listOf("battery", "silicon"),
            priceImpactRange = (-0.12)..(-0.04),
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.RECOVERY, EconomicPhase.NORMAL)
        ),
        NewsTemplate(
            id = "coal_phaseout",
            headlinePattern = "Países anuncian fin del carbón",
            bodyPattern = "Plan de transición acelerada deja al carbón en mínimos históricos.",
            category = NewsCategory.POLITICS, emoji = "🏛️",
            affectedResources = listOf("coal"),
            priceImpactRange = (-0.20)..(-0.08)
        ),
        NewsTemplate(
            id = "wood_shortage",
            headlinePattern = "Escasez de madera por incendios",
            bodyPattern = "Los grandes incendios forestales encarecen troncos y tablones.",
            category = NewsCategory.WEATHER, emoji = "🔥",
            affectedResources = listOf("wood_log", "plank"),
            priceImpactRange = 0.10..0.22
        ),

        // ----- TECNOLOGÍA -----
        NewsTemplate(
            id = "smartphone_boom",
            headlinePattern = "Boom de smartphones",
            bodyPattern = "El nuevo modelo viral dispara la demanda de teléfonos en todo el mundo.",
            category = NewsCategory.TECH, emoji = "📱",
            affectedResources = listOf("smartphone", "circuit"),
            priceImpactRange = 0.06..0.16,
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.BUBBLE, EconomicPhase.NORMAL)
        ),
        NewsTemplate(
            id = "quantum_chip",
            headlinePattern = "Avance histórico en chips cuánticos",
            bodyPattern = "Un nuevo proceso reduce drásticamente el coste de los semiconductores.",
            category = NewsCategory.TECH, emoji = "🧠",
            affectedResources = listOf("silicon", "circuit"),
            priceImpactRange = (-0.16)..(-0.06)
        ),
        NewsTemplate(
            id = "ai_consultancy",
            headlinePattern = "IA reemplaza consultoras tradicionales",
            bodyPattern = "Servicios profesionales se abaratan por la automatización con IA.",
            category = NewsCategory.TECH, emoji = "🤖",
            affectedResources = listOf("consulting", "software"),
            priceImpactRange = (-0.12)..(-0.03)
        ),
        NewsTemplate(
            id = "apple_pay_competitor",
            headlinePattern = "Apple Pay enfrenta a su mayor rival",
            bodyPattern = "Volatilidad en software y servicios financieros tras el anuncio.",
            category = NewsCategory.TECH, emoji = "💳",
            affectedResources = listOf("software"),
            priceImpactRange = (-0.06)..0.06,
            severityWeights = listOf(0.05, 0.30, 0.45, 0.20)
        ),
        NewsTemplate(
            id = "ev_megafactory",
            headlinePattern = "Inauguran megafábrica de baterías",
            bodyPattern = "El precio de las baterías y motores eléctricos se desploma.",
            category = NewsCategory.TECH, emoji = "🔋",
            affectedResources = listOf("battery", "engine"),
            priceImpactRange = (-0.18)..(-0.07)
        ),
        NewsTemplate(
            id = "cybersecurity_breach",
            headlinePattern = "Brecha de ciberseguridad masiva",
            bodyPattern = "Un ataque global a la nube infla la demanda de software seguro.",
            category = NewsCategory.TECH, emoji = "🛡️",
            affectedResources = listOf("software"),
            priceImpactRange = 0.08..0.18,
            severityWeights = listOf(0.05, 0.25, 0.50, 0.20)
        ),

        // ----- POLÍTICA Y ECONOMÍA INTERNACIONAL -----
        NewsTemplate(
            id = "tariffs_imports",
            headlinePattern = "Nuevas tarifas a las importaciones",
            bodyPattern = "El gobierno impone aranceles a productos extranjeros: suben los precios industriales.",
            category = NewsCategory.POLITICS, emoji = "🏛️",
            affectedResources = listOf("steel", "iron_ingot", "plastic"),
            priceImpactRange = 0.08..0.18
        ),
        NewsTemplate(
            id = "brexit_steel",
            headlinePattern = "Brexit afecta al acero europeo",
            bodyPattern = "Nuevas trabas aduaneras encarecen el acero en el continente.",
            category = NewsCategory.INTERNATIONAL, emoji = "🇪🇺",
            affectedResources = listOf("steel", "iron_ingot"),
            priceImpactRange = 0.06..0.14
        ),
        NewsTemplate(
            id = "argentina_hyperinflation",
            headlinePattern = "Hiperinflación argentina sacude el sur",
            bodyPattern = "El descontrol de precios contagia a los mercados regionales de bienes de consumo.",
            category = NewsCategory.INTERNATIONAL, emoji = "🌎",
            affectedResources = listOf("wheat", "bread", "cheese", "smartphone"),
            priceImpactRange = 0.05..0.14
        ),
        NewsTemplate(
            id = "central_bank_cut",
            headlinePattern = "Banco central recorta tipos",
            bodyPattern = "El crédito se abarata: empuje a la actividad y los bienes duraderos.",
            category = NewsCategory.MARKET, emoji = "💵",
            affectedResources = listOf("furniture", "car", "bicycle"),
            priceImpactRange = 0.04..0.10,
            favoredPhases = setOf(EconomicPhase.RECESSION, EconomicPhase.RECOVERY)
        ),
        NewsTemplate(
            id = "central_bank_hike",
            headlinePattern = "Subidón de tipos: caen ventas duraderas",
            bodyPattern = "El alza del precio del dinero enfría el consumo de coches, muebles y bicis.",
            category = NewsCategory.MARKET, emoji = "📉",
            affectedResources = listOf("car", "furniture", "bicycle"),
            priceImpactRange = (-0.14)..(-0.05),
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.BUBBLE)
        ),
        NewsTemplate(
            id = "trade_deal",
            headlinePattern = "Histórico acuerdo comercial",
            bodyPattern = "Caen aranceles a manufacturas: vehículos y muebles se abaratan.",
            category = NewsCategory.POLITICS, emoji = "🤝",
            affectedResources = listOf("car", "furniture"),
            priceImpactRange = (-0.10)..(-0.03)
        ),

        // ----- LUJO / CELEBRITY -----
        NewsTemplate(
            id = "celebrity_yacht",
            headlinePattern = "Famoso compra yate y se vuelve viral",
            bodyPattern = "Una celebrity exhibe su nuevo yate. La demanda de superyates se dispara.",
            category = NewsCategory.CELEBRITY, emoji = "⭐",
            affectedResources = listOf("yacht"),
            priceImpactRange = 0.20..0.40,
            severityWeights = listOf(0.05, 0.20, 0.50, 0.25),
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.BUBBLE)
        ),
        NewsTemplate(
            id = "jewelry_restriction",
            headlinePattern = "Nueva restricción a la joyería",
            bodyPattern = "Una directiva limita la importación de oro: las joyas se pegan un buen palo.",
            category = NewsCategory.POLITICS, emoji = "💎",
            affectedResources = listOf("jewelry"),
            priceImpactRange = (-0.25)..(-0.10)
        ),
        NewsTemplate(
            id = "celeb_chef",
            headlinePattern = "Chef famoso convierte el queso en moda",
            bodyPattern = "Su receta viral dispara la demanda de queso artesanal.",
            category = NewsCategory.CELEBRITY, emoji = "🧀",
            affectedResources = listOf("cheese", "milk"),
            priceImpactRange = 0.05..0.14
        ),
        NewsTemplate(
            id = "rapper_jewelry",
            headlinePattern = "Rapero exhibe colección millonaria",
            bodyPattern = "El video viral dispara la demanda de joyas de gama alta.",
            category = NewsCategory.CELEBRITY, emoji = "💍",
            affectedResources = listOf("jewelry"),
            priceImpactRange = 0.10..0.22,
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.BUBBLE)
        ),

        // ----- ESCÁNDALOS -----
        NewsTemplate(
            id = "scandal_factory",
            headlinePattern = "Escándalo en megafábrica",
            bodyPattern = "Una filtración revela prácticas dudosas. Caen acciones y sube la cautela.",
            category = NewsCategory.SCANDAL, emoji = "🚨",
            affectedResources = listOf("smartphone", "car"),
            priceImpactRange = (-0.10)..(-0.03),
            favoredPhases = setOf(EconomicPhase.RECESSION, EconomicPhase.CRASH)
        ),
        NewsTemplate(
            id = "scandal_food",
            headlinePattern = "Retirada masiva de productos lácteos",
            bodyPattern = "Tras una alerta sanitaria, lácteos contaminados son retirados del mercado.",
            category = NewsCategory.SCANDAL, emoji = "🥛",
            affectedResources = listOf("milk", "cheese"),
            priceImpactRange = (-0.14)..(-0.05)
        ),
        NewsTemplate(
            id = "scandal_software",
            headlinePattern = "Backdoor descubierto en software popular",
            bodyPattern = "El parche urgente dispara la demanda de auditorías y software alternativo.",
            category = NewsCategory.SCANDAL, emoji = "🐛",
            affectedResources = listOf("software", "consulting"),
            priceImpactRange = 0.06..0.14
        ),

        // ----- LOCAL -----
        NewsTemplate(
            id = "local_market_fair",
            headlinePattern = "Feria gastronómica abarrota la ciudad",
            bodyPattern = "Demanda local de pan y queso al alza durante el evento.",
            category = NewsCategory.LOCAL, emoji = "🥖",
            affectedResources = listOf("bread", "cheese"),
            priceImpactRange = 0.04..0.10
        ),
        NewsTemplate(
            id = "local_construction",
            headlinePattern = "Nuevo plan urbanístico en marcha",
            bodyPattern = "Demanda creciente de tablones y acero para obras públicas.",
            category = NewsCategory.LOCAL, emoji = "🏗️",
            affectedResources = listOf("plank", "steel", "iron_ingot"),
            priceImpactRange = 0.05..0.12
        ),
        NewsTemplate(
            id = "local_bike_lane",
            headlinePattern = "Carriles bici disparan ventas",
            bodyPattern = "El nuevo carril bici impulsa la demanda en tiendas locales.",
            category = NewsCategory.LOCAL, emoji = "🚲",
            affectedResources = listOf("bicycle"),
            priceImpactRange = 0.06..0.14
        ),
        NewsTemplate(
            id = "hipster_vegan",
            headlinePattern = "Hipsters vegetarianos disparan el trigo",
            bodyPattern = "La nueva tendencia 'plant-based' refuerza la demanda de cereales y harina.",
            category = NewsCategory.LOCAL, emoji = "🥬",
            affectedResources = listOf("wheat", "flour"),
            priceImpactRange = 0.04..0.09
        ),

        // ----- MERCADO / FINANZAS -----
        NewsTemplate(
            id = "stock_rally",
            headlinePattern = "Bolsa marca máximos históricos",
            bodyPattern = "Optimismo generalizado contagia al consumo de bienes de lujo.",
            category = NewsCategory.MARKET, emoji = "📈",
            affectedResources = listOf("jewelry", "yacht", "car"),
            priceImpactRange = 0.04..0.12,
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.BUBBLE)
        ),
        NewsTemplate(
            id = "stock_crash",
            headlinePattern = "Pánico vendedor en los mercados",
            bodyPattern = "Caída global en bolsa: caen lujo y bienes duraderos.",
            category = NewsCategory.MARKET, emoji = "📉",
            affectedResources = listOf("jewelry", "yacht", "car", "smartphone"),
            priceImpactRange = (-0.18)..(-0.06),
            severityWeights = listOf(0.02, 0.18, 0.50, 0.30),
            favoredPhases = setOf(EconomicPhase.CRASH, EconomicPhase.RECESSION)
        ),
        NewsTemplate(
            id = "wage_strike",
            headlinePattern = "Huelga salarial paraliza fábricas",
            bodyPattern = "La producción industrial cae y el componente eléctrico se encarece.",
            category = NewsCategory.POLITICS, emoji = "✊",
            affectedResources = listOf("gear", "engine"),
            priceImpactRange = 0.07..0.16
        ),
        NewsTemplate(
            id = "market_consolidation",
            headlinePattern = "Fusión de gigantes industriales",
            bodyPattern = "La consolidación encarece componentes claves del automóvil.",
            category = NewsCategory.MARKET, emoji = "🏭",
            affectedResources = listOf("engine", "gear", "car"),
            priceImpactRange = 0.05..0.13
        ),
        NewsTemplate(
            id = "consulting_boom",
            headlinePattern = "Demanda récord de consultoras",
            bodyPattern = "Las grandes empresas se reorganizan y la consultoría vive un boom.",
            category = NewsCategory.MARKET, emoji = "🧑‍💼",
            affectedResources = listOf("consulting"),
            priceImpactRange = 0.06..0.15,
            favoredPhases = setOf(EconomicPhase.BOOM, EconomicPhase.RECOVERY)
        ),

        // ----- INTERNACIONAL VARIADAS -----
        NewsTemplate(
            id = "shipping_chaos",
            headlinePattern = "Crisis de contenedores en puertos",
            bodyPattern = "El bloqueo logístico encarece todo lo importado.",
            category = NewsCategory.INTERNATIONAL, emoji = "🚢",
            affectedResources = listOf("smartphone", "car", "circuit", "plastic"),
            priceImpactRange = 0.05..0.12,
            severityWeights = listOf(0.05, 0.25, 0.50, 0.20)
        ),
        NewsTemplate(
            id = "global_glass_glut",
            headlinePattern = "Sobreproducción mundial de vidrio",
            bodyPattern = "Stocks récord empujan a la baja el precio del vidrio industrial.",
            category = NewsCategory.INTERNATIONAL, emoji = "🪟",
            affectedResources = listOf("glass"),
            priceImpactRange = (-0.12)..(-0.04)
        ),
        NewsTemplate(
            id = "plastic_ban",
            headlinePattern = "Veto al plástico de un solo uso",
            bodyPattern = "La directiva acelera la sustitución del plástico convencional.",
            category = NewsCategory.POLITICS, emoji = "🧴",
            affectedResources = listOf("plastic"),
            priceImpactRange = (-0.10)..0.10,
            severityWeights = listOf(0.05, 0.30, 0.45, 0.20)
        ),

        // ----- VARIOS -----
        NewsTemplate(
            id = "viral_diy",
            headlinePattern = "Boom DIY: la gente fabrica sus muebles",
            bodyPattern = "Ola viral en redes dispara la demanda de tablones y herramientas.",
            category = NewsCategory.LOCAL, emoji = "🪚",
            affectedResources = listOf("plank", "wood_log"),
            priceImpactRange = 0.05..0.13
        ),
        NewsTemplate(
            id = "agri_subsidies",
            headlinePattern = "Nuevas subvenciones al agro",
            bodyPattern = "El campo recibe ayudas: caen los precios de cereales y leche.",
            category = NewsCategory.POLITICS, emoji = "🚜",
            affectedResources = listOf("wheat", "milk", "seed"),
            priceImpactRange = (-0.12)..(-0.04)
        ),
        NewsTemplate(
            id = "spring_festival",
            headlinePattern = "Festival de primavera satura la hostelería",
            bodyPattern = "El consumo de pan y queso se dispara durante las festividades.",
            category = NewsCategory.LOCAL, emoji = "🎉",
            affectedResources = listOf("bread", "cheese"),
            priceImpactRange = 0.05..0.10
        ),
        NewsTemplate(
            id = "global_water_crisis",
            headlinePattern = "Crisis hídrica global",
            bodyPattern = "Reservas mínimas de agua disparan precios en regiones secas.",
            category = NewsCategory.INTERNATIONAL, emoji = "💧",
            affectedResources = listOf("water", "wheat"),
            priceImpactRange = 0.10..0.20
        ),
        NewsTemplate(
            id = "luxury_tax",
            headlinePattern = "Nuevo impuesto al lujo",
            bodyPattern = "El gravamen golpea a yates y joyas: las ventas se desaceleran.",
            category = NewsCategory.POLITICS, emoji = "🏷️",
            affectedResources = listOf("yacht", "jewelry"),
            priceImpactRange = (-0.18)..(-0.06),
            favoredPhases = setOf(EconomicPhase.RECESSION, EconomicPhase.CRASH, EconomicPhase.DEPRESSION)
        )
    )

    fun byCategory(c: NewsCategory): List<NewsTemplate> =
        templates.filter { it.category == c }

    fun pickWeighted(rng: Random, phase: EconomicPhase): NewsTemplate {
        // las plantillas con favoredPhases sin coincidir reducen su peso
        val weighted = templates.map { t ->
            val w = if (t.favoredPhases.isEmpty() || phase in t.favoredPhases) 1.0 else 0.35
            t to w
        }
        val total = weighted.sumOf { it.second }
        var roll = rng.nextDouble() * total
        for ((t, w) in weighted) {
            roll -= w
            if (roll <= 0.0) return t
        }
        return weighted.last().first
    }
}
