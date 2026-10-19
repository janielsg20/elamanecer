package com.example.presentation.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.icon.ProductIconView
import com.example.domain.model.Product
import com.example.domain.model.Project
import com.example.domain.model.Warehouse
import com.example.domain.model.WarehouseDispatch
import com.example.domain.model.WarehouseStockItem
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(
  viewModel: WarehouseViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by remember { mutableIntStateOf(0) }
  var showCreateWarehouseDialog by remember { mutableStateOf(false) }
  var warehouseToEdit by remember { mutableStateOf<Warehouse?>(null) }
  var warehouseToDelete by remember { mutableStateOf<Warehouse?>(null) }

  // Gestor de stock dialogs
  var showStockItemDialog by remember { mutableStateOf(false) }
  var stockItemToEdit by remember { mutableStateOf<WarehouseStockItem?>(null) }
  var stockItemForEntry by remember { mutableStateOf<WarehouseStockItem?>(null) }
  var stockItemForExit by remember { mutableStateOf<WarehouseStockItem?>(null) }
  var stockItemToDelete by remember { mutableStateOf<WarehouseStockItem?>(null) }
  var preselectedProductIdForDispatch by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Gestor de Inventario de Almacén",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = uiState.selectedWarehouse?.let { "${it.name} (${it.targetType})" }
                ?: "Control de Existencias e Importes",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showCreateWarehouseDialog = true },
            modifier = Modifier.testTag("warehouse_btn_create"),
          ) {
            Icon(Icons.Default.AddBusiness, contentDescription = "Nuevo Almacén", tint = MaterialTheme.colorScheme.primary)
          }
          ThemeToggleIconButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    floatingActionButton = {
      if (selectedTab == 0 && uiState.selectedWarehouse != null) {
        FloatingActionButton(
          onClick = {
            stockItemToEdit = null
            showStockItemDialog = true
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("warehouse_fab_add_product"),
        ) {
          Icon(Icons.Default.Add, contentDescription = "Nuevo Producto en Almacén")
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Selector de almacén horizontal
      if (uiState.warehouses.isNotEmpty()) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(uiState.warehouses) { warehouse ->
            val isSelected = uiState.selectedWarehouse?.id == warehouse.id
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.selectWarehouse(warehouse.id) },
              label = {
                Text(
                  text = warehouse.name,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Store,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              },
              modifier = Modifier.testTag("warehouse_chip_${warehouse.id}"),
            )
          }
        }
      }

      // Tab selector: Stock | Despacho | Historial
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 16.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Inventario de Almacén") },
          icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.testTag("tab_warehouse_stock"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Despachar a Local") },
          icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.testTag("tab_warehouse_dispatch"),
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Historial de Envíos") },
          icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.testTag("tab_warehouse_history"),
        )
      }

      // Tab contents
      when (selectedTab) {
        0 -> WarehouseStockTab(
          warehouse = uiState.selectedWarehouse,
          stockItems = uiState.filteredStockItems,
          searchQuery = uiState.searchQuery,
          onSearchChange = viewModel::setSearchQuery,
          onEditWarehouse = { warehouseToEdit = it },
          onDeleteWarehouse = { warehouseToDelete = it },
          onCreateProduct = {
            stockItemToEdit = null
            showStockItemDialog = true
          },
          onEditProduct = {
            stockItemToEdit = it
            showStockItemDialog = true
          },
          onAddEntry = { stockItemForEntry = it },
          onAddExit = { stockItemForExit = it },
          onDeleteStockItem = { stockItemToDelete = it },
          onGoToDispatch = { productId ->
            preselectedProductIdForDispatch = productId
            selectedTab = 1
          },
        )

        1 -> WarehouseDispatchTab(
          warehouses = uiState.warehouses,
          selectedWarehouse = uiState.selectedWarehouse,
          projects = uiState.projects,
          stockItems = uiState.stockItems,
          initialProductId = preselectedProductIdForDispatch,
          onSelectWarehouse = viewModel::selectWarehouse,
          onDispatch = { wId, projId, prodId, qty, notes ->
            viewModel.dispatchStock(wId, projId, prodId, qty, notes)
          },
        )

        2 -> WarehouseDispatchesHistoryTab(
          dispatches = uiState.dispatches,
        )
      }
    }
  }

  // Diálogos modales
  if (showCreateWarehouseDialog) {
    WarehouseFormDialog(
      warehouse = null,
      onDismiss = { showCreateWarehouseDialog = false },
      onConfirm = { name, type, loc, notes ->
        viewModel.createWarehouse(name, type, loc, notes)
        showCreateWarehouseDialog = false
      },
    )
  }

  warehouseToEdit?.let { wh ->
    WarehouseFormDialog(
      warehouse = wh,
      onDismiss = { warehouseToEdit = null },
      onConfirm = { name, type, loc, notes ->
        viewModel.updateWarehouse(wh.id, name, type, loc, notes)
        warehouseToEdit = null
      },
    )
  }

  warehouseToDelete?.let { wh ->
    AlertDialog(
      onDismissRequest = { warehouseToDelete = null },
      title = { Text("Eliminar Almacén") },
      text = { Text("¿Estás seguro de que deseas eliminar el almacén \"${wh.name}\"? Esta acción no se puede deshacer.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteWarehouse(wh.id)
            warehouseToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
        ) {
          Text("Eliminar")
        }
      },
      dismissButton = {
        TextButton(onClick = { warehouseToDelete = null }) {
          Text("Cancelar")
        }
      },
    )
  }

  // Diálogo para Crear / Editar Producto en Almacén (con los 8 campos)
  if (showStockItemDialog) {
    val currentWarehouseId = uiState.selectedWarehouse?.id ?: 1L
    WarehouseStockItemDialog(
      item = stockItemToEdit,
      catalogProducts = uiState.products,
      onDismiss = {
        showStockItemDialog = false
        stockItemToEdit = null
      },
      onConfirm = { prodId, name, unit, inicio, entrada, salida, costo ->
        viewModel.saveWarehouseStockItem(
          warehouseId = currentWarehouseId,
          productId = prodId,
          productName = name,
          unitOfMeasure = unit,
          initialStock = inicio,
          entries = entrada,
          exits = salida,
          costPrice = costo,
        )
        showStockItemDialog = false
        stockItemToEdit = null
      },
    )
  }

  // Diálogo para Entrada Rápida (+ Entrada)
  stockItemForEntry?.let { item ->
    QuickStockEntryDialog(
      item = item,
      onDismiss = { stockItemForEntry = null },
      onConfirm = { qty ->
        viewModel.addStockEntry(item.warehouseId, item.productId, qty)
        stockItemForEntry = null
      },
    )
  }

  // Diálogo para Salida Rápida (- Salida)
  stockItemForExit?.let { item ->
    QuickStockExitDialog(
      item = item,
      onDismiss = { stockItemForExit = null },
      onConfirm = { qty ->
        viewModel.addStockExit(item.warehouseId, item.productId, qty)
        stockItemForExit = null
      },
    )
  }

  // Diálogo para Eliminar Producto del Almacén
  stockItemToDelete?.let { item ->
    AlertDialog(
      onDismissRequest = { stockItemToDelete = null },
      title = { Text("Eliminar Producto de Almacén") },
      text = {
        Text("¿Deseas quitar \"${item.productName}\" de este almacén? El producto conservará su historial previo si existiese.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteWarehouseStockItem(item.warehouseId, item.productId)
            stockItemToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
        ) {
          Text("Eliminar")
        }
      },
      dismissButton = {
        TextButton(onClick = { stockItemToDelete = null }) {
          Text("Cancelar")
        }
      },
    )
  }
}

