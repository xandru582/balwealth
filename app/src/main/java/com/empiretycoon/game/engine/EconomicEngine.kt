package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor macro-económico: gestiona ciclos económicos, generación de noticias y
 * sus efectos sobre el mercado. Es puro: recibe estados y devuelve estados.
 *
 * Las llamadas se invocan una vez por día in-game salvo [generateNewsTick]
 * que se evalúa con probabilidad varias veces al día.
 */
object EconomicEngine {

    // ---------- CICLO ECONÓMICO ----------

    /**
     * Avanza un día económico: incrementa contadores, aplica deriva del PIB e
     * inflación según la fase actual y, con probabilidad
     * [EconomicTransitions.DAILY_TRANSITION_PROB], decide saltar de fase.
     *
     * Hay un mínimo de permanencia de 2 días en cada fase para evitar
     * oscilaciones bruscas. Tras un CRASH se fuerza al menos 6 días sin otro.
     */
    fun tickPhase(econ: EconomicState, rng: Random): EconomicState {
        val phase = econ.currentPhase
        val newDays = econ.daysInPhase + 1

        // Deriva del PIB: positivo en fases alcistas, negativo en bajistas
        val gdpDelta = when (phase) {
            EconomicPhase.BOOM -> 0.6
            EconomicPhase.RECOVERY -> 0.3
            EconomicPhase.NORMAL -> 0.05
            EconomicPhase.RECESSION -> -0.4
            EconomicPhase.DEPRESSION -> -0.7
            EconomicPhase.BUBBLE -> 0.9
            EconomicPhase.CRASH -> -1.4
        }
        val newGdp = (econ.gdpIndex + gdpDelta).coerceIn(40.0, 260.0)

        // Inflación: las fases alcistas tienden a inflar; recesivas, deflación leve
        val infDelta = when (phase) {
            EconomicPhase.BOOM -> 0.0008
            EconomicPhase.RECOVERY -> 0.0003
            EconomicPhase.NORMAL -> 0.00005
            EconomicPhase.RECESSION -> -0.0004
            EconomicPhase.DEPRESSION -> -0.0007
            EconomicPhase.BUBBLE -> 0.0014
            EconomicPhase.CRASH -> -0.0009
        }
        val newInflation = (econ.inflation + infDelta).coerceIn(-0.05, 0.18)

        // Trend strength: corre hacia el "carácter" de la fase
        val targetTrend = when (phase) {
            EconomicPhase.BOOM -> 0.7
            EconomicPhase.RECOVERY -> 0.35
            EconomicPhase.NORMAL -> 0.0
            EconomicPhase.RECESSION -> -0.55
            EconomicPhase.DEPRESSION -> -0.85
            EconomicPhase.BUBBLE -> 0.95
            EconomicPhase.CRASH -> -0.95
        }
        val newTrend = econ.trendStrength + (targetTrend - econ.trendStrength) * 0.25

        // ¿Intentamos transición?
        val canCrash = econ.daysSinceLastCrash >= 6
        val minStayInPhase = if (phase == EconomicPhase.CRASH) 2 else 2
        val tryTransition = newDays >= minStayInPhase &&
            rng.nextDouble() < EconomicTransitions.DAILY_TRANSITION_PROB

        val nextPhase = if (tryTransition) {
            var sample = EconomicTransitions.sampleNext(phase, rng)
            // bloqueo de re-crash
            if (sample == EconomicPhase.CRASH && !canCrash) {
                sample = EconomicPhase.RECESSION
            }
            sample
        } else phase

        val phaseChanged = nextPhase != phase
        val newDaysInPhase = if (phaseChanged) 0 else newDays
        val newDaysSinceCrash = when {
            nextPhase == EconomicPhase.CRASH -> 0
            else -> econ.daysSinceLastCrash + 1
        }
        val newRecent = if (phaseChanged) {
            (econ.recentPhases + nextPhase).takeLast(8)
        } else econ.recentPhases

        return econ.copy(
            currentPhase = nextPhase,
            daysInPhase = newDaysInPhase,
            daysSinceLastCrash = newDaysSinceCrash,
            gdpIndex = newGdp,
            inflation = newInflation,
            trendStrength = newTrend.coerceIn(-1.0, 1.0),
            recentPhases = newRecent
        )
    }

    /**
     * Aplica la fase económica al mercado: corrige los factores hacia el
     * multiplicador global de demanda y aumenta la volatilidad efectiva.
     * Se llama tras [tickPhase] al final del día.
     */
    fun applyPhaseToMarket(market: Market, phase: EconomicPhase, rng: Random): Market {
        val target = phase.globalDemandMultiplier
        val pull = 0.05  // tira un 5% hacia el target cada día
        val noiseAmp = 0.02 * phase.volatilityMultiplier

        val newFactors = HashMap<String, Double>(market.priceFactors.size)
        for (res in ResourceCatalog.all) {
            val f = market.priceFactors[res.id] ?: 1.0
            val phaseShift = (target - f) * pull
            val noise = (rng.nextDouble() - 0.5) * 2 * noiseAmp
            val nf = clampFactor(f + phaseShift + noise)
            newFactors[res.id] = nf
        }
        return market.copy(priceFactors = newFactors)
    }

