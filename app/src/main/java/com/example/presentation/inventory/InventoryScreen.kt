package com.example.presentation.inventory

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import com.example.domain.icon.ProductIconView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DateHelper
import com.example.domain.model.Product
import com.example.domain.model.SessionStatus
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.ExpandableInventoryCard
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
  viewModel: InventoryViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // Diálogos de acción
  var activeEntryItem by remember { mutableStateOf<DailyInventoryItem?>(null) }
  var activeCountItem by remember { mutableStateOf<DailyInventoryItem?>(null) }
  var activeEditSaleItem by remember { mutableStateOf<DailyInventoryItem?>(null) }
  var showCloseDayDialog by remember { mutableStateOf(false) }
  var showNewSessionDialog by remember { mutableStateOf(false) }

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

  val selectedSession = uiState.selectedSession
  val isClosed = selectedSession?.status == SessionStatus.CLOSED

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Control de Inventario",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = buildString {
                append(uiState.currentProject?.name ?: "Local")
                selectedSession?.let { append(" • Fecha: ${it.dateString}") } ?: append(" • Sin jornada")
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          ThemeToggleIconButton()
          IconButton(
            onClick = { showNewSessionDialog = true },
            modifier = Modifier.testTag("btn_new_session"),
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Nueva Jornada", modifier = Modifier.size(22.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier,
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // 1. Selector Horizontal de Jornadas
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        uiState.allSessions.forEach { session ->
          val isSelected = session.id == selectedSession?.id
          val isSessionClosed = session.status == SessionStatus.CLOSED
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectSession(session.id) },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                  imageVector = if (isSessionClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                  tint = if (isSessionClosed) DangerRed else SuccessGreen,
                )
                Text(session.dateString, style = MaterialTheme.typography.labelMedium)
              }
            },
            shape = RoundedCornerShape(8.dp),
          )
        }
      }

      // 2. Barra de Búsqueda y Filtros de Categoría
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          placeholder = { Text("Buscar producto...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_search_field"),
          shape = RoundedCornerShape(10.dp),
          singleLine = true,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Categorías
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = uiState.selectedCategoryId == null,
            onClick = { viewModel.setCategoryFilter(null) },
            label = { Text("Todos", style = MaterialTheme.typography.labelMedium) },
          )
          uiState.categories.forEach { cat ->
            FilterChip(
              selected = uiState.selectedCategoryId == cat.id,
              onClick = { viewModel.setCategoryFilter(cat.id) },
              label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
            )
          }
        }
      }

      // 3. Banner Informativo de Cierre o Botón de Cierre
      if (selectedSession != null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
          color = if (isClosed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isClosed) "Jornada Cerrada (Inmutable)" else "Jornada en curso (Abierta)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
              Text(
                text = "Venta: ${Product.formatMoney(uiState.daySummary?.totalSalesCents ?: 0L)} • Utilidad: ${Product.formatMoney(uiState.daySummary?.totalProfitCents ?: 0L)}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (!isClosed) {
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { showCloseDayDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_close_day_trigger"),
              ) {
                Text("Cerrar Día", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // 4. Lista de Productos e Inventarios
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(uiState.filteredItems, key = { it.id }) { item ->
          ExpandableInventoryCard(
            item = item,
            isSessionClosed = isClosed,
            onAddEntriesClick = { activeEntryItem = item },
            onAdjustClosingClick = { activeCountItem = item },
            onEditSaleClick = { activeEditSaleItem = item },
          )
        }
      }
    }
  }

  // DIÁLOGO: REGISTRAR ENTRADA
  activeEntryItem?.let { item ->
    var entriesInput by remember { mutableStateOf("") }
    val additionalEntries = entriesInput.toDoubleOrNull() ?: 0.0
    val simulatedAvailable = item.availableStock + additionalEntries

    AlertDialog(
      onDismissRequest = { activeEntryItem = null },
      icon = {
        ProductIconView(
          productName = item.productName,
          categoryName = item.categoryName,
          size = 46.dp,
          iconSize = 26.dp,
        )
      },
      title = {
        Text(
          text = "Entrada: ${item.productName}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Disponible actual: ${item.availableStock} ${item.unitOfMeasure}", style = MaterialTheme.typography.bodyMedium)

          OutlinedTextField(
            value = entriesInput,
            onValueChange = { entriesInput = it },
            label = { Text("Nueva Entrada a Agregar") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_entry_quantity"),
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                "NUEVO DISPONIBLE: $simulatedAvailable ${item.unitOfMeasure}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (additionalEntries > 0) {
              viewModel.addEntries(item.productId, additionalEntries)
              activeEntryItem = null
            }
          },
          enabled = additionalEntries > 0,
          modifier = Modifier.testTag("btn_confirm_entry"),
        ) {
          Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Registrar")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeEntryItem = null }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO: CONTEO FINAL Y ROTURAS (Fórmula F = E - L - M)
  activeCountItem?.let { item ->
    var closingInput by remember { mutableStateOf(if (item.closingStock > 0) item.closingStock.toString() else "") }
    var breakageInput by remember { mutableStateOf(if (item.breakage > 0) item.breakage.toString() else "0") }

    val closingVal = closingInput.toDoubleOrNull() ?: 0.0
    val breakageVal = breakageInput.toDoubleOrNull() ?: 0.0
    val isExceeded = (closingVal + breakageVal) > (item.availableStock + 0.0001)

    // Cálculos en vivo
    val soldCalc = if (!isExceeded) maxOf(0.0, item.availableStock - closingVal - breakageVal) else 0.0
    val salesCalcCents = (soldCalc * item.salePriceCents).toLong()
    val costCalcCents = (soldCalc * item.costPriceCents).toLong()
    val profitCalcCents = salesCalcCents - costCalcCents

    AlertDialog(
      onDismissRequest = { activeCountItem = null },
      icon = {
        ProductIconView(
          productName = item.productName,
          categoryName = item.categoryName,
          size = 46.dp,
          iconSize = 26.dp,
        )
      },
      title = {
        Text(
          text = "Conteo: ${item.productName}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Disponible en sistema: ${item.availableStock} ${item.unitOfMeasure}",
            style = MaterialTheme.typography.bodySmall,
          )

          OutlinedTextField(
            value = closingInput,
            onValueChange = { closingInput = it },
            label = { Text("Inventario Final Físico (L)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeded,
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_closing_stock"),
          )

          OutlinedTextField(
            value = breakageInput,
            onValueChange = { breakageInput = it },
            label = { Text("Roturas / Mermas (M)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeded,
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_breakage"),
          )

          if (isExceeded) {
            Text(
              text = "El inventario final ($closingVal) + roturas ($breakageVal) supera el disponible (${item.availableStock})",
              color = DangerRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  "CÁLCULOS EN TIEMPO REAL",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
                DialogCalcRow("Vendido (F = E - L - M):", "$soldCalc ${item.unitOfMeasure}", isBold = true)
                DialogCalcRow("Importe Venta (H = F * G):", Product.formatMoney(salesCalcCents), color = MaterialTheme.colorScheme.primary)
                DialogCalcRow("Importe Costo (J = F * I):", Product.formatMoney(costCalcCents))
                DialogCalcRow("Utilidad (K = H - J):", Product.formatMoney(profitCalcCents), color = SuccessGreen, isBold = true)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (!isExceeded && closingVal >= 0 && breakageVal >= 0) {
              viewModel.updateClosingAndBreakage(item.productId, closingVal, breakageVal)
              activeCountItem = null
            }
          },
          enabled = !isExceeded && closingInput.isNotBlank(),
          modifier = Modifier.testTag("btn_confirm_count"),
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Guardar")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeCountItem = null }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO: CONFIRMAR CIERRE DIARIO
  if (showCloseDayDialog && selectedSession != null) {
    val summary = uiState.daySummary
    AlertDialog(
      onDismissRequest = { showCloseDayDialog = false },
      title = { Text("Cierre de Jornada: ${selectedSession.dateString}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Al cerrar la jornada, los valores quedarán inmutables y el inventario final se transferirá automáticamente al siguiente día.",
            style = MaterialTheme.typography.bodySmall,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              DialogCalcRow("Ventas Totales:", Product.formatMoney(summary?.totalSalesCents ?: 0L), isBold = true)
              DialogCalcRow("Costo Total:", Product.formatMoney(summary?.totalCostCents ?: 0L))
              DialogCalcRow("Utilidad Total:", Product.formatMoney(summary?.totalProfitCents ?: 0L), color = SuccessGreen, isBold = true)
              DialogCalcRow("Margen:", "${String.format("%.1f", summary?.marginPercentage ?: 0.0)}%", isBold = true)
              DialogCalcRow("Roturas:", "${summary?.totalBreakage ?: 0.0} un.", color = DangerRed)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.closeCurrentSession()
            showCloseDayDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("btn_confirm_close_day"),
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Confirmar y Cerrar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCloseDayDialog = false }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO: CREAR NUEVA JORNADA
  var showCalendarForInventory by remember { mutableStateOf(false) }
  if (showNewSessionDialog) {
    var newDateInput by remember { mutableStateOf(DateHelper.getTodayDateString()) }
    AlertDialog(
      onDismissRequest = { showNewSessionDialog = false },
      icon = {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
      },
      title = { Text("Abrir Nueva Jornada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "El sistema transferirá automáticamente el inventario final del día previo como inicio de esta nueva jornada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = { newDateInput = DateHelper.getTodayDateString() },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hoy", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(
              onClick = { showCalendarForInventory = true },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Calendario", style = MaterialTheme.typography.labelMedium)
            }
          }

          OutlinedTextField(
            value = newDateInput,
            onValueChange = { newDateInput = it },
            label = { Text("Fecha (DD-MM-YYYY)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_new_session_date"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newDateInput.isNotBlank()) {
              val epochDay = DateHelper.parseDateStringToEpochDay(newDateInput.trim())
              viewModel.createNewSession(newDateInput.trim(), epochDay)
              showNewSessionDialog = false
            }
          },
          modifier = Modifier.testTag("btn_confirm_new_session"),
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Abrir")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewSessionDialog = false }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  if (showCalendarForInventory) {
    AppDatePickerDialog(
      title = "Seleccionar Fecha de Jornada",
      onDateSelected = { selectedMillis ->
        val dateStr = DateHelper.formatEpochMillisToDateString(selectedMillis)
        val epochDay = DateHelper.epochMillisToEpochDay(selectedMillis)
        viewModel.createNewSession(dateStr, epochDay)
        showCalendarForInventory = false
        showNewSessionDialog = false
      },
      onDismiss = { showCalendarForInventory = false },
    )
  }

  // DIÁLOGO: EDITAR O ANULAR VENTA (CORRECCIÓN DE ERRORES)
  activeEditSaleItem?.let { item ->
    var soldQuantityInput by remember { mutableStateOf(if (item.soldQuantity > 0) item.soldQuantity.toString() else "0") }
    val newSoldVal = soldQuantityInput.toDoubleOrNull() ?: 0.0
    val maxAllowedSold = maxOf(0.0, item.availableStock - item.breakage)
    val isExceeded = (newSoldVal + item.breakage) > (item.availableStock + 0.0001)

    val simulatedSalesCents = (newSoldVal * item.salePriceCents).toLong()
    val simulatedCostCents = (newSoldVal * item.costPriceCents).toLong()
    val simulatedProfitCents = simulatedSalesCents - simulatedCostCents
    val simulatedClosing = maxOf(0.0, item.availableStock - item.breakage - newSoldVal)

    AlertDialog(
      onDismissRequest = { activeEditSaleItem = null },
      title = {
        Text(
          text = "Corregir / Anular Venta",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "${item.productName} • Disp: ${item.availableStock} • Venta actual: ${item.soldQuantity} ${item.unitOfMeasure}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = soldQuantityInput,
            onValueChange = { soldQuantityInput = it },
            label = { Text("Nueva Cantidad Vendida Real") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeded,
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_edit_sold_quantity"),
          )

          if (isExceeded) {
            Text(
              text = "La venta ($newSoldVal) supera el disponible menos roturas ($maxAllowedSold)",
              color = DangerRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  "RECÁLCULO AUTOMÁTICO",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
                DialogCalcRow("Nuevo Importe Venta:", Product.formatMoney(simulatedSalesCents), color = MaterialTheme.colorScheme.primary, isBold = true)
                DialogCalcRow("Nueva Utilidad:", Product.formatMoney(simulatedProfitCents), color = SuccessGreen, isBold = true)
                DialogCalcRow("Inventario Final Resultante:", "$simulatedClosing ${item.unitOfMeasure}")
              }
            }
          }
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (item.soldQuantity > 0) {
            Button(
              onClick = {
                viewModel.cancelSale(item.productId)
                activeEditSaleItem = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
              modifier = Modifier.testTag("btn_cancel_sale"),
            ) {
              Text("Anular Venta")
            }
          }
          Button(
            onClick = {
              if (!isExceeded && newSoldVal >= 0.0) {
                viewModel.updateSoldQuantity(item.productId, newSoldVal)
                activeEditSaleItem = null
              }
            },
            enabled = !isExceeded && soldQuantityInput.isNotBlank(),
            modifier = Modifier.testTag("btn_save_sold_quantity"),
          ) {
            Text("Guardar Venta")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { activeEditSaleItem = null }) {
          Text("Cancelar")
        }
      },
    )
  }
}

@Composable
private fun DialogCalcRow(
  label: String,
  value: String,
  color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
  isBold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.weight(1f, fill = false),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium),
      color = if (color != androidx.compose.ui.graphics.Color.Unspecified) color else MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
      maxLines = 1,
    )
  }
}
