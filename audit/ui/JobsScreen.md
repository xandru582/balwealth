# Auditoría QA UX — JobsScreen.kt

## Lista de oficios: locked vs unlocked
- Iteración por `JobCategory` con conteo `unlocked / total` (L262-L268, L329).
- Borde dinámico según nivel/desbloqueo (L347-L352): `InkBorder` locked, Sapphire/Emerald/Gold por tier.
- Locked: nombre en `Dim`, mensaje rojo `🔒 Requiere nivel X` (L369, L385-L388). Sin botón Trabajar (L391).

## Energy display
- Header global: `${energy}/${maxEnergy} ⚡` (L231).
- Coste por oficio visible en línea de stats `⚡-${energyCost}` (L375).
- Botón Trabajar deshabilitado si `energy < energyCost` (L277, L399). Falta feedback explícito del motivo cuando está disabled.

## Wage preview
- `JobsEngine.previewWage` por oficio, mostrado en Emerald + "/ turno" (L276, L393-L395). Correcto.

## Bolsa de empleo accept dialog
- No hay diálogo de confirmación: `JobsWelcome` con dos `EmpireCard` informativas y botón directo `vm.jobsAccept()` (L202-L209). Se etiqueta "irreversible" (L192) pero sin AlertDialog.

## Stat preferido visible
- Mostrado solo si unlocked: `Stat: ${job.preferredStat.name}` (L375). No visible en locked.

## Ordenación
- Solo agrupado por categoría (L259-L260). NO existe sort por preferred stat / level / unlocked. Sin filtros ni toggles.

## Hallazgos
- Falta confirm dialog accept (irreversible).
- Sin ordenación/filtros de 40 oficios.
- Stat oculto en locked impide planificar.
- Disabled button sin tooltip de energía.
