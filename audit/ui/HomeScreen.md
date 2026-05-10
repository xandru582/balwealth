# Auditoría: HomeScreen

## Bugs críticos
- [ ] L27-38: no hay TopAppBar/BottomBar/Drawer; navegación inalcanzable desde HomeScreen, jugador queda atrapado tras splash.
- [ ] L92: `state.realEstate` puede NPE si jugador no posee inmuebles (no hay null-check).

## Bugs de balanceo / lógica
- [ ] L99-100: `pending` y `open` recalculan el filtro en cada recomposición; sin `remember`, costoso si `quests` crece.
- [ ] L100: `.take(3)` oculta misiones en curso sin indicar que hay más.

## UX / feedback
- [ ] L121-124: `Button "Cobrar"` sin `enabled`/loading state; doble-tap puede reclamar dos veces.
- [ ] L85-92: `StatPill` sin `Modifier.clickable` ni semántica; cash del jugador (`state.player.cash`) no aparece en el resumen — info clave escondida.
- [ ] L52: hora/día sin contraste accesible (`Dim`); difícil leer.
- [ ] L36: Spacer 60.dp hardcoded para "respeto" a bottom-bar inexistente.

## Performance
- [ ] L151: `notifications.reversed().take(6)` por recomposición; usar `derivedStateOf` + `asReversed`.

## Code smell / dead code
- [ ] L85-92: emojis Unicode crudos `🏭` en lugar de iconos vectoriales/recurso.
- [ ] L28,29,30,36,52,113,155: paddings/sizes hardcoded (12/10/60/4/6 dp) sin tokens de tema.

## Localización / texto
- [ ] L44-46,49,52,57,63,71,103-104,108,118,124,131,146,149: TODO el texto en español hardcoded; cero `stringResource()`.
- [ ] L118: formato `"+X XP · $ · +Y rep"` no pluraliza ni se traduce.

## Recomendación 1-line
P0 — añadir Scaffold con BottomNavigation, mostrar `player.cash`, extraer strings a `res/values-es/strings.xml`.
