# Accessibility Audit (UI Cross-cut)

**Scope**: muestreo de `app/src/main/java/com/empiretycoon/game/ui/**/*.kt` (componentes, pantallas, tema).
**Fecha**: 2026-05-10. **Severidad**: A (alto), M (medio), B (bajo).

## Resumen ejecutivo
Tema 100 % oscuro (Theme.kt:61 fuerza `Dark` siempre). Tipografía mínima en `labelSmall` = 11 sp (Type.kt:48), pero abundan overrides con 10 sp y emojis utilizados como íconos sin `contentDescription`. Tap targets bajo 48 dp en HUD, HelpButton y top bar. Sólo 2 archivos referencian `contentDescription` (Root.kt, PolishedTopBar.kt).

## Hallazgos

### A1. Tap targets < 48 dp (Severidad A)
- `components/HelpButton.kt:63,71` — `IconButton` envuelto en `Box.size(32.dp)` y modifier `.size(32.dp)`. Mínimo Material = 48 dp.
- `components/PolishedTopBar.kt:124` — `IconButton` mute música `Modifier.size(36.dp)`.
- `components/PolishedTopBar.kt:284` — botón `.size(36.dp)`.
- `components/Toast.kt:142` — `.size(28.dp)` en control interactivo.
**Impacto**: tap difícil para usuarios con motricidad reducida; viola WCAG 2.5.5.
**Fix**: usar `Modifier.minimumInteractiveComponentSize()` o `48.dp`.

### A2. Iconos/emoji sin `contentDescription` (Severidad A)
Sólo Root.kt y PolishedTopBar.kt referencian `contentDescription` en todo `ui/`. Casos críticos:
- `components/PolishedTopBar.kt:127` — botón mute usa `Text("🔊"/"🔇")` sin descripción accesible.
- `components/HelpButton.kt:73-78` — botón "?" sin label semántica para TalkBack.
- `components/Common.kt:46` — `Text(emoji)` en `StatPill` sin descripción ni `Modifier.semantics{}`.
- `screens/AchievementsScreen.kt:223,288` y `screens/EmpireScreen.kt:336` — `Text("✅")`/`Text("🧑")` portan información sin texto alterno.
**Fix**: añadir `Modifier.semantics { contentDescription = "Silenciar música" }` o duplicar con `Text` visible.

### A3. Texto < 11 sp (Severidad M)
- `Root.kt:265` — etiquetas de `NavigationBar` `fontSize = 10.sp`.
- `screens/AchievementsScreen.kt:236`, `screens/ArcadeScreen.kt:187,209` — métricas `fontSize = 10.sp`.
- `components/AnimatedBadge.kt:75` — contador 10 sp sobre fondo de color.
**Recomendación**: mínimo 12 sp para texto secundario (Material 3 `labelSmall`).

### A4. Contraste de colores (Severidad M)
`theme/Theme.kt:10-19` no existe `Color.kt` (esperado). Paleta:
- `Dim = #8899AA` sobre `Ink #0F1724` ≈ ratio 5.7:1 — OK para body, **insuficiente** cuando se usa a 10–11 sp con `FontWeight.Normal` (AAA exige 7:1).
- `Gold #FFD166` sobre `Midnight #073B4C` ≈ 9:1 — OK.
- `onSurfaceVariant = Dim` aplicado a labels de 11 sp (Common.kt:50) está al límite.
**Fix**: subir `Dim` a #A6B5C5 o forzar `FontWeight.Medium` en textos atenuados.

### A5. Botones con sólo emoji (Severidad A)
- `PolishedTopBar.kt:122-130` — IconButton con sólo "🔊"/"🔇".
- `HelpButton.kt:69-79` — botón "?" sin label.
- `Root.kt:419` — `Text(sub.emoji, fontSize = 22.sp)` clickeable en grilla.
**Fix**: añadir `contentDescription` por `Modifier.semantics`.

## Otros
- `Theme.kt:61`: `if (dark) Dark else Dark` impide tema claro — bloquea preferencias de usuario / alto contraste del sistema.

## Acciones priorizadas
1. Wrapper `AccessibleIconButton(emoji, description)` y migrar TopBar/HelpButton/Common.
2. Bump tap targets HUD a 48 dp.
3. Subir mínimo tipográfico a 12 sp + revisar Dim contrast.
4. Habilitar tema claro real en `EmpireTheme`.
