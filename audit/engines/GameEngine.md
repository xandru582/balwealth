# Auditoría: GameEngine

## Bugs críticos
- [ ] L252/L787 — `System.nanoTime()` puede generar IDs de notificación duplicados en mismo tick → usar `state.tick * 1000 + counter` o UUID.
- [ ] L245-246 — División en dividendos sin guard contra NaN/Infinity (`stock.price * stock.annualDividendYield`).
- [ ] L147-158 — `EconomicEngine.generateNewsTick()` ignora `state.paused` (eventos económicos siguen generándose pausado).

## Bugs de balanceo / lógica
- [ ] L48-49 — Investigación con `researchSpeedMul` muy alto puede saltar de positivo a negativo en un solo tick (skip de ticks).
- [ ] L242-246 — O(N²) potencial en dividendos: loop stocks + lookup en holdings.

## UX / feedback
- [ ] L251-257 — Notificaciones de dividendos spam si yield < 0.10 €/día → añadir umbral mínimo.
- [ ] L268-295 — `NpcWorldEngine`, Weather y Traffic avanzan sin paused check → desincronización visual.

## Performance
- [ ] L694 — `HashMap(inventory)` allocation en `resolveEvent()` → cambiar a `.toMutableMap()`.
- [ ] L242-246 — O(N²) en dividendos diarios.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (todos los strings en español, sin EN equivalente)

## Recomendación 1-line
**P1** — Añadir paused checks (L147, L268, L283, L293), IDs únicos por tick (L252/787), guard NaN/Infinity en dividendos (L245).
