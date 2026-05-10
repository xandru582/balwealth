# Auditoría: IpoEngine

## Bugs críticos
- [ ] L80 — `computeIpoPrice()` se asume `> 0` sin validar. Si la valuation es muy baja, el `coerceAtLeast(1.0)` evita 0, OK. Pero falta guard contra NaN si valuation es Infinity.

## Bugs de balanceo / lógica
- [ ] L34 — `IPOState.estimateValuation()` no se valida — depende de cash + reputación + buildings. Si el jugador acumula trillions de cash, la valuation es astronómica y los números se rompen visualmente.
- [ ] L113 — `sellDownStake`: `coerceIn(0L, sharesOwnedByPlayer)` correcto, sin sobreventa.
- [ ] L159 — `tickPrice` con `coerceIn(0.85, 1.20)` y piso 0.5 → fence adecuado.

## UX / feedback
- [ ] El feedback de progreso de roadshow (3 días) podría mostrarse con barra más prominente (verificar en UI screen).

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Strings notify() en español-only.

## Recomendación 1-line
**P2** — Cap de valuation máxima (`min(raw, 1e15)`) en `estimateValuation()` para evitar números absurdos.
