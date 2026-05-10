# Auditoría: JobsEngine

## Bugs críticos
- [ ] L153-158 — Wage explosivo. Con stat 1000+ (vía traits/prestige), `statMul = 1 + 1000*0.005 = 6.0`. Multiplicado por levelMul 3.45 + playerMul + traitMul → wage descontrolado.

## Bugs de balanceo / lógica
- [ ] L99-106 / L213-220 — Si jobLevel ≥ MAX_LEVEL=50, el carry de XP se descarta silenciosamente. Aceptable, pero debería notificarse "Has alcanzado el máximo".
- [ ] L78-150 — `workShift` (sin score) sigue activo y se usa cuando un job no tiene mini-juego implementado. Coexistencia con `workShiftWithScore` correcta.

## UX / feedback
- [ ] Sin notificación al alcanzar level 50 ("¡Has dominado el oficio!").
- [ ] L146 — Mensaje de éxito no muestra cuánto te acercas al siguiente nivel.

## Performance
- [ ] L59-71 — `checkUnlocks` itera todos los JobId.values() → 40 oficios, O(40), aceptable.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Strings notify() en español-only.

## Recomendación 1-line
**P1** — Cap `statMul.coerceAtMost(2.0)` (L153) para evitar explosión de wage con stats inflados por traits.
