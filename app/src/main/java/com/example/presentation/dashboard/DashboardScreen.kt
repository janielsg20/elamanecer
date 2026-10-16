package com.example.presentation.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DateHelper
import com.example.domain.model.Product
import com.example.domain.model.SessionStatus
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.MetricCard
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: DashboardViewModel,
  onNavigateToSales: () -> Unit,
  onNavigateToInventory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val summary = uiState.summary
  val activeSession = uiState.activeSession
  val isOpen = activeSession?.status == SessionStatus.OPEN

  var showCalendarDialog by remember { mutableStateOf(false) }
  var showCloseConfirmDialog by remember { mutableStateOf(false) }
  var showNewSessionOptionsDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

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
              text = "Buenos días",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = activeSession?.let { "Jornada: ${it.dateString} (${if (isOpen) "Abierta" else "Cerrada"})" }
                ?: "Sin jornada activa",
              style = MaterialTheme.typography.bodySmall,
              color = if (isOpen) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showCalendarDialog = true },
            modifier = Modifier.testTag("dashboard_btn_calendar_top"),
          ) {
            Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha en calendario", modifier = Modifier.size(22.dp))
          }
          ThemeToggleIconButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier,
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Panel de Control de Jornada (Abierta vs Cerrada)
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isOpen) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                  imageVector = if (isOpen) Icons.Default.LockOpen else Icons.Default.Lock,
                  contentDescription = null,
                  tint = if (isOpen) SuccessGreen else DangerRed,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = if (isOpen) "Jornada en Curso: ${activeSession?.dateString ?: ""}" else "Jornada Cerrada / Sin Abrir",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isOpen) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
              ) {
                Text(
                  text = if (isOpen) "ABIERTA" else "CERRADA",
                  color = if (isOpen) SuccessGreen else DangerRed,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  maxLines = 1,
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones condicionales según el estado de la jornada
            if (isOpen) {
              // --- JORNADA ABIERTA ---
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = { showCloseConfirmDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_btn_close_session"),
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                  ),
                ) {
                  Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Cerrar Jornada", maxLines = 1)
                }

                OutlinedButton(
                  onClick = { showNewSessionOptionsDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_btn_switch_session"),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Nueva / Otra", maxLines = 1)
                }
              }
            } else {
              // --- JORNADA CERRADA O SIN ABRIR ---
              Text(
                text = "Inicia el día abriendo la jornada de hoy o elige una fecha del calendario:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = { viewModel.openTodaySession() },
                  modifier = Modifier
                    .weight(1.1f)
                    .testTag("dashboard_btn_open_today"),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Abrir Hoy (${DateHelper.getTodayDateString()})", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                  onClick = { showCalendarDialog = true },
                  modifier = Modifier
                    .weight(0.9f)
                    .testTag("dashboard_btn_open_calendar"),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Calendario", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
              }

              if (activeSession != null && activeSession.status == SessionStatus.CLOSED) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                  onClick = { viewModel.reopenActiveSession() },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_btn_reopen_session"),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Reabrir Jornada (${activeSession.dateString})", color = SuccessGreen, maxLines = 1)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de Vender e Inventario
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilledTonalButton(
                onClick = onNavigateToSales,
                modifier = Modifier
                  .weight(1f)
                  .testTag("dashboard_btn_quick_sale"),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Punto de Venta", maxLines = 1)
              }
              FilledTonalButton(
                onClick = onNavigateToInventory,
                modifier = Modifier
                  .weight(1f)
                  .testTag("dashboard_btn_inventory"),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inventario", maxLines = 1)
              }
            }
          }
        }
      }

      // 2. Tarjetas de Métricas Principales (Grid 2x2 calibrado)
      item {
        val salesCents = summary?.totalSalesCents ?: 0L
        val profitCents = summary?.totalProfitCents ?: 0L
        val margin = summary?.marginPercentage ?: 0.0

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricCard(
              title = "Ventas ${activeSession?.dateString ?: "Hoy"}",
              value = Product.formatMoney(salesCents),
              subtitle = "${summary?.totalUnitsSold ?: 0.0} un. vendidas",
              icon = Icons.Default.AttachMoney,
              highlightColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f),
              testTag = "metric_sales",
            )
            MetricCard(
              title = "Utilidad Neta",
              value = Product.formatMoney(profitCents),
              subtitle = "Margen: ${String.format("%.1f", margin)}%",
              icon = Icons.Default.TrendingUp,
              highlightColor = SuccessGreen,
              modifier = Modifier.weight(1f),
              testTag = "metric_profit",
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricCard(
              title = "Margen General",
              value = "${String.format("%.1f", margin)}%",
              subtitle = "Costo: ${Product.formatMoney(summary?.totalCostCents ?: 0L)}",
              icon = Icons.Default.Percent,
              highlightColor = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
              testTag = "metric_margin",
            )
            MetricCard(
              title = "Roturas / Mermas",
              value = "${summary?.totalBreakage ?: 0.0} un.",
              subtitle = "Dañados",
              icon = Icons.Default.RemoveShoppingCart,
              highlightColor = DangerRed,
              modifier = Modifier.weight(1f),
              testTag = "metric_breakage",
            )
          }
        }
      }

      // 3. Alertas de Stock Bajo
      if (uiState.lowStockAlerts.isNotEmpty()) {
        item {
          ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = WarningOrange,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Alertas de Inventario (${uiState.lowStockAlerts.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              uiState.lowStockAlerts.take(4).forEach { alert ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = alert.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (alert.isOut) DangerRed.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                  ) {
                    Text(
                      text = if (alert.isOut) "Agotado" else "${alert.currentStock} (Mín: ${alert.minStock})",
                      color = if (alert.isOut) DangerRed else WarningOrange,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 4. Desglose de Ventas por Categoría
      if (uiState.categorySummaries.isNotEmpty()) {
        item {
          ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Desglose por Categoría",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.height(10.dp))

              val totalSales = (summary?.totalSalesCents ?: 1L).coerceAtLeast(1L)
              uiState.categorySummaries.forEach { cat ->
                val ratio = (cat.totalSalesCents.toFloat() / totalSales.toFloat()).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = cat.categoryName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      modifier = Modifier.weight(1f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "${Product.formatMoney(cat.totalSalesCents)} (${String.format("%.1f", ratio * 100)}%)",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                    )
                  }
                  Spacer(modifier = Modifier.height(3.dp))
                  LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "Utilidad: ${Product.formatMoney(cat.totalProfitCents)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = SuccessGreen,
                      maxLines = 1,
                    )
                    Text(
                      text = "Margen: ${String.format("%.1f", cat.marginPercentage)}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. Ranking de Productos Más Vendidos
      if (uiState.topSellingItems.isNotEmpty()) {
        item {
          ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Productos Más Vendidos Hoy",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.height(8.dp))
              uiState.topSellingItems.forEachIndexed { index, item ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "${index + 1}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.productName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${item.soldQuantity} ${item.unitOfMeasure} vendidos",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = Product.formatMoney(item.saleAmountCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // DIÁLOGO: SELECCIÓN EN CALENDARIO
  if (showCalendarDialog) {
    AppDatePickerDialog(
      title = "Abrir Jornada por Fecha",
      onDateSelected = { selectedEpochMillis ->
        viewModel.openSessionFromDateMillis(selectedEpochMillis)
        showCalendarDialog = false
      },
      onDismiss = { showCalendarDialog = false },
    )
  }

  // DIÁLOGO: CONFIRMACIÓN DE CIERRE DE JORNADA
  if (showCloseConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCloseConfirmDialog = false },
      title = {
        Text(
          text = "¿Cerrar Jornada ${activeSession?.dateString ?: ""}?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Al cerrar la jornada:")
          Text("• Ventas registradas: ${Product.formatMoney(summary?.totalSalesCents ?: 0L)}")
          Text("• Utilidad neta: ${Product.formatMoney(summary?.totalProfitCents ?: 0L)}")
          Text("• Unidades vendidas: ${summary?.totalUnitsSold ?: 0.0}")
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "El inventario final de hoy se transferirá automáticamente como inventario inicial de la siguiente jornada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.closeActiveSession()
            showCloseConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
          modifier = Modifier.testTag("btn_confirm_close_session"),
        ) {
          Text("Confirmar Cierre")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCloseConfirmDialog = false }) {
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO: OPCIONES DE NUEVA JORNADA
  if (showNewSessionOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showNewSessionOptionsDialog = false },
      title = {
        Text(
          text = "Abrir Nueva Jornada",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Selecciona cómo deseas crear o activar la jornada:",
            style = MaterialTheme.typography.bodyMedium,
          )
          Button(
            onClick = {
              viewModel.openTodaySession()
              showNewSessionOptionsDialog = false
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jornada de Hoy (${DateHelper.getTodayDateString()})")
          }
          FilledTonalButton(
            onClick = {
              showNewSessionOptionsDialog = false
              showCalendarDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seleccionar en Calendario")
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showNewSessionOptionsDialog = false }) {
          Text("Cancelar")
        }
      },
    )
  }
}
