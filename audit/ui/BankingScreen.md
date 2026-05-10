# Auditoría QA UX — BankingScreen.kt

## Resumen
Pantalla con 3 tabs (L32-L37): Disponibles, Mis préstamos, Hipotecas.

## Hallazgos sistema préstamos

### Lista de ofertas (APR/plazo/cantidad)
OK. `LoanOfferCard` muestra cantidad (L107), APR (L111), plazo en días (L114) y cuota/d estimada (L116). Comisión + interés total visibles (L119).

### Préstamos activos
Parcial. `ActiveLoanCard` muestra principal vivo (L203), APR y cuota diaria (L200), progreso día X/Y (L207-L211). FALTA: fecha del próximo cobro (solo días transcurridos), no hay countdown a próxima cuota.

### Repay
Incompleto. Solo dos botones fijos: "Pagar 1 cuota" (L229) y "Liquidar" (L236). FALTA input de cantidad personalizada para abonos parciales. "Liquidar" cumple rol "pagar todo".

### Refresh ofertas
Riesgo. `vm.refreshLoanOffers()` (L70) sin indicar coste ni cooldown en UI. Ambiguo para el usuario.

### Warning impago
OK parcial. Aviso amarillo "Cuotas impagadas X/N" (L213) y rojo "En mora" (L218). FALTA warning preventivo antes del primer impago (cash insuficiente para próxima cuota).

### Filtros consumer/business/mortgage
Ausente. Tab 0 mezcla todos los no-MORTGAGE (L51); hipotecas separadas en tab 2 (L245). No hay chips de filtro consumer vs business vs predatory.

## Prioridades
1. Añadir input repay parcial (L223).
2. Filtros tipo en tab 0 (L51).
3. Warning cash < dailyPayment (L212).
4. Mostrar coste/cooldown refresh (L70).
