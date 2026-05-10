# Auditoría UX — Achievements / Player / Wealth

Fecha: 2026-05-10
Alcance: progreso, claim, training stats, distribución de wealth.

## AchievementsScreen.kt

- L57-L72: cabecera muestra `unlocked / total`, `claimed`, y porcentaje. Falta exponer total de recompensas pendientes de reclamar (cash + XP); el jugador no sabe cuánto deja sobre la mesa.
- L66, L76: cálculo del porcentaje duplicado; extraer a `val` para evitar recomputación.
- L173-L177: `progress = maxOf(rawProg, storedProg)` — bien, evita retroceso visual, pero no se cita la fuente al usuario. Sin tooltip ni unidad (ej. "$" vs "uds").
- L235: formato `"$cap / ${ach.threshold}"` no usa `fmtMoney` aun cuando la categoría sea WEALTH; umbrales millonarios se muestran sin separador.
- L258-L272: botón "Reclamar" ocupa el mismo slot en estados Bloqueado/Reclamar/Reclamado; OK, pero el estado "Reclamado" debería ser no-Button (Text) para evitar feedback ripple inútil.

## PlayerScreen.kt

- L51-L54: barra XP usa `xpForNextLevel()`; no se muestra delta al subir de nivel ni recompensas. Añadir hint "Sube de nivel para…".
- L82: `value / max.toFloat().coerceAtLeast(1f)` — la coerción se aplica al float pero `value/max` ya pudo dividir por cero antes si `max=0` (Int division). Usar `value.toFloat() / max.coerceAtLeast(1)`.
- L94, L96-L105: subtítulo dice "10 ⚡ y sube 1 punto" pero el botón "Entrenar" (L121-L125) no muestra coste inline ni se deshabilita cuando `energy < 10`. Riesgo de click fallido silencioso.
- L145: "Trabajar" exige `energy >= 15`, distinto del entrenamiento (10). Inconsistencia no documentada.

## WealthScreen.kt

- L34-L36: solo dos pestañas (Bolsa, Inmuebles); el título "Wealth" sugiere también crypto/banking — ver navegación global.
- L62-L77: cartera muestra valor y P&L pero no % retorno ni coste total invertido.
- L177: `vm.buyShares` en diálogo personalizado no valida `qty <= maxAffordable` en cliente (lo hace `canBuy` L195 pero el botón sigue ejecutando si se pulsa rápido — OK por guard).
- L251-L252: `values.min()/max()` lanza si `priceHistory` vacío; protegido por L103 `isNotEmpty`, correcto.
- L333-L335: ROI anual ignora impuestos/vacancia; etiquetar como "bruto".
