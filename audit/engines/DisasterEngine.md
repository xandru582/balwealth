# Auditoría: DisasterEngine

## Bugs críticos
- [ ] (sin bugs críticos detectados)

## Bugs de balanceo / lógica
- [ ] L165 — `productionMul` sin piso simétrico (otros caps son 0.1-2.0; éste solo `coerceAtMost(1.0)`). Asimétrico — inconsistente con el resto.
- [ ] L83/L243-248 — Resilience XP: LOW=1, MEDIUM=3, HIGH=7, CATASTROPHIC=15 → progresión razonable, sin exploit aparente.
- [ ] L98-143 — Daño aplicado a campo correcto en cada tipo de desastre. ✅

## UX / feedback
- [ ] DAILY_TRIGGER 5%: aproximadamente un desastre cada 20 días. Aceptable.
- [ ] L116/L128 — Insurance reduce daño: bien aplicado.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Cap simétrico `productionMul` 0.5..1.5 para uniformidad (L165).
