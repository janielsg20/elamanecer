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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
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
  var stockItemToAdd by remember { mutableStateOf<WarehouseStockItem?>(null) }
  var stockItemToAdjust by remember { mutableStateOf<WarehouseStockItem?>(null) }

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
              text = "Gestión de Almacenes",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = uiState.selectedWarehouse?.let { "${it.name} (${it.targetType})" }
                ?: "Control y Despachos Centrales",
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
            val firstProd = uiState.products.firstOrNull()
            if (firstProd != null) {
              val existing = uiState.stockItems.find { it.productId == firstProd.id }
              if (existing != null) {
                stockItemToAdd = existing
              } else {
                stockItemToAdd = WarehouseStockItem(
                  warehouseId = uiState.selectedWarehouse!!.id,
                  productId = firstProd.id,
                  productName = firstProd.name,
                  categoryName = firstProd.categoryName,
                  unitOfMeasure = firstProd.unitOfMeasure,
                  currentStock = 0.0,
                )
              }
            }
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("warehouse_fab_add_stock"),
        ) {
          Icon(Icons.Default.Add, contentDescription = "Añadir Stock al Almacén")
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
          text = { Text("Stock en Almacén") },
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
          onAddStock = { stockItemToAdd = it },
          onAdjustStock = { stockItemToAdjust = it },
          onGoToDispatch = { selectedTab = 1 },
        )

        1 -> WarehouseDispatchTab(
          warehouses = uiState.warehouses,
          selectedWarehouse = uiState.selectedWarehouse,
          projects = uiState.projects,
          stockItems = uiState.stockItems,
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

  stockItemToAdd?.let { item ->
    AddStockDialog(
      item = item,
      products = uiState.products,
      onDismiss = { stockItemToAdd = null },
      onConfirm = { prodId, qty, minAlert ->
        val wId = uiState.selectedWarehouse?.id ?: 1L
        viewModel.addStock(wId, prodId, qty, minAlert)
        stockItemToAdd = null
      },
    )
  }

  stockItemToAdjust?.let { item ->
    AdjustStockDialog(
      item = item,
      onDismiss = { stockItemToAdjust = null },
      onConfirm = { newStock, minAlert ->
        val wId = uiState.selectedWarehouse?.id ?: 1L
        viewModel.updateStock(wId, item.productId, newStock, minAlert)
        stockItemToAdjust = null
      },
    )
  }
}

// -------------------------------------------------------------
// TAB 1: Stock en Almacén
// -------------------------------------------------------------
@Composable
private fun WarehouseStockTab(
  warehouse: Warehouse?,
  stockItems: List<WarehouseStockItem>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onEditWarehouse: (Warehouse) -> Unit,
  onDeleteWarehouse: (Warehouse) -> Unit,
  onAddStock: (WarehouseStockItem) -> Unit,
  onAdjustStock: (WarehouseStockItem) -> Unit,
  onGoToDispatch: () -> Unit,
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

          // Métricas del almacén
          val totalStockUnits = stockItems.sumOf { it.currentStock }
          val lowStockCount = stockItems.count { it.currentStock <= it.minAlertStock }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StockSummaryChip(
              title = "Total Unidades",
              value = String.format("%,.0f", totalStockUnits),
              modifier = Modifier.weight(1f),
            )
            StockSummaryChip(
              title = "Productos",
              value = stockItems.size.toString(),
              modifier = Modifier.weight(1f),
            )
            StockSummaryChip(
              title = "Stock Bajo",
              value = lowStockCount.toString(),
              color = if (lowStockCount > 0) WarningOrange else SuccessGreen,
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Botón directo a Despachar
          Button(
            onClick = onGoToDispatch,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("warehouse_btn_quick_dispatch"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          ) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Despachar Productos a Proyecto")
          }
        }
      }
    }

    // Buscador
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Buscar producto en este almacén...") },
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
          }
        }
      }
    } else {
      items(stockItems, key = { it.id }) { item ->
        WarehouseStockItemCard(
          item = item,
          onAddStock = { onAddStock(item) },
          onAdjustStock = { onAdjustStock(item) },
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
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
      )
    }
  }
}

