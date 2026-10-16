# ARCHITECTURE — Documento de Arquitectura

## 1. Visión General
El proyecto sigue los principios de **Clean Architecture** adaptados a Android moderno con Jetpack Compose y Kotlin Coroutines/Flow.

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  Compose Screens (M3)  │  ViewModels  │  UI State / Events  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                          │
│   Use Cases / Calculators    │   Domain Models / Contracts  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        DATA LAYER                           │
│  Repository Implementations  │  Room Database & Entities    │
│  DAOs & Mappers              │  Local SQLite                │
└─────────────────────────────────────────────────────────────┘
```

## 2. Paquetes y Estructura
- `com.example.domain.calculator`: Motor matemático (`SalesCalculator`).
- `com.example.domain.model`: Modelos puros (`Product`, `Category`, `DailySession`, `DailyInventoryItem`, `DaySummary`).
- `com.example.domain.repository`: Interfaces del repositorio.
- `com.example.data.local.entity`: Entidades Room con claves foráneas e índices.
- `com.example.data.local.dao`: DAOs con consultas SQL reactivas (`Flow`).
- `com.example.data.local.AppDatabase`: Configuración de Room Database con TypeConverters.
- `com.example.data.repository`: Implementación del repositorio e integración de datos.
- `com.example.presentation`: Módulos por feature:
  - `navigation`: Destinos y BottomNavigationBar.
  - `dashboard`: Resumen de métricas del día activo y tendencias.
  - `session`: Gestión del inventario diario, entradas, conteo físico y cierre.
  - `quicksale`: Modo de registro de ventas directas.
  - `products`: Catálogo y administración de productos y categorías.
  - `history`: Consulta de sesiones pasadas y reportes.
- `com.example.ui.theme`: Temas, tipografía y paleta de colores Material 3.

## 3. Manejo de Errores e Integridad
- Validación preventiva antes de guardar cualquier movimiento.
- Mensajes de error amigables al usuario mediante Snackbars y feedback en campos de texto (`isError`).
- Inmutabilidad de sesiones marcadas como `CLOSED`.