// -------------------------------------------------------------
// TAB 1: Stock en Almacén (Gestor de Inventario con 8 campos)
// -------------------------------------------------------------
@Composable
private fun WarehouseStockTab(
  warehouse: Warehouse?,
  stockItems: List<WarehouseStockItem>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onEditWarehouse: (Warehouse) -> Unit,
  onDeleteWarehouse: (Warehouse) -> Unit,
  onCreateProduct: () -> Unit,
  onEditProduct: (WarehouseStockItem) -> Unit,
  onAddEntry: (WarehouseStockItem) -> Unit,
  onAddExit: (WarehouseStockItem) -> Unit,
  onDeleteStockItem: (WarehouseStockItem) -> Unit,
  onGoToDispatch: (productId: Long?) -> Unit,
) {
  if (warehouse == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No hay almacenes configurados. Crea uno con el botón superior.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  // Cálculos globales para el almacén
  val totalImporte = stockItems.sumOf { item ->
    val available = item.initialStock + item.entries - item.exits
    val cost = if (item.costPrice > 0.0) item.costPrice else (item.costPriceCents / 100.0)
    available * cost
  }
  val totalDisponibles = stockItems.sumOf { it.initialStock + it.entries - it.exits }
  val totalEntradas = stockItems.sumOf { it.entries }
  val totalSalidas = stockItems.sumOf { it.exits }
  val totalInicio = stockItems.sumOf { it.initialStock }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    // Tarjeta del Almacén Seleccionado
    item {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = warehouse.name,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                  Text(
                    text = warehouse.targetType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }

              if (warehouse.location.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(top = 4.dp),
                ) {
                  Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = warehouse.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Row {
              IconButton(onClick = { onEditWarehouse(warehouse) }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Almacén", tint = MaterialTheme.colorScheme.primary)
              }
              IconButton(onClick = { onDeleteWarehouse(warehouse) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar Almacén", tint = DangerRed)
              }
            }
          }

          if (warehouse.notes.isNotBlank()) {
            Text(
              text = warehouse.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Métricas de cabecera rápida
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StockSummaryChip(
              title = "Disponible",
              value = String.format("%,.0f", totalDisponibles),
              modifier = Modifier.weight(1f),
            )
            StockSummaryChip(
              title = "Productos",
              value = stockItems.size.toString(),
              modifier = Modifier.weight(1f),
            )
            StockSummaryChip(
              title = "Total Importe",
              value = WarehouseStockItem.formatMoneyDouble(totalImporte),
              color = SuccessGreen,
              modifier = Modifier.weight(1.3f),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Acciones principales del almacén
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = onCreateProduct,
              modifier = Modifier
                .weight(1f)
                .testTag("btn_add_warehouse_product"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Nuevo Producto", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
              onClick = { onGoToDispatch(null) },
              modifier = Modifier
                .weight(1f)
                .testTag("warehouse_btn_quick_dispatch"),
            ) {
              Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Despachar a Local", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    // Buscador
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Buscar por producto o categoría...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("warehouse_search_input"),
      )
    }

    // Lista de productos en almacén
    if (stockItems.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              Icons.Default.Inventory2,
              contentDescription = null,
              modifier = Modifier.size(40.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No hay productos coincidentes con \"$searchQuery\""
              else "No hay stock registrado aún en este almacén",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCreateProduct) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Agregar Primer Producto")
            }
          }
        }
      }
    } else {
      items(stockItems, key = { it.id }) { item ->
        WarehouseStockItemCard(
          item = item,
          onEdit = { onEditProduct(item) },
          onAddEntry = { onAddEntry(item) },
          onAddExit = { onAddExit(item) },
          onDelete = { onDeleteStockItem(item) },
          onDispatch = { onGoToDispatch(item.productId) },
        )
      }

      // ---------------------------------------------------------
      // TOTAL DE IMPORTE AL FINAL (REQUISITO EXPLÍCITO)
      // ---------------------------------------------------------
      item {
        WarehouseTotalSummaryCard(
          totalImporte = totalImporte,
          totalInicio = totalInicio,
          totalEntradas = totalEntradas,
          totalSalidas = totalSalidas,
          totalDisponibles = totalDisponibles,
          itemCount = stockItems.size,
        )
      }
    }
  }
}

