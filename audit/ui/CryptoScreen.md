# Auditoría QA UX — CryptoScreen.kt

## Resumen
Pantalla funcional pero densa: 5 Rows apilados por token sin separación visual.

## Hallazgos

### Cards de token (L132-148)
- Border de color por tendencia (Emerald/Ruby) OK.
- Sparkline 80x28dp a la derecha (L146, L344-346): pequeño pero legible.
- Precio + volatilidad en línea secundaria (L138-141): jerarquía clara.

### Buy/Sell (L157-174)
- PROBLEMA: usa `TextButton` (L167, L170) en vez de `Button` grandes; tap target débil para acción crítica de dinero.
- Input qty compartido entre Buy/Sell/Stake (L121, L186): ambiguo.

### Stake (L176-195)
- APY visible en label del campo (L180): bien.
- "Unstake" condicional (L190-194): correcto.

### Mining + hire + auto-sell (L197-255)
- Mining row clara (L197-210); +1/-1 con TextButton diminutos (L203-204).
- Auto-mine toggle (L213-237) muestra €/día estimado: excelente feedback (L218-220).
- "🆕 Contratar" como `Button` Gold destacado (L247-254): correcto, único CTA visualmente fuerte.
- Dialog HireMinersDialog (L292-334): muestra coste, producción, nómina — completo.

### Rugpull warning
- Etiqueta "RUGGED 💀" inline (L143-144): visible pero pequeña; falta banner top o color de fondo en card. Subtítulo genérico (L54) no alerta tras evento.

## Recomendaciones
1. Convertir Buy/Sell a `Button` grandes con altura ≥48dp.
2. Separar inputs qty de Buy vs Stake.
3. Banner full-width al detectar `state.rugged`.
4. Añadir Divider entre rows de cada card.
