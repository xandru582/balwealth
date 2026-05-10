# Auditoría QA UX — MultiCity / Heists / Disasters

## MultiCityScreen.kt

**Claridad de info**
- Buen onboarding en estado bloqueado: requisitos coloreados verde/rojo (L60-67) y guía numerada "Cómo funciona" (L73-80).
- Ratio FX/aranceles/tax en línea densa monocroma (L247-253), difícil escaneo. Sugerir chips o iconos por concepto.
- "Texto orientativo del beneficio aproximado" (L482) demasiado pequeño (9.sp) y técnico; conviene preview numérico real.

**Botones de acción**
- "Enviar/Cerrar/Abrir" como TextButton sin icono (L298-310), poco jerárquico vs CTA principal.
- Botón "Enviar" del modal valida cash, capacidad e inventario (L471-473) — correcto, pero sin mensaje explicando por qué se deshabilita.

**Feedback**
- Sentiment dot (L229) sin tooltip/leyenda.
- ShipmentRow muestra ETA en d/h (L333) — bien. Falta progress bar.

## HeistsScreen.kt

**Claridad de info**
- Stats agregadas en una línea (L66) — sobrecarga; separar perfectos/desastres visualmente.
- Roles requeridos solo como emojis (L150-152) sin tooltip de nombre.

**Botones de acción**
- "Planificar" sin estilo destacado (L209-217) pese a ser CTA principal.
- "🔥 EJECUTAR HEIST" usa color Ruby como texto pero Button por defecto (L221-223), inconsistente con MultiCity (Gold container).
- Selección crew sin indicar máximo permitido ni roles cubiertos.

**Feedback**
- Estados HeistStatus.EXECUTING/COMPLETED no muestran nada (L228) — usuario queda sin feedback.
- Outcome solo aparece como texto (L154-159), sin animación ni resaltado por éxito/fracaso.

## DisastersScreen.kt

**Claridad de info**
- Multiplicadores de producción/precios (L108-113) bien etiquetados con emojis.
- Histórico (L86-95) muestra severidad como `.name` enum crudo, poco amigable.

**Botones de acción**
- Estrategias mitigación como TextButton planos (L121-123) sin coste/efectividad visible — decisión a ciegas.
- Sin botón "ignorar/aceptar pérdidas" explícito.

**Feedback**
- Cuenta atrás "24h in-game" textual (L117) sin temporizador visual.
- Switch seguro (L51-54) sin confirmación de activación.
