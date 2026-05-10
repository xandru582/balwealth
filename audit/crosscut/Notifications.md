# Auditoría UX — Sistema de Notificaciones (Toast)

**Archivo auditado:** `app/src/main/java/com/empiretycoon/game/ui/components/Toast.kt`
**Fecha:** 2026-05-10

## Resumen
Host único `GameToastHost` (L60-125) renderiza una notificación a la vez con cola FIFO y deduplicación por `id`. Diseño translúcido, no bloquea input.

## Hallazgos

### Queue logic
- Cola en `mutableStateListOf` (L70). Encolado vía `LaunchedEffect(notifications)` (L77) iterando `takeLast(20)` — solo procesa las 20 más recientes; ráfagas mayores se pierden silenciosamente.
- Deduplicación triple: `shownIds` map (L68, L79), check vs `current` (L80), check vs cola (L81). Robusto contra recomposición.
- **Cap de cola = 8** (L86): `removeAt(0)` descarta los más antiguos al sobrepasar. Razonable, pero no notifica al usuario que se perdieron eventos (potencial pérdida de info crítica como ERROR/ECONOMY).
- **Cap de `shownIds` = 200** (L88-91), poda a 150 IDs más recientes. Previene leak; ok.

### Dismiss timing
- `displayMs = 2_400L` por defecto (L64) + `fadeMs = 250` (L65). Total ~2.65s por toast.
- Loop secuencial (L95-108): no hay forma de **dismiss manual** — el usuario debe esperar siempre. UX deficiente para mensajes largos o ERROR críticos.
- Polling de 60ms cuando idle (L105) — aceptable, sin impacto perceptible.
- Tiempo **fijo** independiente del `kind`: un ERROR dura igual que un INFO. Recomendable extender duración para WARNING/ERROR.

### Opacity
- Fondo `InkSoft.copy(alpha = 0.86f)` (L135), borde `accent.copy(alpha = 0.55f)` (L136). Translúcido legible; contraste adecuado para `Paper`/`Dim` sobre `InkSoft`.

### Blocking input
- Comentario explícito L52-53: ni host ni burbuja tienen `clickable`. Confirmado: `Box(modifier)` (L112) y `Row` (L130) sin gestos. **No bloquea** — gestos pasan a la UI inferior. Correcto.

### Stacking / máximo simultáneo
- **Máximo simultáneo = 1** por diseño (L46-48, L97 condición `current == null`). No hay stacking visual. Decisión consciente, evita tapar pantalla.

## Riesgos UX
1. Sin dismiss manual ni acción asociada (tap-to-action).
2. Duración fija no diferenciada por severidad.
3. Pérdida silenciosa al desbordar cola de 8.
4. Sin indicador de cola pendiente (badge "+N").
5. `takeLast(20)` (L78) puede descartar notificaciones legítimas en bursts.
