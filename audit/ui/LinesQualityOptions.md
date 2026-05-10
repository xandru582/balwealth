# Auditoría UX — Lines / Quality / Options

Fecha: 2026-05-10
Alcance: `LinesScreen.kt`, `QualityInventoryScreen.kt`, `OptionsScreen.kt`.

## 1. Chain visualization (LinesScreen.kt)

- `LineChainVisual` (L139-178) renderiza la cadena con emoji + nombre de receta + flecha "→". Funcional pero el texto a `9.sp` (L165) es ilegible en pantallas densas; recomendado mínimo 11.sp.
- `LineCard` muestra estado por etapa (L112-124) concatenado con `·` — si hay 5+ etapas se trunca/horizontal-overflow al no usar `LazyRow`.
- Cuello de botella (L101-109) calcula `maxOrNull()` ignorando edificios sin receta: puede mostrar `0s` engañoso (L105).
- Falta indicador visual por-etapa (color rojo/ámbar cuando `b == null` o `assignedWorkers == 0`); todo se relega al texto plano de L114-119.

## 2. Quality tier display (QualityInventoryScreen.kt)

- Iteración correcta `QualityTier.ascending.reversed()` (L67) — muestra tiers altos primero. Bien.
- Cabecera de tier (L70-89) usa `colorForTier` + emoji + multiplicador `x%.2f` (L85): claro y consistente.
- `QualityBadge` repetido por fila (L101) además del header de tier es redundancia visual; considerar omitir badge dentro de su propio grupo.
- Diálogo `SellQualityDialog` (L141-202): el botón `Máx` (L182) no tiene feedback si `available==0`; no se previene amount=0 en confirmar (`coerceAtMost` no fuerza ≥1, L192).

## 3. Options chain calls/puts (OptionsScreen.kt)

- `BuyOptionsTab` no muestra cadena (chain) tipo broker — solo configurador con sliders (L105-123) y chips de expiry (L129-139). Falta tabla strike×expiry.
- Breakeven calculado solo si `stock != null` (L164-168); divide premium entre contract size — correcto pero no explica fee.
- `CallCard`/`PutCard` (L244-334) prácticamente duplicados; refactor a composable común reduciría 90 líneas.
- `daysLeft` divide ticks/1440 (L250, L296) sin `coerceAtLeast(0)`: opciones expiradas mostrarán negativos.
- ITM/OTM color en L260/L306 usa `Ruby` para OTM — confunde con pérdida; usar `Dim` neutro.
