# Auditoría — Modelos Financieros

Alcance: `IPO.kt`, `Loans.kt`, `Options.kt`, `Contracts.kt`.

## 1. Serializable / Defaults

- `CompanyStock` (IPO.kt:50) y `IPOState` (IPO.kt:77): `@Serializable` correcto. Defaults razonables.
- `LoanOffer` (Loans.kt:74), `ActiveLoan` (Loans.kt:110), `LoansState` (Loans.kt:130): serializables; `LoanType` enum no `@Serializable` pero Kotlinx serializa enums por nombre por defecto, OK.
- `CallOption`/`PutOption` (Options.kt:20, 41) y `OptionsBook` (Options.kt:62): serializables.
- `Contract` (Contracts.kt:22) y `ContractsState` (Contracts.kt:68): serializables. `Map<String,Int>` y `Map<String,Double>` bien tipados.

## 2. Exploits en data class fields

- IPO.kt:54 `sharesOwnedByPlayer: Long` mutable vía `copy()` desde persistencia: un save manipulado puede inflar la participación. Falta `require` en init.
- IPO.kt:67 `playerStakePct` no clampea a [0,1]: si `sharesOwnedByPlayer > sharesOutstanding`, devuelve >1.
- Loans.kt:114 `remainingPrincipal` independiente de `principal`: save tampering puede dejar `remainingPrincipal=0` con `principal` alto sin pagar.
- Loans.kt:118 `dailyPayment` es campo libre, no derivado: puede setearse a 0 vía deserialización maliciosa.
- Options.kt:26 `premiumPaid` editable: un save puede registrar premium=0 manteniendo strike favorable, P&L infinito al ejercer.
- Options.kt:34 `intrinsicValue` no acota por liquidez del subyacente; combinado con `contractSize` arbitrario (no validado, Options.kt:26) permite contratos gigantes.
- Contracts.kt:33 `deliveredQty` libre: cliente puede marcar entregado sin haber producido.
- Contracts.kt:27 `paymentPerUnit` Map externo: save editado eleva precio sin tope.

## 3. Validaciones ausentes

- Ningún `init { require(...) }` en las cuatro data classes.
- IPOState.estimateValuation (IPO.kt:91) usa `(reputation-50).coerceAtLeast(0)` pero no acota `level` (overflow lineal infinito).
- Loans.kt:212 APR puede caer bajo `baseAprMin` por `rng.nextDouble(0.85,1.15)` antes del clamp en :240; clamp existe, OK.
- Loans.kt:219 `cashFlowBoost` no admite negativo extremo: clamp a [0,50000] ignora pérdidas grandes (no exploit, diseño laxo).
- Options.kt:106 `vol.coerceIn(0.01,1.0)` correcto; `strike.coerceAtLeast(0.01)` evita div/0 (Options.kt:115).
- Contracts.kt:161 `qty.coerceAtLeast(1)` OK; falta tope superior — `tierMul` con `tier=5` y rango alto puede generar lotes desmesurados.
- Contracts.kt:54 `deadlineTick` suma sin chequear overflow Long (teórico).

## Recomendaciones

Añadir `init { require(...) }` validando no-negatividad y consistencia (`remainingPrincipal<=principal`, `sharesPublic+ownedByPlayer<=outstanding`, `premiumPaid>=0`, `deliveredQty[k]<=items[k]`). Marcar campos derivados con `@Transient` o recalcularlos al cargar.
