# Auditoría: DailyChallengeEngine

## Bugs críticos
- [ ] L29-39 — `evaluateAndAutoClaim` recalcula progreso pero auto-claim está deshabilitado (L37). Progress se evalúa por snapshot comparativo (L44-57), pero no captura cambios si el jugador no actúa en el día.
- [ ] L116 — `expiresAtTick` no se valida en `evaluateAndAutoClaim`: retos viejos visibles si el engine falla.

## Bugs de balanceo / lógica
- [ ] L141 — Reto semanal paga 500K€ × tier vs diarios ~30K. Diferencia de 16× entre semanal y diario. Considerar 5× × 7 = 35× para coherencia.
- [ ] L259 — `WIN_RACE` (F1) requiere victoria. Si el equipo del jugador es Phoenix Garage (peor team) sin pilotos top, prácticamente imposible → reto inalcanzable.
- [ ] L277 — `HAPPINESS_THRESHOLD ≥70`: trivial si el jugador empieza con happiness 80.

## UX / feedback
- [ ] Sin animación de "completaste un reto, claim aquí".

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Validar `expiresAtTick` en evaluación. Reset de retos imposibles cuando el jugador no cumple precondiciones (sin equipo F1 → no asignar WIN_RACE).
