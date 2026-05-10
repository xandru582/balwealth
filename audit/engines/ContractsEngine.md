# Auditoría: ContractsEngine

## Bugs críticos
- [ ] (sin bugs críticos detectados)

## Bugs de balanceo / lógica
- [ ] L127 — Auto-deliver con `min(have, remaining)`: NUNCA entrega más de lo necesario. ✅
- [ ] L66-71 — Multa por vencimiento `minOf(company.cash, c.penaltyMissed)`: nunca deja cash negativo, aplica malus extra de reputación si no llega. ✅
- [ ] L42-46 — TraitTree multipliers (CONTRACT_REVENUE_MUL, CASH_GAIN_MUL, REPUTATION_GAIN_MUL): cableados correctamente. ✅

## UX / feedback
- [ ] L98 — Refresh diario (1440 ticks): puede ser mucho tiempo si el jugador rechaza muchas ofertas seguidas. Considerar refresh manual con coste (botón "renovar ofertas, -reputación").

## Performance
- [ ] L29-83 — Loop sobre contratos aceptados (max ~5-10): O(N), aceptable.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Strings notify() en español-only.

## Recomendación 1-line
**P3** — Botón de refresh manual de ofertas (con coste reputacional pequeño) para evitar bloqueo cuando todas las ofertas son malas.
