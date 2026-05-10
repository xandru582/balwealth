# Auditoría UX — RivalsScreen.kt

## Diferenciación activos vs derrotados
- Filtrado en `L41-42` separa listas; secciones rotuladas `Activos` (Gold, `L77`) y `Derrotados` (Emerald, `L90`).
- Borde de tarjeta cambia por estado: Emerald derrotado, Gold objetivo, InkBorder resto (`L105-109`).
- Badge `DERROTADO`/`OBJETIVO` (`L132-133`) y mensaje cierre "Recompensa cobrada" (`L203-208`). Diferenciación clara.

## Progress bar al rival actual
- `ProgressBarWithLabel` (`L165-170`) muestra cash actual vs requerido y % entero. Color Gold si `isCurrent`, Sapphire en otros activos: buen acento jerárquico. Solo se renderiza si `!defeated` (`L162`).

## Trash talk
- Pulla global persistente en cabecera con `EmpireCard` Ruby + botón "Tragar saliva" (`L57-69`). Botón "¡A ver qué dicen!" para forzarla (`L78-80`).
- Taunt por rival siempre visible en cursiva (`L149-154`). Doble canal correcto.

## Botón OPA
- `enabled = canLaunch` con colores disabled InkBorder/Dim (`L183-187`). Texto dinámico: muestra coste si habilitado, `reason` si no (`L191-192`). Feedback excelente: el porqué del bloqueo es legible.

## Texto explicativo del flow
- Header explica objetivo cash/recompensas (`L52`).
- Bajo botón OPA: nota sobre 51%, premium 20% y defensas (`L196-200`). Suficiente.

## Riesgos
- Caption OPA en Dim 10sp (`L199`): legibilidad límite.
- Sin separación visual entre bloque recompensa y OPA más allá de Spacer 8.dp; un divider reforzaría jerarquía.
