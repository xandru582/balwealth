# Auditoría: JobBusinessEngine

## Bugs críticos
- [ ] L254 — Si pasan múltiples días offline (rawTicksElapsed > 1440), se calcula `daysToPay` proporcional pero el payroll solo se ejecuta UNA vez. Después de cubrir 1 día con cascada de despidos, los días restantes no se cobran ni se aplican. Posible exploit: cerrar app durante varios días para evitar nóminas.

## Bugs de balanceo / lógica
- [ ] L257-283 — Treasury negativo bloqueado correctamente con cascada de despidos. ✅
- [ ] L92 — Upgrade level cap = 5. ✅
- [ ] L243 — OFFLINE_CAP_TICKS = 1440 (1 día). Sin embargo coincide con la limitación anterior: si `lastPayrollDay` no se respeta correctamente, el offline cap no salva.

## UX / feedback
- [ ] Falta preview de "ingresos por día esperados" en la UI antes de upgradearr.

## Performance
- [ ] L272-275 — Sort empleados desc por salario en cada cascada → O(N log N), aceptable (N pequeño).

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P0** — Validar que `lastPayrollDay` se actualiza tras cada día procesado en el loop offline (L254), no solo una vez.
