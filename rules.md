# RULES — Reglas del Proyecto

1. **No romper funcionalidades existentes:** Todo cambio debe mantener la retrocompatibilidad y la compilación limpia.
2. **No duplicar lógica:** Cada cálculo matemático tiene un único método de referencia en `SalesCalculator`.
3. **No poner fórmulas en la UI:** Los Composables solo renderizan estados provenientes de ViewModels y formatean cadenas.
4. **No utilizar valores mágicos:** Constantes de negocio, categorías y unidades de medida deben estar tipadas o documentadas.
5. **No guardar cálculos derivados innecesariamente:** En la base de datos se almacenan los datos de entrada (`openingStock`, `entries`, `closingStock`, `breakage`, snapshots de precios); los valores derivados se computan dinámicamente o se mapean a modelos de dominio.
6. **Validación estricta de datos:**
   - `entradas >= 0`
   - `roturas >= 0`
   - `inventarioFinal >= 0`
   - `precioVenta >= 0`
   - `costo >= 0`
   - `inventarioFinal + roturas <= disponible` (evitar ventas negativas).
7. **Mantener Clean Architecture:** Capas desacopladas: `data`, `domain`, `presentation`.
8. **Crear pruebas automatizadas:** Cada fórmula y regla de negocio debe tener cobertura de pruebas unitarias.
9. **Mantener Material Design 3:** Utilizar `MaterialTheme.colorScheme`, tipografía y componentes canónicos de M3.
10. **Documentar cambios:** Actualizar `memory.md` al finalizar cada fase.
11. **Gestión de Jornadas y Filtro por Calendario:** Las jornadas pueden crearse para el día actual o cualquier fecha seleccionada mediante el DatePicker de Material 3. En Reportes, el filtro de calendario permite localizar o crear cierres diarios al instante sin perder la continuidad de existencias.

