# Play Store Readiness Audit — BalWealth

Fecha: 2026-05-10
Alcance: Manifest, Gradle, theming, recursos para publicación.

## Resumen ejecutivo

Estado: **NO LISTO** para publicación. Bloqueadores críticos en versionCode/Name, política de privacidad ausente, theming inconsistente y backup permitido sin reglas robustas.

## Hallazgos

### 1. Identidad y versión — BLOQUEANTE
- `applicationId = "com.empiretycoon.game"` (build.gradle.kts L13) no coincide con marca **BalWealth** (`strings.xml` L3). Riesgo de rechazo por inconsistencia de marca.
- `versionCode = 1` y `versionName = "1.0.0"` (build.gradle.kts L16-L17): aceptable solo para primer release, pero ninguna estrategia de bumping documentada.
- `app_name = "BalWealth"` (strings.xml L3) — OK.

### 2. SDKs
- `minSdk = 26` (build.gradle.kts L14): razonable, cubre >97% del parque Android.
- `targetSdk = 36` y `compileSdk = 36` (build.gradle.kts L11, L15): cumple requisito Play 2025/2026 (mín. targetSdk 35). OK.

### 3. Permisos
- Único permiso: `android.permission.VIBRATE` (AndroidManifest.xml L4). Sin permisos peligrosos. OK, no requiere declaración adicional en Play Console.

### 4. Política de privacidad — BLOQUEANTE
- No existe URL de privacidad referenciada ni en manifest, strings, ni docs. Play exige Privacy Policy URL en Console aunque la app no recolecte datos. **Acción**: redactar y publicar.
- `android:allowBackup="true"` (AndroidManifest.xml L8) + `android:dataExtractionRules` y `fullBackupContent` (L9-L10): revisar `xml/backup_rules.xml` y `data_extraction_rules.xml` (presentes) para excluir DataStore con datos sensibles.

### 5. App icon adaptive — OK parcial
- `mipmap-anydpi-v26/ic_launcher.xml` L2-L5 declara adaptive-icon con foreground+background vectoriales. Cumple Android 8+.
- Falta `monochrome` para themed icons Android 13+ — recomendado, no bloqueante.
- Solo existe `mipmap-mdpi`; faltan densidades hdpi/xhdpi/xxhdpi/xxxhdpi en PNG fallback para pre-API 26 (minSdk 26 lo hace innecesario, OK).

### 6. Theming Material3 y dark mode — BLOQUEANTE menor
- `Theme.kt` L61: `colorScheme = if (dark) Dark else Dark` — **bug**: light mode ignorado pese a definir `Light` (L38-L53). Comentario "siempre oscuro" lo confirma intencional, pero rompe expectativa de usuarios con light mode forzado.
- `themes.xml` L3 hereda `android:Theme.Material.NoActionBar.Fullscreen` (Material1 legacy), no `Theme.Material3.*` ni `Theme.AppCompat`. Inconsistente con Compose Material3 (build.gradle.kts L68).
- `windowLightStatusBar=false` (themes.xml L11) hardcodea iconos claros: incompatible si activan light theme.

### 7. Otros
- `MainActivity` `exported="true"` con LAUNCHER (manifest L18, L22-L25): correcto.
- `isMinifyEnabled = false` en release (build.gradle.kts L23): habilitar R8 + shrinkResources antes de publicar.

## Acciones priorizadas
1. Habilitar `isMinifyEnabled` y `shrinkResources` en release.
2. Corregir `Theme.kt` L61 (light/dark real) o eliminar `Light` muerto.
3. Migrar `themes.xml` a `Theme.Material3.DayNight.NoActionBar`.
4. Publicar Privacy Policy y registrar URL en Play Console.
5. Añadir `monochrome` al adaptive icon.
