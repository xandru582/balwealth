# Auditoría: OptionsEngine

## Bugs críticos
- [ ] L42/L80 — Sin límites de cantidad. Permite naked puts/calls infinitos sin colateral → exploit (vender muchas calls cuando suben, recoger primas).
- [ ] L23-24 — `OptionsPricer.fairPremium()` no visible en este archivo. Riesgo si la prima no refleja volatilidad o tiempo: explota strikes profundos OTM con primas baratas.

## Bugs de balanceo / lógica
- [ ] L101-102/L123-124 — Exercise on expiry: `intrinsicValue()` calcula correctamente, payoff agregado al cash.
- [ ] L100/L122 — Underlying con rugpull: `find()` retorna null → intrinsic = 0 → expira OTM. Sin warning al jugador de que perdió la prima.

## UX / feedback
- [ ] Sin warning visual de "esta opción venció OTM, perdiste la prima".

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Implementar margin requirement para naked positions (cash bloqueado proporcional). Auditar `OptionsPricer.fairPremium()` para asegurar Black-Scholes razonable.
