# Audit UI — Job Minigames

Auditadas 5 pantallas en `app/src/main/java/com/empiretycoon/game/ui/screens/jobs/`. Patrón compartido: callback `onFinish(scoreMul: Double)` (delega `vm.workShiftWithScore` al caller, no invocado directamente aquí), `onCancel`, timer 30s, score visible, botón "Plantarse" para cerrar antes y "Cancelar" sin coste.

## PoliceJobScreen.kt — tap-reaction
- Objective claro: "Tap sospechosos rojos · evita civiles verdes" (L120-121).
- Score visible: `$score / $maxScore` (L141-143). Wage previsto live (L146-152).
- Timer: 30s con `LinearProgressIndicator` (L130-133); contador grande (L125).
- Finish: `onFinish(scoreMul)` en botón "Cobrar y volver al hub" (L225-231).
- Abandon: `onCancel` (L235) y `Plantarse` (L242) ambos presentes.
- Issue UX: targets posicionados con offset hardcoded 200×320 dp (L181-183) — desbordan en pantallas grandes.

## TaxiJobScreen.kt — pickup/dropoff
- Objective: "Recoge en azul y deja en naranja" (L121-122) — emojis 🟦/🟧 ambiguos vs colores reales (L184-185).
- Score: "Trayectos $score / $maxScore" (L140-143) + estado pasajero (L145-149).
- Timer: 30s + barra (L131-134).
- Finish: `onFinish(scoreMul)` (L241).
- Abandon: cancelar (L251) + plantarse (L258). OK.

## ProgrammerJobScreen.kt — find-the-bug
- Objective explícito (L191-192) + hint post-feedback (L271-275). Excelente clarity.
- Score visible (L211-213); doble timer (total + ronda 5s, L201-204, L228-236).
- Finish: `onFinish(scoreMul)` (L290).
- Abandon: ambos botones (L300, L307).
- Issue: highlight de respuesta correcta en error (L243-244) revela solución sin penalizar suficiente — debería esperar a soltar.

## StreamerJobScreen.kt — sequence
- Objective: "Tap los colores en el orden mostrado" (L146).
- Score + longitud secuencia visible (L165-169). Doble timer OK (L156-159, L183-191).
- Finish: `onFinish(scoreMul)` (L262).
- Abandon: ambos (L272, L279).
- Issue: emojis del color set a `Color.Transparent` cuando done (L218) — accesibilidad pobre para daltónicos sin label de color.

## DoctorJobScreen.kt — precision-tap
- Objective: "Tap puntos rojos. NO toques cuando no haya nada" (L79).
- Score: "Cirugías $score / $maxScore" (L92-93). Wage live (L96-98).
- Timer: 30s (L83, L87).
- Finish: `onFinish(scoreMul)` botón "Cobrar y volver" (L124).
- Abandon: cancelar (L127) + plantarse (L129).
- Issue crítico: NO hay overlay "done" como los otros (cf. Police L199-217) — fin abrupto sin resumen. Score teórico 30 con visible toggling cada 700ms es matemáticamente inalcanzable (~21 max).

## Hallazgos transversales
- Consistencia textual y de timing 30s: buena.
- Ningún screen llama `vm.workShiftWithScore` directamente; lo hace el orchestrator vía `onFinish`.
- Falta haptic/SFX feedback en tap correcto/erróneo en todos.
- "Plantarse" usa color rojo (#B85C5C) idéntico al de error — confusión visual.
