# Auditoría: HeistEngine

## Bugs críticos
- [ ] L197-206 — Probabilidad de éxito sin cap superior efectivo. Con crew skill 99 + gear 250k + luck 5: score puede pasar de 1.4, dando 65%+ de PERFECT. Falta `score.coerceAtMost(0.85)`.
- [ ] L215-217 — `cutPaid = grossLoot * sumOf(cutPct)`: si la suma de cuts excede 1.0, el pago supera el botín y deja números negativos al jugador.

## Bugs de balanceo / lógica
- [ ] L188 — Crew skill stack: average correcto, pero sin límite `crew.size <= 4`. Crew con 6+ miembros buenos infla artificialmente.
- [ ] L50-51/L254 — Heat: cap [0,100] y decay correctos. ✅
- [ ] L242/L85 — Cooldown 3 días enforced. ✅

## UX / feedback
- [ ] Falta preview claro del resultado esperado antes de ejecutar.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Validar `sum(cutPct) <= 1.0` al añadir crew (L215). Cap de score en 0.85 (L197). Limitar crew.size a 4 (L188).
