# Auditoría componentes UI — `ui/components/*.kt`

Citas: `archivo:Lnn`. Foco: defaults, recomposition, accesibilidad, theming.

## EmpireCard / SectionTitle / ProgressBarWithLabel / StatPill — `Common.kt`
- **Defaults**: `EmpireCard` aplica `borderColor = InkBorder` (`Common.kt:21`) y `RoundedCornerShape(14.dp)` hardcoded (`:26-29`); no expone `padding` ni `shape` — rigidez. `ProgressBarWithLabel` no admite `modifier` (`:69-73`), bloquea reuso.
- **Recomposition**: `progress` se pasa como lambda `{ progress.coerceIn… }` (`:79`) — bien, evita recomposición de la barra. `EmpireCard` usa `content: @Composable ColumnScope.()` — estable.
- **Accesibilidad**: ningún `contentDescription` ni `Modifier.semantics` para barras; el label es solo visual (`:76`). Falta `Role` en card.
- **Theming**: usa `InkSoft`/`InkBorder` directos (`:28,41,85`) en vez de `colorScheme.surface*` — rompe dark/light dinámico. `SectionTitle` sí usa `MaterialTheme.typography` (`:61`).

## AnimatedBadge — `AnimatedBadge.kt`
- **Defaults**: solo `count` y `modifier`; sin override de color (`Ruby` fijo `:67`).
- **Recomposition**: `Animatable` y `lastCount` en `remember` (`:44-45`); `LaunchedEffect(count)` correcto (`:47`).
- **Accesibilidad**: el número crítico (notif count) carece de `contentDescription`; lectores de pantalla no lo anuncian (`:72-77`).
- **Theming**: `Ruby`, `Ink`, `Paper` hardcoded; ignora `colorScheme.error`.

## AnimatedMoneyCounter / AnimatedIntCounter — `AnimatedCounter.kt`
- **Defaults**: `fontSize = 16.sp/14.sp` razonables (`:43, :97`); `color` obligatorio — bien.
- **Recomposition**: dos `mutableStateOf` (`previous`, `direction`) + `LaunchedEffect(value)` (`:46-64`). Riesgo: `delay(700)` siempre dispara recomposición de color aunque `direction==0`. `Crossfade` sobre `Int` (`:131`) — costoso si cambia rápido.
- **Accesibilidad**: el valor cambia sin `liveRegion`; `fmtMoney()` no se anuncia tras update.
- **Theming**: `Emerald`/`Ruby` literales (`:67-69, :120-122`) — no respeta semántica de `colorScheme`.

## EmployeeCard / BadgePill / FlowChips — `EmployeeCard.kt`
- **Defaults**: `isExecutive=false`, `execSlot=null`, `actions=null` (`:34-37`) razonables. `BadgePill` exige `tint` sin default (`:151`).
- **Recomposition**: parámetros estables (`Employee`, `EmployeeProfile?`); riesgo si `Employee` no es `@Stable`. Cálculo de `roleProfile.get()` dentro de composición (`:40`) — barato pero idealmente `remember(role)`.
- **Accesibilidad**: emojis grandes (`:46, :113`) sin `contentDescription`; barras satisfacción/burnout sin etiqueta semántica para SR.
- **Theming**: muchos colores hardcoded (`Gold`, `Sapphire`, `Emerald`, `Ruby`, `Dim` `:41,82,88,112,118,134`). `FlowChips` (`:172`) reimplementa flow manual — sustituir por `FlowRow` M3.

## QualityBadge / QualityCountBadge — `QualityBadge.kt`
- **Defaults**: `showLabel=true`, `compact=false` (`:40-41`) ergonómicos. Mapeo `colorForTier` (`:23`) puro — bien.
- **Recomposition**: stateless; recomposición trivial.
- **Accesibilidad**: `tier.label` visible pero badge sin `semantics{}` agrupado.
- **Theming**: `Color(0xFF9D4EDD)` literal (`:28`) fuera de paleta.

## GameToastHost — `Toast.kt`
- **Defaults**: `displayMs=2400, fadeMs=250` (`:64-65`) razonables.
- **Recomposition**: bucle `while(true)` con `delay(60)` (`:96-107`) consume frames; `mutableStateMapOf` para 200 IDs OK.
- **Accesibilidad**: faltan `liveRegion = Polite` en bubble.
- **Theming**: `InkSoft.copy(alpha=0.86f)` literal (`:135`).

## HelpButton / Tooltip / PolishedTopBar
- `HelpButton` (`HelpButton.kt:69`) carece `contentDescription` en `IconButton`.
- `Tooltip` (`Tooltip.kt:65`) la capa scrim no expone rol modal a TalkBack.
- `PolishedTopBar` (`PolishedTopBar.kt:111`) `shimmer` permanente fuerza recomposición continua del nombre.

## Recomendaciones priorizadas
1. Añadir `contentDescription`/`semantics` en badges, counters, IconButtons.
2. Migrar literales `Ink*`/`Gold` a `MaterialTheme.colorScheme` extendido.
3. Exponer `modifier` y `shape` en `ProgressBarWithLabel`/`EmpireCard`.
4. Reemplazar `FlowChips` manual por `FlowRow` (compose-foundation 1.6+).
5. Marcar `Employee`/`EmployeeProfile` como `@Immutable` para skippability.
