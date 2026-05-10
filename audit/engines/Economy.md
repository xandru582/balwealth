# Auditoría: Economy + EconomicEngine

## Bugs críticos
- [ ] EconomicEngine.kt L213-214 — News feed `takeLast(60)` mantiene 60 items, pero `expiresAtDay + 14` deja items 14 días post-expiración. `pruneFeed()` no se llama en cada tick → growth potencial sin límite si no se invoca regularmente.

## Bugs de balanceo / lógica
- [ ] Economy.kt L28-29 — Random walk con `clampFactor()` evita 0/Infinity. ✅
- [ ] EconomicEngine.kt L109-122 — Macroeconomic cycle aplica `globalDemandMultiplier` con 5%/día reversion. CRASH (-0.95) y BOOM (+0.7) coherentes.
- [ ] Economy.kt L59-71 — `tickStocks()` no implementa dividendos aquí (delegado a GameEngine). Sin doble cobro detectable en este archivo.

## UX / feedback
- [ ] EconomicEngine.kt L145-151 — Probabilidad de news 0.0012-0.0036 = 1-3 noticias/día. Podría ser baja y dar sensación de mundo muerto.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Llamar `pruneFeed()` cada N días para evitar bloat. **P3** — Aumentar tasa de news a 3-5/día para más viveza.
