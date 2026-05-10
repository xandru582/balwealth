# Auditoría QA UX — Pantallas Secundarias

Foco: claridad. Severidad: A (bloqueante) / M (medio) / L (pulido).

## CompanionScreen.kt

- **L73** — Símbolo `≥` y emoji 🔒 mezclados; el texto "nivel ≥ 3" puede leerse como "nivel mayor o igual" pero no todos los usuarios lo entenderán. Sugerir "nivel 3 o más".
- **M85** — `var selected by remember` se reinicia si el composable se recompone tras rotación; usar `rememberSaveable` para preservar selección de personalidad.
- **L204** — El contador "Tips activos (${ai.tips.size}/6)" no explica por qué el tope es 6; añadir tooltip o subtítulo.
- **L227** — "Limpiar todos los tips" como `TextButton` gris compite poco visualmente; al ser destructivo debería tener color de aviso (Ruby/Dim explícito) y confirmación.

## NpcsScreen.kt

- **A68** — `npc.role.name.lowercase()` muestra enum crudo (ej. `employee_special`) al usuario; no localizable y poco claro. Mapear a string traducido.
- **M127** — Misma fuga de enum en card real; `replaceFirstChar { it.uppercase() }` no arregla "Employee_special".
- **L200-224** — Cinco botones de regalo con costes hardcodeados (50/300/1500/6000); no se valida `cash` antes de ejecutar `onGift`. Botón debería deshabilitarse si no hay fondos.
- **L78** — Spacer 60dp para FAB/nav; magic number sin justificar.

## PetShopScreen.kt

- **A78** — `feedActivePet()` muestra coste "5 €" en label pero no valida fondos en UI; el botón siempre está habilitado si hay activePet.
- **M134** — Input de nombre permite vacío → al adoptar pasa `""` a `vm.buyPet`. Falta validación mínima (`isNotBlank`).
- **L66** — "Hambre 100/100" es ambiguo: ¿100 = hambriento o saciado? Etiquetar dirección.

## DreamScreen.kt

- **M107** — Texto "💤 SUEÑO LÚCIDO" en mayúsculas + 28sp es agresivo para pantalla onírica; suaviza.
- **L131** — "+30 ⚡ +5 😊 +XP" sin valor numérico de XP; inconsistente con los otros bonos.
- **L122** — Botón "Despertar" único, sin escape gesture (back); usuarios esperan poder cerrar con back.

## BalWealthScreen.kt

- **A17-27** — Pantalla casi vacía: solo delega a `BalWealthDetail`. Sin SectionTitle ni contexto. Si la carga falla el usuario ve scroll vacío. Falta estado de error/loading.

## SeasonsScreen.kt

- **M88** — Comparación `mods == SeasonModifiers.NEUTRAL` después de pintar filas: si es NEUTRAL ya se mostraron rows con "—". Lógica invertida; comprobar primero.
- **L138** — `display = "$pct%"` para valores negativos ya incluye signo; ok, pero en `pct == 0 && value != 1.0` (raro por redondeo) muestra "+0%".
- **L187** — "×$completed" sin label; ambiguo (¿multiplicador? ¿veces completada?). Añadir "completada ×N".