@Composable
private fun WarehouseStockItemCard(
  item: WarehouseStockItem,
  onAddStock: () -> Unit,
  onAdjustStock: () -> Unit,
) {
  val isLow = item.currentStock <= item.minAlertStock
  val isOut = item.currentStock <= 0.0

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("warehouse_stock_item_${item.productId}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isOut) DangerRed.copy(alpha = 0.08f)
      else if (isLow) WarningOrange.copy(alpha = 0.08f)
      else MaterialTheme.colorScheme.surface,
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProductIconView(
        productName = item.productName,
        categoryName = item.categoryName,
        size = 44.dp,
        iconSize = 24.dp,
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.productName,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.categoryName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = " • Min: ${item.minAlertStock.toInt()} ${item.unitOfMeasure}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stock disponible destacado
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Stock disponible: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${String.format("%,.0f", item.currentStock)} ${item.unitOfMeasure}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isOut) DangerRed else if (isLow) WarningOrange else SuccessGreen,
          )
        }
      }

      // Botones de acción rápida
      Column(horizontalAlignment = Alignment.End) {
        OutlinedButton(
          onClick = onAddStock,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Entrada", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
          onClick = onAdjustStock,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
        ) {
          Text("Ajustar", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 2: Despachar a Local (Envío de Stock)
// -------------------------------------------------------------
@Composable
private fun WarehouseDispatchTab(
  warehouses: List<Warehouse>,
  selectedWarehouse: Warehouse?,
  projects: List<Project>,
  stockItems: List<WarehouseStockItem>,
  onSelectWarehouse: (Long) -> Unit,
  onDispatch: (warehouseId: Long, projectId: Long, productId: Long, quantity: Double, notes: String) -> Unit,
) {
  var selectedWarehouseId by remember(selectedWarehouse) {
    mutableStateOf(selectedWarehouse?.id ?: warehouses.firstOrNull()?.id ?: 1L)
  }
  var selectedProjectId by remember(projects) {
    mutableStateOf(projects.firstOrNull()?.id ?: 1L)
  }
  var selectedProductId by remember(stockItems) {
    mutableStateOf(stockItems.firstOrNull()?.productId ?: 0L)
  }
  var quantityText by remember { mutableStateOf("45") }
  var notesText by remember { mutableStateOf("") }

  val currentItem = stockItems.find { it.productId == selectedProductId }
  val currentAvailableStock = currentItem?.currentStock ?: 0.0
  val quantityToDispatch = quantityText.toDoubleOrNull() ?: 0.0
  val remainingStock = currentAvailableStock - quantityToDispatch
  val isExceeding = quantityToDispatch > currentAvailableStock
  val isValid = quantityToDispatch > 0 && !isExceeding && selectedProductId > 0

  val targetProject = projects.find { it.id == selectedProjectId }
  val originWarehouse = warehouses.find { it.id == selectedWarehouseId }

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
            text = "Envío y Despacho de Productos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "Envía mercadería desde un almacén directamente al stock disponible del local seleccionado.",
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
                        }
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
                FilterChip(
                  selected = isSel,
                  onClick = { selectedProductId = stockItem.productId },
                  label = {
                    Text("${stockItem.productName} (${stockItem.currentStock.toInt()} ${stockItem.unitOfMeasure})")
                  },
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 4. Cantidad a Despachar
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

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Se añadirán ${quantityToDispatch.toInt()} unidades a las entradas de hoy en \"${targetProject?.name ?: "Local"}\".",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
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
// DIÁLOGOS
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
private fun AddStockDialog(
  item: WarehouseStockItem,
  products: List<Product>,
  onDismiss: () -> Unit,
  onConfirm: (productId: Long, quantity: Double, minAlert: Double?) -> Unit,
) {
  var selectedProdId by remember { mutableStateOf(item.productId) }
  var quantityText by remember { mutableStateOf("50") }
  var minAlertText by remember { mutableStateOf(item.minAlertStock.toInt().toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Añadir Stock a Almacén") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Producto: ${item.productName}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "Stock actual: ${item.currentStock.toInt()} ${item.unitOfMeasure}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it },
          label = { Text("Cantidad a Ingresar *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = minAlertText,
          onValueChange = { minAlertText = it },
          label = { Text("Alerta de Stock Mínimo") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = quantityText.toDoubleOrNull() ?: 0.0
          val minA = minAlertText.toDoubleOrNull()
          if (qty > 0) {
            onConfirm(selectedProdId, qty, minA)
          }
        },
      ) {
        Text("Añadir Stock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}

@Composable
private fun AdjustStockDialog(
  item: WarehouseStockItem,
  onDismiss: () -> Unit,
  onConfirm: (newStock: Double, minAlert: Double?) -> Unit,
) {
  var stockText by remember { mutableStateOf(item.currentStock.toInt().toString()) }
  var minAlertText by remember { mutableStateOf(item.minAlertStock.toInt().toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Ajustar Stock del Producto") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Producto: ${item.productName}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(
          value = stockText,
          onValueChange = { stockText = it },
          label = { Text("Stock Físico Real *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = minAlertText,
          onValueChange = { minAlertText = it },
          label = { Text("Alerta de Stock Mínimo") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newStock = stockText.toDoubleOrNull()
          val minA = minAlertText.toDoubleOrNull()
          if (newStock != null && newStock >= 0) {
            onConfirm(newStock, minA)
          }
        },
      ) {
        Text("Guardar Ajuste")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}
