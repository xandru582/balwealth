# Auditoría QA UX — ContractsScreen.kt

Ruta: `app/src/main/java/com/empiretycoon/game/ui/screens/ContractsScreen.kt`
Tabs: Ofertas / Activos / Histórico (L35-L51).

## Hallazgos

### Ofertas (OfferCard L101-L176)
- Payment visible: total `c.totalPaymentEstimate` + bonus on-time (L114-L121). OK.
- Items requeridos: emoji, nombre, cantidad y `paymentPerUnit` por recurso (L126-L146). OK.
- Deadline: `Plazo` formateado (L150-L153). OK.
- Penalty preview de impago: `Multa` mostrada en rojo Ruby (L155-L158). OK.

### Aceptados (AcceptedCard L203-L287)
- Progreso global con `ProgressBarWithLabel` y % (L233-L237). OK.
- Progreso por ítem: `delivered / needed (stock)` + barra lineal (L262, L275-L283). OK.
- Urgencia por color según tiempo restante (L205-L209). OK.

### Manual delivery (L266-L272)
- Botón "Entregar" hace `minOf(have, remaining)`; NO hay input de qty manual — entrega máxima auto. Falta TextField/stepper.

### Reject offer (L170-L173)
- "Descartar" llama `vm.rejectContract` sin diálogo, motivo ni feedback/snackbar. Sin confirmación, fácil mis-tap.

### Refresh (L74-L80)
- Botón "Actualizar lista" llama `vm.refreshContracts()`. Sin coste/cooldown visible ni estado loading.

## Riesgos
- Sin input qty: no permite reservar stock para otros contratos.
- Sin confirmación reject: pérdida accidental de oferta.
- Empty-state ofertas (L83-L90) no sugiere cuándo refrescar.
