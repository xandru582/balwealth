# Auditoría: CryptoEngine

## Bugs críticos
- [ ] L276-281 — En despido cascada: si todos los holdings tienen 0 mineros (no debería pasar), `firstSym` es null y se pierden despidos. Añadir null-check explícito.
- [ ] L69-74 — Drift+vol del random walk: aunque hay piso `max(initialPrice * 0.0001, ...)`, con drift positivo sostenido los precios pueden inflarse arbitrariamente. Falta techo dinámico.

## Bugs de balanceo / lógica
- [ ] L340/L349 — Threshold de "polvo" 1e-6 demasiado permisivo. Stake/mining con qty muy pequeños deja restos. Subir a 1e-8.
- [ ] L270 — Cálculo `share = (toFire * minersAssigned / totalBefore).toInt()` pierde 1-2 mineros por redondeo (compensa al primero, OK).

## UX / feedback
- [ ] L245-248 — `rugpullsSurvived` se incrementa pero no se usa en achievements ni UI. O cablearlo a un logro o eliminarlo.

## Performance
- [ ] L518 (`totalMiners`) — Recorre `holdings` cada llamada (O(6)). Se llama 2× por dailyTick + UI. Cacheable en `CryptoState`.
- [ ] L87 — `news.toMutableList()` cada dailyTick → micro-allocación.

## Code smell / dead code
- [ ] L339 (`claimMining`) — sigue presente pero ahora con auto-sell suele ser irrelevante.

## Localización / texto
- [ ] (todos los textos en español; sin EN)

## Recomendación 1-line
**P2** — Null-check en cascada despidos (L276), threshold polvo a 1e-8 (L340/L349), techo dinámico de precios (L69-74).
