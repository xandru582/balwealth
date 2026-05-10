# Auditoría QA UX — IpoScreen.kt

## RequirementsCard (L78-115)
OK: `ReqRow` (L83-90) muestra check/cruz, valor actual vs requerido y colorea (Emerald/Ruby) en L299-304. Botón deshabilitado con texto guía "Cumple los requisitos primero" (L103). Hint de 1.440 ticks (L110) solo visible al habilitarse — debería mostrarse siempre para fijar expectativa.

## ProspectusCard (L118-144)
OK: barra de progreso (L128-132) y subtitle (L126). FALTA tiempo restante en días (sí existe en Roadshow, L153). Bug semántico: botón "Iniciar roadshow" llama `vm.completeRoadshow()` (L135) — nombre confuso o incorrecto, revisar VM.

## RoadshowCard (L147-174)
OK: progreso + `daysLeft` en subtitle (L153, L156). Mejora: mostrar `daysLeft` también junto a la barra; cuando `ready` el subtitle cambia bien.

## ListedCard (L177-216)
OK: ticker, precio, % cambio coloreado (L187-190), capitalización, yield, splits, valor paquete (L199-214). FALTA: dividendo histórico vive fuera (L42-47), correcto. Sparkline solo si history>=2 (L194), bien.

## SellDownCard (L219-259)
OK: slider (L229-238), texto "Vendes X de Y" (L227), ingreso estimado en Emerald (L239), atajos 10/25/50% (L243-247). FALTA atajo 100%/MAX y validación cuando `maxSell==0`.

## Sparkline (L326-343)
Bug: `var prev` reasignado en lambda (L334, L340) — en Compose/Kotlin moderno requiere `var` capturado; funcional pero frágil. Sin ejes ni min/max visibles.
