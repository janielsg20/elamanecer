# MEMORY — Sistema de Control de Ventas e Inventario

## 1. Visión y Propósito
Reemplazo estructurado, automatizado y seguro del archivo Excel:
`ANALISIS DE LAS VENTAS JUNIO2026 LISTO PARA LA VENTA.xlsx`

El sistema digitaliza el proceso:
`Productos → Inventario → Entradas → Ventas → Costos → Utilidades → Cierres Diarios → Reportes`

## 2. Decisiones de Arquitectura y UX
- **Plataforma:** Android Nativo con Kotlin.
- **UI:** Jetpack Compose + Material Design 3.
- **Tema:** Soporte de Tema Claro (predeterminado) y Tema Oscuro con selector instantáneo (`ThemeManager` y botón de cambio de tema en la barra superior).
- **Tipografía y Maquetación:** Escala tipográfica calibrada (`Type.kt`) para pantallas móviles con pesos proporcionales (`Modifier.weight(1f)`), límites de líneas (`maxLines = 1`) y elipsis (`TextOverflow.Ellipsis`) para evitar cualquier desbordamiento o salto de línea indeseado en filas, columnas y tarjetas.
- **Base de Datos:** SQLite local mediante Android Room (versión 2 con soporte de eliminación en cascada `ForeignKey.CASCADE`), DAOs y Flow reactivo.
- **Corrección y Anulación de Ventas en Inventario:** Los usuarios pueden editar directamente la cantidad vendida real de cualquier producto o anular la venta (restableciendo existencias a 0 ventas y devolviendo el stock de manera matemáticamente consistente: $L = E - M - F$).
- **Entrada Manual de Cantidades en Punto de Venta (POS):** Además de los botones `+` y `-`, los usuarios pueden tocar directamente la cantidad o el botón de edición para escribir cualquier cifra numérica (ej. 24, 50, 100) con teclado numérico y validación en tiempo real frente al stock.
- **Gestión de Ciclo de Vida de Catálogo:** Soporte completo para creación, edición, activación/desactivación y eliminación tanto de productos como de categorías con diálogos modales de confirmación preventiva.
- **Precisión Monetaria:** Enfoque en centavos (`Long`) para cálculo sin pérdida de precisión de punto flotante.
- **Separación de Lógica:** Las fórmulas matemáticas residen en un motor aislado (`SalesCalculator`), nunca en componentes de UI.

## 3. Fórmulas Consolidadas y Auditadas
- **Disponible:** `disponible = inventarioInicial + entradas` ($E = C + D$)
- **Vendido:** `vendido = disponible - inventarioFinal - roturas` ($F = E - L - M$)
- **Importe Venta:** `importeVenta = vendido * precioVenta` ($H = F \times G$)
- **Importe Costo:** `importeCosto = vendido * costoUnitario` ($J = F \times I$)
- **Utilidad:** `utilidad = importeVenta - importeCosto` ($K = H - J$)
- **Margen %:** `margen = (utilidadTotal / ventasTotales) * 100`

## 4. Estado de Fases y Funcionalidades
- [x] **FASE 0 — Análisis del Excel:** Entregado y documentado.
- [x] **FASE 1 — Proyecto Android & Clean Architecture:** Configurado con theme M3, `navigation-compose`, y soporte de tema claro/oscuro.
- [x] **FASE 2 — Base de Datos Room:** Entidades `Category`, `Product`, `DailySession`, `DailyInventory`, DAOs y Repository con precarga de datos de junio de 2026.
- [x] **FASE 3 — Motor de Cálculo & Pruebas Unitarias:** `SalesCalculator` con pruebas unitarias 100% aprobadas.
- [x] **FASE 4 — Módulo de Productos & Categorías:** CRUD completo (incluye creación, edición, activación y eliminación de productos y categorías con diálogos de confirmación).
- [x] **FASE 5 — Módulo de Inventario Diario:** Traspaso automático `Inicio[T] = Final[T-1]`, control de entradas y conteo físico con validación preventiva `Final + Roturas <= Disponible`.
- [x] **FASE 6 — Módulo de Ventas Rápidas:** POS ágil con carrito, cálculo de totales, botones incrementales y entrada manual directa de cantidad mediante teclado.
- [x] **FASE 7 — Cierre Diario:** Diálogo de confirmación, inmutabilidad de jornadas cerradas y resumen consolidado.
- [x] **FASE 8 — Dashboard & Métricas:** Tarjetas KPI ajustadas, alertas de stock bajo y gráficos de distribución por categoría.
- [x] **FASE 9 — Historial & Reportes:** Navegación por sesiones históricas, cuadrícula 2x2 sin desbordamientos, desglose por categoría y producto.
- [x] **FASE 10 — Importación de Datos Históricos:** Precarga de sesiones de junio (19-06, 20-06, 28-06) con continuidad exacta 1:1.
- [x] **FASE 11 — Exportación (CSV/Texto):** Generación de CSV estructurado con Share Intent de Android.
- [x] **FASE 12 — QA y Pruebas Globales:** Tests automatizados aprobados (`BUILD SUCCESSFUL`).
- [x] **Edición y Anulación de Ventas en Inventario:** Implementado con recálculo automático de stock e importes.
- [x] **Entrada Manual de Cantidades en Ventas:** Soporte de teclado numérico con vista previa de subtotales.
- [x] **Apertura y Cierre Dinámico en Dashboard:** Botones adaptativos en Dashboard para "Cerrar Jornada" (con diálogo informativo) o "Abrir Jornada (Hoy)" y "Elegir Fecha en Calendario" cuando no hay sesión abierta, más opción de reabrir.
- [x] **Creación Flexible de Jornadas (Hoy / Calendario):** Soporte en Dashboard e Inventario para abrir jornadas hoy o seleccionando en calendario M3 (`AppDatePickerDialog`).
- [x] **Filtro por Calendario en Historial y Reportes:** Selección de fecha en calendario para filtrar jornadas, con banner de filtro activo, chip rápido, botón para limpiar filtro y creación rápida en caso de no existir registros.