    // ---------- NOTICIAS ----------

    /**
     * Probabilidad por tick de generar noticia. Se llama frecuentemente; el
     * ratio efectivo equivale a aprox. 1-3 noticias por día in-game según fase.
     */
    private fun newsProbabilityFor(phase: EconomicPhase): Double = when (phase) {
        EconomicPhase.BOOM -> 0.0018
        EconomicPhase.RECOVERY -> 0.0014
        EconomicPhase.NORMAL -> 0.0012
        EconomicPhase.RECESSION -> 0.0020
        EconomicPhase.DEPRESSION -> 0.0024
        EconomicPhase.BUBBLE -> 0.0026
        EconomicPhase.CRASH -> 0.0036
    }

    /**
     * Decide si se genera una noticia este tick y, si sí, devuelve la
     * [NewsItem]. Devuelve null si no se genera nada.
     */
    fun generateNewsTick(
        econ: EconomicState,
        rng: Random,
        currentTick: Long,
        currentDay: Int
    ): NewsItem? {
        if (rng.nextDouble() >= newsProbabilityFor(econ.currentPhase)) return null
        val template = NewsTemplates.pickWeighted(rng, econ.currentPhase)
        val severity = template.rollSeverity(rng)
        val impact = template.rollImpact(severity, rng)
        val days = rng.nextInt(template.durationRangeDays.first,
            template.durationRangeDays.last + 1)
        return NewsItem(
            id = currentTick * 1_000L + rng.nextInt(1_000),
            headline = template.headlinePattern,
            body = template.bodyPattern,
            category = template.category,
            emoji = template.emoji,
            timestamp = currentTick,
            severity = severity,
            affectedResources = template.affectedResources,
            priceImpact = impact,
            durationDays = days,
            applied = false,
            expiresAtDay = currentDay + days
        )
    }

    /**
     * Aplica el impacto inicial de una noticia recién generada al mercado.
     * Devuelve un par (mercado modificado, noticia con applied=true).
     */
    fun applyNewsToMarket(market: Market, news: NewsItem): Pair<Market, NewsItem> {
        if (news.applied) return market to news
        val newFactors = HashMap(market.priceFactors)
        for (rid in news.affectedResources) {
            if (!newFactors.containsKey(rid)) continue
            val curr = newFactors[rid] ?: 1.0
            val nf = clampFactor(curr * (1.0 + news.priceImpact))
            newFactors[rid] = nf
        }
        return market.copy(priceFactors = newFactors) to news.copy(applied = true)
    }

    /**
     * Aplica todas las noticias activas y aún no aplicadas. Devuelve
     * (mercado actualizado, lista de noticias actualizada).
     */
    fun applyAllPendingNews(
        market: Market,
        feed: NewsFeed
    ): Pair<Market, NewsFeed> {
        var m = market
        val updated = feed.items.map { item ->
            if (!item.applied) {
                val (m2, ni) = applyNewsToMarket(m, item)
                m = m2
                ni
            } else item
        }
        return m to feed.copy(items = updated)
    }

    /**
     * Limpieza diaria del feed: elimina noticias caducadas (mantiene últimas
     * 60 para histórico de UI). Devuelve el feed limpio.
     */
    fun pruneFeed(feed: NewsFeed, currentDay: Int): NewsFeed {
        val pruned = feed.items.filter { (it.expiresAtDay + 14) >= currentDay }
            .takeLast(60)
        return feed.copy(items = pruned)
    }

    // ---------- HELPERS ----------

    /** Construye una notificación [GameNotification] a partir de una noticia. */
    fun toGameNotification(news: NewsItem): GameNotification = GameNotification(
        id = news.id,
        timestamp = System.currentTimeMillis(),
        kind = NotificationKind.ECONOMY,
        title = "${news.emoji} ${news.headline}",
        message = news.body
    )

    /** Indica si la noticia es lo bastante relevante para empujar a notificación. */
    fun shouldNotify(news: NewsItem): Boolean =
        news.severity == NewsSeverity.MAJOR || news.severity == NewsSeverity.BREAKING ||
            abs(news.priceImpact) >= 0.10

    /** Multiplicador macro útil para efectos terceros (ventas, contratos...). */
    fun macroDemandMultiplier(econ: EconomicState): Double =
        econ.currentPhase.globalDemandMultiplier *
            (1.0 + (econ.trendStrength * 0.05))
}
