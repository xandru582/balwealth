# Auditoría: BankingEngine

## Bugs críticos
- [ ] L175-179 — Descuento doble del principal cuando hay penalización por impago previo: `pay = basePay * penalty` y luego `principalReduce = pay - interest` → si la penalización ya redujo `basePay`, restar interés "doble-aplicado" deja menos principal del esperado.

## Bugs de balanceo / lógica
- [ ] No hay tope de endeudamiento (préstamos simultáneos ilimitados, sin ratio deuda/ingresos). Permite exploit: tomar 10 préstamos para llenar caja y luego incumplir uno.
- [ ] L111 — Refresh offers: ofertas basadas en `cashFlow` estimado (L238-241) sin caps de APR mínimos/máximos visibles → revisar `LoanOfferGenerator`.

## UX / feedback
- [ ] L195-206 — Default penalty: aplicación correcta de `DEFAULT_REP_PENALTY` pero sin notificación clara al jugador del estado de cada préstamo.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Strings notify() en español-only.

## Recomendación 1-line
**P1** — Añadir tope de endeudamiento (max 3-5 préstamos o ratio deuda/cash <= 5×). **P2** — Validar fórmula de descuento de principal cuando hay penalización (L175-179).
