# Auditoría UX — Tutorial de Onboarding

## Resumen
Tutorial guiado lineal de 15 pasos (`WELCOME` → `FINISHED`) implementado como FSM pura sobre snapshots de `GameState`. Buen andamiaje técnico, pero **cobertura insuficiente** para un juego con 30+ sistemas: enseña los 5 pilares (Imperio, Mercado, Ciencia, Tú, Patrimonio) y omite el resto.

## Pasos y condiciones
`TutorialStep.kt` L12-45 define 15 pasos. `TutorialScript.steps` L152-294 los empareja con `AdvanceCondition` (L72-95):
- L156-159 WELCOME (TAP_PRIMARY) · L160-168 OPEN_EMPIRE (TAP_TAB)
- L169-177 BUILD_FIRST (BUILT_BUILDING) · L178-186 ASSIGN_RECIPE (RECIPE_ASSIGNED)
- L187-195 HIRE_FIRST (EMPLOYEE_HIRED) · L196-204 ASSIGN_WORKER (WORKER_ASSIGNED)
- L205-213 OPEN_MARKET · L214-222 SELL_GOODS (MARKET_TX)
- L223-231 OPEN_RESEARCH · L232-240 START_RESEARCH (RESEARCH_STARTED)
- L241-249 OPEN_PLAYER · L250-258 TRAIN_STAT (STAT_TRAINED)
- L259-267 OPEN_WEALTH · L268-276 BUY_STOCK · L277-285 BUY_PROPERTY
- L286-293 FINISHED

## Cobertura vs. 30+ sistemas
**Cubierto (5):** edificios+recetas, plantilla, mercado, I+D, stats jugador, bolsa, inmuebles. **No cubierto:** payroll/eventos diarios (`TutorialEngine.kt` L124), cadenas de suministro/insumos, energía del jugador, deuda/préstamos, impuestos, eventos económicos, tab `MORE` (L126), tab `HOME`, despidos, venta de edificios, cancelación de I+D, dividendos, finanzas avanzadas. **Veredicto:** un nuevo jugador NO comprende el bucle económico completo solo con el tutorial.

## Skip path
`skip()` L74-80 marca `skipped=true` y salta a `FINISHED` sin restauración. `bumpDismiss()` L87-92 incrementa contador (TutorialState L61 sugiere mostrar Skip "tras varias dudas") pero **nada en el engine consume `dismissCount`**: la heurística depende de la UI no auditada aquí.

## Atascos potenciales
1. **MARKET_TX (L112-126):** exige delta de cash + delta total inventario + tick no múltiplo de 1.440. Si el jugador vende justo en boundary diaria, NO avanza (bug latente, parche imperfecto del BUG-08-07).
2. **RECIPE_ASSIGNED (L106-110):** cuenta edificios con receta; reasignar receta no avanza si el contador no sube.
3. **ASSIGN_WORKER:** depende de `totalWorkers` (L39); si el jugador despide antes de asignar puede quedar bloqueado sin pista.
4. **No hay timeout/hint** si el jugador no encuentra el botón; `dismissable=true` por defecto pero sin fallback narrativo.
5. **Sin precondiciones de caja:** BUILD_FIRST puede fallar silenciosamente si no hay efectivo inicial.

## Resetable
Sí: `restart()` L83-84 reinstancia `TutorialState()` por defecto (vuelve a WELCOME, limpia `completedSteps`, `skipped`, `dismissCount`). `markCompleted()` L65-71 es idempotente.

## Recomendaciones
- Añadir pasos para deuda, energía, eventos y cadenas de producción.
- Sustituir heurística MARKET_TX por hook explícito en el comando de venta.
- Implementar consumidor de `dismissCount` (mostrar "Saltar" tras N≥3).
- Añadir hint de fallback tras X ticks sin avance.

Archivos: `TutorialEngine.kt`, `TutorialState.kt`.