@Composable
private fun StockSummaryChip(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

// -------------------------------------------------------------
// TARJETA DE CADA PRODUCTO EN ALMACÉN (CON LOS 8 CAMPOS)
// - Producto (texto)
// - Unidad (texto)
// - Inicio (número)
// - entrada (número)
// - salida (número)
// - disponible (número = inicio + entrada - salida)
// - costo (número)
// - importe (número = disponible * costo)
// -------------------------------------------------------------
@Composable
private fun WarehouseStockItemCard(
  item: WarehouseStockItem,
  onEdit: () -> Unit,
  onAddEntry: () -> Unit,
  onAddExit: () -> Unit,
  onDelete: () -> Unit,
  onDispatch: () -> Unit,
) {
  val disponible = item.initialStock + item.entries - item.exits
  val costo = if (item.costPrice > 0.0) item.costPrice else (item.costPriceCents / 100.0)
  val importe = disponible * costo
  val isLow = disponible <= item.minAlertStock
  val isOut = disponible <= 0.0

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("warehouse_stock_item_${item.productId}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isOut) DangerRed.copy(alpha = 0.05f)
      else if (isLow) WarningOrange.copy(alpha = 0.05f)
      else MaterialTheme.colorScheme.surface,
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      // 1. Fila de Encabezado: Icono + Producto (texto) + Unidad (texto) + Acciones
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ProductIconView(
          productName = item.productName,
          categoryName = item.categoryName,
          size = 40.dp,
          iconSize = 22.dp,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          // Campo: Producto
          Text(
            text = item.productName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          // Campo: Unidad
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "Unidad: ${item.unitOfMeasure}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            if (item.categoryName.isNotBlank()) {
              Text(
                text = "• ${item.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Acciones: Editar y Eliminar
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar Producto", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar Producto", tint = DangerRed)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Cuadrícula de Campos de Cantidad: Inicio | Entrada | Salida | Disponible
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Campo: Inicio
        MetricColumnBox(
          label = "Inicio",
          value = String.format("%,.0f", item.initialStock),
          unit = item.unitOfMeasure,
          modifier = Modifier.weight(1f),
        )

        // Campo: Entrada
        MetricColumnBox(
          label = "Entrada",
          value = "+${String.format("%,.0f", item.entries)}",
          unit = item.unitOfMeasure,
          textColor = SuccessGreen,
          modifier = Modifier.weight(1f),
        )

        // Campo: Salida
        MetricColumnBox(
          label = "Salida",
          value = "-${String.format("%,.0f", item.exits)}",
          unit = item.unitOfMeasure,
          textColor = if (item.exits > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )

        // Campo: Disponible (Fórmula: inicio + entrada - salida)
        MetricColumnBox(
          label = "Disponible",
          value = String.format("%,.0f", disponible),
          unit = item.unitOfMeasure,
          textColor = if (isOut) DangerRed else if (isLow) WarningOrange else SuccessGreen,
          isHighlighted = true,
          modifier = Modifier.weight(1.2f),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Fila de Campos Financieros: Costo | Importe (disponible * costo)
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Campo: Costo
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Costo: ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = WarehouseStockItem.formatMoneyDouble(costo),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          // Campo: Importe (disponible * costo)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Importe: ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = WarehouseStockItem.formatMoneyDouble(importe),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 4. Botones Operativos Rápidos
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OutlinedButton(
          onClick = onAddEntry,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(32.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Entrada", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onAddExit,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(32.dp),
        ) {
          Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("- Salida", style = MaterialTheme.typography.labelSmall)
        }

        Button(
          onClick = onDispatch,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          modifier = Modifier.weight(1.2f).height(32.dp),
        ) {
          Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Despachar", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun MetricColumnBox(
  label: String,
  value: String,
  unit: String,
  modifier: Modifier = Modifier,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  isHighlighted: Boolean = false,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold),
        color = textColor,
        maxLines = 1,
      )
      Text(
        text = unit,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

// -------------------------------------------------------------
// TOTAL DE IMPORTE AL FINAL (TARJETA PROMINENTE DE CIERRE)
// -------------------------------------------------------------
@Composable
private fun WarehouseTotalSummaryCard(
  totalImporte: Double,
  totalInicio: Double,
  totalEntradas: Double,
  totalSalidas: Double,
  totalDisponibles: Double,
  itemCount: Int,
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 24.dp)
      .testTag("warehouse_total_summary_card"),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    ),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Calculate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Total de Importe en Almacén",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primary,
        ) {
          Text(
            text = "$itemCount productos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Importe total destacado
      Text(
        text = WarehouseStockItem.formatMoneyDouble(totalImporte),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("warehouse_total_importe_value"),
      )

      Text(
        text = "Valor monetario consolidado: Disponible total × Costos unitarios",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Desglose de unidades
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Inicio Acumulado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("%,.0f", totalInicio), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Entradas (+)", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
            Text(String.format("%,.0f", totalEntradas), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Salidas (-)", style = MaterialTheme.typography.labelSmall, color = DangerRed)
            Text(String.format("%,.0f", totalSalidas), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = DangerRed))
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1.1f),
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Disponible Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(String.format("%,.0f", totalDisponibles), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 2: Despachar a Local (Envío de Stock a Locales)
// -------------------------------------------------------------
@Composable
private fun WarehouseDispatchTab(
  warehouses: List<Warehouse>,
  selectedWarehouse: Warehouse?,
  projects: List<Project>,
  stockItems: List<WarehouseStockItem>,
  initialProductId: Long?,
  onSelectWarehouse: (Long) -> Unit,
  onDispatch: (warehouseId: Long, projectId: Long, productId: Long, quantity: Double, notes: String) -> Unit,
) {
  var selectedWarehouseId by remember(selectedWarehouse) {
    mutableStateOf(selectedWarehouse?.id ?: warehouses.firstOrNull()?.id ?: 1L)
  }
  var selectedProjectId by remember(projects) {
    mutableStateOf(projects.firstOrNull()?.id ?: 1L)
  }
  var selectedProductId by remember(stockItems, initialProductId) {
    val initial = initialProductId?.takeIf { pId -> stockItems.any { it.productId == pId } }
    mutableStateOf(initial ?: stockItems.firstOrNull()?.productId ?: 0L)
  }
  var quantityText by remember { mutableStateOf("45") }
  var notesText by remember { mutableStateOf("") }

  val currentItem = stockItems.find { it.productId == selectedProductId }
  val currentAvailableStock = currentItem?.let { it.initialStock + it.entries - it.exits } ?: 0.0
  val quantityToDispatch = quantityText.toDoubleOrNull() ?: 0.0
  val remainingStock = currentAvailableStock - quantityToDispatch
  val isExceeding = quantityToDispatch > currentAvailableStock
  val isValid = quantityToDispatch > 0 && !isExceeding && selectedProductId > 0

  val targetProject = projects.find { it.id == selectedProjectId }
  val originWarehouse = warehouses.find { it.id == selectedWarehouseId }
  val costUnit = currentItem?.let { if (it.costPrice > 0.0) it.costPrice else it.costPriceCents / 100.0 } ?: 0.0
  val dispatchAmount = quantityToDispatch * costUnit

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Envío y Despacho de Stock a Locales",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "Transfiere mercadería desde los almacenes centrales a los inventarios de venta de cada local. Si el producto no existe en el local, se creará automáticamente en su stock.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(16.dp))

          // 1. Almacén de Origen
          Text(
            text = "1. Almacén de Origen",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(warehouses) { wh ->
              val isSel = wh.id == selectedWarehouseId
              FilterChip(
                selected = isSel,
                onClick = {
                  selectedWarehouseId = wh.id
                  onSelectWarehouse(wh.id)
                },
                label = { Text(wh.name) },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp)) },
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 2. Local de Destino
          Text(
            text = "2. Local de Destino",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects) { proj ->
              val isSel = proj.id == selectedProjectId
              FilterChip(
                selected = isSel,
                onClick = { selectedProjectId = proj.id },
                label = { Text("${proj.name} (${proj.type})") },
                leadingIcon = {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(
                        try {
                          Color(android.graphics.Color.parseColor(proj.colorHex))
                        } catch (_: Exception) {
                          MaterialTheme.colorScheme.primary
                        },
                      ),
                  )
                },
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 3. Producto a Despachar
          Text(
            text = "3. Producto a Despachar",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.height(6.dp))
          if (stockItems.isEmpty()) {
            Text(
              text = "No hay productos con stock en este almacén",
              style = MaterialTheme.typography.bodySmall,
              color = DangerRed,
            )
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(stockItems) { stockItem ->
                val isSel = stockItem.productId == selectedProductId
                val available = stockItem.initialStock + stockItem.entries - stockItem.exits
                FilterChip(
                  selected = isSel,
                  onClick = { selectedProductId = stockItem.productId },
                  label = {
                    Text("${stockItem.productName} (${available.toInt()} ${stockItem.unitOfMeasure})")
                  },
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 4. Cantidad a Despachar (Salida del almacén)
          Text(
            text = "4. Cantidad de Salida",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeding,
            supportingText = {
              if (isExceeding) {
                Text("La salida no puede superar el stock disponible ($currentAvailableStock)", color = DangerRed)
              }
            },
            trailingIcon = {
              Text(
                text = currentItem?.unitOfMeasure ?: "U",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dispatch_quantity_input"),
          )

          // Presets rápidos
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(12, 24, 45, 50, 100).forEach { preset ->
              OutlinedButton(
                onClick = { quantityText = preset.toString() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
              ) {
                Text("$preset", style = MaterialTheme.typography.labelSmall)
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 5. Notas opcionales
          OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = { Text("Notas de salida (ej. Despacho matutino)") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Tarjeta de Previsualización y Confirmación en Tiempo Real
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (isExceeding) DangerRed.copy(alpha = 0.12f)
              else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ),
            shape = RoundedCornerShape(12.dp),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Resumen del Despacho",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Stock actual en almacén:", style = MaterialTheme.typography.bodySmall)
                Text("${currentAvailableStock.toInt()} ${currentItem?.unitOfMeasure ?: "U"}", fontWeight = FontWeight.SemiBold)
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Cantidad de salida:", style = MaterialTheme.typography.bodySmall)
                Text(
                  "-${quantityToDispatch.toInt()} ${currentItem?.unitOfMeasure ?: "U"}",
                  fontWeight = FontWeight.Bold,
                  color = DangerRed,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Quedan en almacén:", style = MaterialTheme.typography.bodySmall)
                Text(
                  "${remainingStock.toInt()} ${currentItem?.unitOfMeasure ?: "U"}",
                  fontWeight = FontWeight.Bold,
                  color = if (remainingStock >= 0) SuccessGreen else DangerRed,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Importe del despacho:", style = MaterialTheme.typography.bodySmall)
                Text(
                  WarehouseStockItem.formatMoneyDouble(dispatchAmount),
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Nota de creación automática en el local
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Se agregarán ${quantityToDispatch.toInt()} unidades a \"${targetProject?.name ?: "Local"}\". Si el producto no estaba en este local, se creará automáticamente en su stock.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Botón final de despacho
          Button(
            onClick = {
              if (isValid) {
                onDispatch(selectedWarehouseId, selectedProjectId, selectedProductId, quantityToDispatch, notesText)
                notesText = ""
              }
            },
            enabled = isValid,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("dispatch_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          ) {
            Icon(Icons.Default.LocalShipping, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirmar y Despachar al Local")
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 3: Historial de Envíos / Despachos
// -------------------------------------------------------------
@Composable
private fun WarehouseDispatchesHistoryTab(
  dispatches: List<WarehouseDispatch>,
) {
  if (dispatches.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No hay registros de despachos aún.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    items(dispatches, key = { it.id }) { dispatch ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dispatch_item_${dispatch.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = dispatch.warehouseName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = dispatch.projectName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
              )
            }

            Text(
              text = dispatch.dateString,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Detalles de la salida
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ProductIconView(
              productName = dispatch.productName,
              size = 36.dp,
              iconSize = 20.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = dispatch.productName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              )
              Row {
                Text(
                  text = "Salida: ${dispatch.quantity.toInt()} ${dispatch.unitOfMeasure}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = DangerRed,
                )
                Text(
                  text = " • Quedan: ${dispatch.remainingStockInWarehouse.toInt()} ${dispatch.unitOfMeasure}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (dispatch.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Nota: ${dispatch.notes}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// DIÁLOGO: CREAR / EDITAR PRODUCTO EN ALMACÉN (8 CAMPOS)
// - Producto (texto)
// - Unidad (texto)
// - Inicio (número)
// - entrada (número)
// - salida (número)
// - disponible (número, calculado: inicio + entrada - salida)
// - costo (número)
// - importe (número, calculado: disponible * costo)
// -------------------------------------------------------------
@Composable
private fun WarehouseStockItemDialog(
  item: WarehouseStockItem?,
  catalogProducts: List<Product>,
  onDismiss: () -> Unit,
  onConfirm: (
    productId: Long?,
    productName: String,
    unitOfMeasure: String,
    initialStock: Double,
    entries: Double,
    exits: Double,
    costPrice: Double,
  ) -> Unit,
) {
  var productName by remember { mutableStateOf(item?.productName ?: "") }
  var unitOfMeasure by remember { mutableStateOf(item?.unitOfMeasure ?: "U") }
  var initialStockText by remember { mutableStateOf(item?.let { String.format(java.util.Locale.US, "%.0f", it.initialStock) } ?: "0") }
  var entriesText by remember { mutableStateOf(item?.let { String.format(java.util.Locale.US, "%.0f", it.entries) } ?: "0") }
  var exitsText by remember { mutableStateOf(item?.let { String.format(java.util.Locale.US, "%.0f", it.exits) } ?: "0") }
  var costText by remember {
    val cost = item?.let { if (it.costPrice > 0.0) it.costPrice else it.costPriceCents / 100.0 } ?: 0.0
    mutableStateOf(if (cost > 0.0) String.format(java.util.Locale.US, "%.2f", cost) else "")
  }

  // Cálculos dinámicos en tiempo real
  val inicioVal = initialStockText.toDoubleOrNull() ?: 0.0
  val entradaVal = entriesText.toDoubleOrNull() ?: 0.0
  val salidaVal = exitsText.toDoubleOrNull() ?: 0.0
  val costoVal = costText.toDoubleOrNull() ?: 0.0

  val disponibleCalculado = inicioVal + entradaVal - salidaVal
  val importeCalculado = disponibleCalculado * costoVal
  val isDisponibleNegative = disponibleCalculado < 0
  val isFormValid = productName.isNotBlank() && unitOfMeasure.isNotBlank() &&
    inicioVal >= 0 && entradaVal >= 0 && salidaVal >= 0 && costoVal >= 0 && !isDisponibleNegative

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      ProductIconView(
        productName = productName.ifBlank { "Producto" },
        size = 46.dp,
        iconSize = 26.dp,
      )
    },
    title = {
      Text(if (item == null) "Nuevo Producto en Almacén" else "Editar Producto en Almacén")
    },
    text = {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        // 1. Campo: Producto (campo de texto)
        item {
          OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Producto (Nombre) *") },
            placeholder = { Text("ej. Cerveza Cristal, Refresco TuKola") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_product_name"),
          )
        }

        // Sugerencias rápidas de productos existentes del catálogo
        if (item == null && catalogProducts.isNotEmpty()) {
          item {
            Text("O seleccionar del catálogo:", style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(catalogProducts.take(6)) { catProd ->
                FilterChip(
                  selected = productName.equals(catProd.name, ignoreCase = true),
                  onClick = {
                    productName = catProd.name
                    unitOfMeasure = catProd.unitOfMeasure
                    costText = String.format(java.util.Locale.US, "%.2f", catProd.costPriceCents / 100.0)
                  },
                  label = { Text(catProd.name, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
        }

        // 2. Campo: Unidad (campo de texto)
        item {
          OutlinedTextField(
            value = unitOfMeasure,
            onValueChange = { unitOfMeasure = it },
            label = { Text("Unidad de Medida *") },
            placeholder = { Text("ej. U, Botella, Lata, Caja, Kg") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_unit_input"),
          )

          // Chips rápidos de unidades
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("U", "Botella", "Lata", "Caja", "Kg").forEach { unit ->
              FilterChip(
                selected = unitOfMeasure.equals(unit, ignoreCase = true),
                onClick = { unitOfMeasure = unit },
                label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }

        // 3. Campo: Inicio (campo de números)
        item {
          OutlinedTextField(
            value = initialStockText,
            onValueChange = { initialStockText = it },
            label = { Text("Inicio (Stock Inicial) *") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_initial_input"),
          )
        }

        // 4. Campo: Entrada (campo de números)
        item {
          OutlinedTextField(
            value = entriesText,
            onValueChange = { entriesText = it },
            label = { Text("Entrada (Reabastecimientos) *") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_entries_input"),
          )
        }

        // 5. Campo: Salida (campo de números)
        item {
          OutlinedTextField(
            value = exitsText,
            onValueChange = { exitsText = it },
            label = { Text("Salida (Despachos / Consumos) *") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_exits_input"),
          )
        }

        // 6. Campo: Costo (campo de números)
        item {
          OutlinedTextField(
            value = costText,
            onValueChange = { costText = it },
            label = { Text("Costo Unitario ($) *") },
            placeholder = { Text("ej. 4.50") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("stock_dialog_cost_input"),
          )
        }

        // 7 y 8. PANEL DE CÁLCULO EN TIEMPO REAL: DISPONIBLE E IMPORTE
        item {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDisponibleNegative) DangerRed.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Fórmulas Calculadas en Tiempo Real:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(4.dp))

              // Fórmula 1: disponible = inicio + entrada - salida
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Disponible (inicio + entrada - salida):", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = "${String.format("%,.1f", disponibleCalculado)} $unitOfMeasure",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isDisponibleNegative) DangerRed else SuccessGreen,
                )
              }

              Spacer(modifier = Modifier.height(4.dp))

              // Fórmula 2: importe = disponibilidad * costo
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Importe (disponible × costo):", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = WarehouseStockItem.formatMoneyDouble(importeCalculado),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              if (isDisponibleNegative) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Error: La salida no puede ser mayor que inicio + entrada.",
                  style = MaterialTheme.typography.labelSmall,
                  color = DangerRed,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (isFormValid) {
            onConfirm(
              item?.productId,
              productName.trim(),
              unitOfMeasure.trim(),
              inicioVal,
              entradaVal,
              salidaVal,
              costoVal,
            )
          }
        },
        enabled = isFormValid,
        modifier = Modifier.testTag("stock_dialog_confirm_button"),
      ) {
        Text("Guardar Producto")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}

// -------------------------------------------------------------
// DIÁLOGO: ENTRADA RÁPIDA (+ Entrada)
// -------------------------------------------------------------
@Composable
private fun QuickStockEntryDialog(
  item: WarehouseStockItem,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Double) -> Unit,
) {
  var quantityText by remember { mutableStateOf("10") }
  val quantity = quantityText.toDoubleOrNull() ?: 0.0
  val currentAvailable = item.initialStock + item.entries - item.exits
  val newAvailable = currentAvailable + quantity
  val cost = if (item.costPrice > 0.0) item.costPrice else item.costPriceCents / 100.0
  val newImporte = newAvailable * cost

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Registrar Entrada de Mercadería") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Producto: ${item.productName}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "Disponible actual: ${currentAvailable.toInt()} ${item.unitOfMeasure}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it },
          label = { Text("Cantidad a Añadir a Entradas *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Nuevo disponible: ${newAvailable.toInt()} ${item.unitOfMeasure}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen,
            )
            Text(
              text = "Nuevo importe: ${WarehouseStockItem.formatMoneyDouble(newImporte)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (quantity > 0) onConfirm(quantity) },
        enabled = quantity > 0,
      ) {
        Text("Añadir Entrada")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}

// -------------------------------------------------------------
// DIÁLOGO: SALIDA RÁPIDA (- Salida)
// -------------------------------------------------------------
@Composable
private fun QuickStockExitDialog(
  item: WarehouseStockItem,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Double) -> Unit,
) {
  var quantityText by remember { mutableStateOf("10") }
  val quantity = quantityText.toDoubleOrNull() ?: 0.0
  val currentAvailable = item.initialStock + item.entries - item.exits
  val newAvailable = currentAvailable - quantity
  val isExceeding = quantity > currentAvailable
  val cost = if (item.costPrice > 0.0) item.costPrice else item.costPriceCents / 100.0
  val newImporte = (if (newAvailable > 0) newAvailable else 0.0) * cost

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Registrar Salida de Mercadería") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Producto: ${item.productName}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "Disponible actual: ${currentAvailable.toInt()} ${item.unitOfMeasure}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it },
          label = { Text("Cantidad a Deducir en Salidas *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = isExceeding,
          supportingText = {
            if (isExceeding) {
              Text("No puedes salir más unidades que el disponible ($currentAvailable)", color = DangerRed)
            }
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isExceeding) DangerRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Nuevo disponible: ${newAvailable.toInt()} ${item.unitOfMeasure}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isExceeding) DangerRed else MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Nuevo importe: ${WarehouseStockItem.formatMoneyDouble(newImporte)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (quantity > 0 && !isExceeding) onConfirm(quantity) },
        enabled = quantity > 0 && !isExceeding,
        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
      ) {
        Text("Confirmar Salida")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}

// -------------------------------------------------------------
// DIÁLOGO: CREAR / EDITAR ALMACÉN
// -------------------------------------------------------------
@Composable
private fun WarehouseFormDialog(
  warehouse: Warehouse?,
  onDismiss: () -> Unit,
  onConfirm: (name: String, targetType: String, location: String, notes: String) -> Unit,
) {
  var name by remember { mutableStateOf(warehouse?.name ?: "") }
  var targetType by remember { mutableStateOf(warehouse?.targetType ?: "GENERAL") }
  var location by remember { mutableStateOf(warehouse?.location ?: "") }
  var notes by remember { mutableStateOf(warehouse?.notes ?: "") }

  val types = listOf("KIOSKOS", "BAR", "PALADAR", "GENERAL")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (warehouse == null) "Nuevo Almacén" else "Editar Almacén") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre del Almacén *") },
          placeholder = { Text("ej. Almacén Central Kioskos") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("warehouse_dialog_name_input"),
        )

        Text("Tipo o Destino Principal:", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          types.forEach { type ->
            FilterChip(
              selected = targetType == type,
              onClick = { targetType = type },
              label = { Text(type, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Ubicación física") },
          placeholder = { Text("ej. Nave A, Bodega 2, Cuarto Frío") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas o Descripción") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onConfirm(name, targetType, location, notes) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("warehouse_dialog_confirm_button"),
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}
