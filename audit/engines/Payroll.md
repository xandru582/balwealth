# Auditoría: Payroll

## Bugs críticos
- [ ] L26-28 — Impago: `payable = company.cash.coerceAtLeast(0.0)` y luego `cash - payable = 0`. Lealtad cae uniforme -0.1 sin escalar al % impago.
- [ ] L49 — Churn: empleados con loyalty < 0.2 dimiten 25% por día. Con impago seguido, equipo desaparece rápido. Sin coste de retención.

## Bugs de balanceo / lógica
- [ ] L19 — `totalSalaries` recalcula `sumOf` cada llamada. Con N=1000+ empleados (mineros legacy ya migrados, pero podría haber otros), overhead.
- [ ] No hay duplicación de cobro detectable. ✅
- [ ] Mineros agregados (Crypto): NO se cuentan aquí (van por `CryptoEngine.dailyTick`). ✅

## UX / feedback
- [ ] L29-43 — Notificación de "nóminas incompletas" cuando no hay liquidez. ✅
- [ ] Falta detalle: cuántos empleados quedaron sin pagar y cuánto se debe.

## Performance
- [ ] L19 — Cache de `totalSalaries` recomendable si hay muchos empleados.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Lealtad escalada: -0.05 si pagas <100%, -0.15 si <50%, -0.25 si <20%. Notificación con detalle del impago.
