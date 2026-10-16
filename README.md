# Control de Ventas — El Amanecer

Sistema profesional y nativo en Android para el control y análisis de inventario, ventas diarias, utilidades y cierres de jornada, construido con Kotlin y Jetpack Compose bajo Clean Architecture.

---

## 🚀 Características Principales

1. **Dashboard & Métricas en Tiempo Real**:
   - Resumen del día: ventas brutas, costo total, utilidad neta y porcentaje de margen.
   - Estado de la jornada (Abierta / Cerrada) con opciones de cierre y apertura rápida (Hoy o selección en calendario).
   - Alertas de inventario bajo y desglose por categoría.

2. **Inventario Diario & Entradas**:
   - Traspaso automático de stock: `Inicio[T] = Final[T-1]`.
   - Registro de entradas y conteo físico final con validación preventiva (`Final + Roturas <= Disponible`).
   - Corrección y anulación de ventas directamente desde la vista de inventario.

3. **Punto de Venta / Ventas Rápidas (POS)**:
   - Carrito de compras interactivo con catálogo filtrable por categoría y búsqueda en tiempo real.
   - Ajuste de cantidades con botones `+` / `-` y entrada numérica directa con teclado.
   - Cálculo automático de subtotales, totales y registro instantáneo en la jornada activa.

4. **Catálogo de Productos & Categorías**:
   - CRUD completo con modales de confirmación para eliminar o editar.
   - Precios de venta, costo unitario, unidades de medida y estado activo/inactivo.

5. **Historial & Reportes**:
   - Visualización y consulta de cierres diarios anteriores.
   - Filtro interactivo por fecha mediante DatePicker de Material 3.
   - Desglose detallado por categoría y producto.
   - Exportación de resumen y datos en formato CSV mediante Android Share Intent.

6. **Experiencia de Usuario & Diseño**:
   - Material Design 3 con soporte completo de Tema Claro y Tema Oscuro con selector instantáneo.
   - Icono adaptativo personalizado con branding de "El Amanecer".

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Persistencia:** Android Room (SQLite local con eliminación en cascada y flujos reactivos `Flow`)
- **Arquitectura:** Clean Architecture (Domain, Data, Presentation) + MVVM
- **Pruebas:** JUnit + Robolectric para pruebas unitarias y de integración del motor matemático `SalesCalculator`
