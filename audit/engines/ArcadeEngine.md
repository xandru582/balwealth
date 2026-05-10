# Auditoría: ArcadeEngine

## Bugs críticos
- [ ] L51-66 — Ningún cooldown entre `placeBet()` y `finishPlay()`. Exploit: spam de apuestas legales con resultados favorables si la RNG del minijuego no es robusta.

## Bugs de balanceo / lógica
- [ ] L80-81 — `safeWinnings = max(0.0, winnings)`: previene negativos. ✅
- [ ] L107 — JACKPOT trigger en `safeWinnings >= bet * 5.0` hardcoded → verificar si los minijuegos pueden devolver 5×bet de manera trivial.
- [ ] L43 — Apuestas clampeadas con `MIN_BET` y `MAX_BET` (depende de `ArcadeCatalog.kt`).
- [ ] TraitTree luck multiplier no aplicado aquí. Verificar si los minijuegos lo aplican.

## UX / feedback
- [ ] L126 — `totalLifetimeNet` permite auditar house edge. ✅

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Añadir cooldown de 2-3 segundos entre apuestas. Verificar que la luck multiplier (LUC stat / TraitTree) se aplica en composables de minijuegos.
