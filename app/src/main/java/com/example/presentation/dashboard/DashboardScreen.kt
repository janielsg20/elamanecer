package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.icon.ProductIconView
import com.example.domain.model.DateHelper
import com.example.domain.model.Product
import com.example.domain.model.Project
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
  onNavigateToWarehouse: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val summary = uiState.summary
  val activeSession = uiState.activeSession
  val currentProject = uiState.currentProject
  val isOpen = activeSession?.status == SessionStatus.OPEN

  var showCalendarDialog by remember { mutableStateOf(false) }
  var showCloseConfirmDialog by remember { mutableStateOf(false) }
  var showNewSessionOptionsDialog by remember { mutableStateOf(false) }
  var showProjectSelectorDialog by remember { mutableStateOf(false) }
  var showCreateProjectDialog by remember { mutableStateOf(false) }
  var projectToEdit by remember { mutableStateOf<Project?>(null) }
  var projectToDelete by remember { mutableStateOf<Project?>(null) }

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
      val subtitle = activeSession?.let { "Jornada: ${it.dateString} (${if (isOpen) "Abierta" else "Cerrada"})" }
        ?: (currentProject?.name ?: "Panel Principal")
      com.example.ui.components.ElAmanecerTopBar(
        subtitle = subtitle,
        actions = {
          // Selector de local en barra superior
          FilledTonalButton(
            onClick = { showProjectSelectorDialog = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier
              .height(32.dp)
              .testTag("dashboard_btn_projects_menu"),
          ) {
            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = currentProject?.type ?: "Locales",
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
            )
          }

          IconButton(
            onClick = { showCalendarDialog = true },
            modifier = Modifier.testTag("dashboard_btn_calendar_top"),
          ) {
            Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha en calendario", modifier = Modifier.size(22.dp))
          }
          ThemeToggleIconButton()
        },
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
      // 0. Tarjeta de Local Activo y Selector
      item {
        ProjectHeaderCard(
          project = currentProject,
          onSwitchProject = { showProjectSelectorDialog = true },
          onCreateProject = { showCreateProjectDialog = true },
          onEditProject = { projectToEdit = currentProject },
          onDeleteProject = { projectToDelete = currentProject },
          canDelete = uiState.projects.size > 1,
        )
      }

      // 0.1 Shortcut directo a Almacén
      item {
        WarehouseShortcutCard(
          projectName = currentProject?.name ?: "Local",
          onNavigateToWarehouse = onNavigateToWarehouse,
        )
      }

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
                  Text("Cambiar Fecha", maxLines = 1)
                }
              }
            } else {
              // --- JORNADA CERRADA O SIN SESIÓN ---
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeSession != null) {
                  Button(
                    onClick = { viewModel.reopenActiveSession() },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("dashboard_btn_reopen_session"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reabrir Jornada (${activeSession.dateString})", maxLines = 1)
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Button(
                    onClick = { viewModel.openTodaySession() },
                    modifier = Modifier
                      .weight(1.2f)
                      .testTag("dashboard_btn_open_today"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (activeSession == null) MaterialTheme.colorScheme.primary else SuccessGreen,
                    ),
                  ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir Hoy (${DateHelper.getTodayDateString()})", maxLines = 1)
                  }

                  OutlinedButton(
                    onClick = { showCalendarDialog = true },
                    modifier = Modifier
                      .weight(0.9f)
                      .testTag("dashboard_btn_open_calendar"),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calendario", maxLines = 1)
                  }
                }
              }
            }
          }
        }
      }

      // 2. Métricas Principales (Grid 2x2 calibrado)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Resumen Financiero del Día",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MetricCard(
              title = "Ventas Totales",
              value = Product.formatMoney(summary?.totalSalesCents ?: 0L),
              subtitle = "${String.format("%,.0f", summary?.totalUnitsSold ?: 0.0)} unidades vendidas",
              icon = Icons.Default.AttachMoney,
              highlightColor = SuccessGreen,
              modifier = Modifier.weight(1f),
              testTag = "metric_total_sales",
            )

            MetricCard(
              title = "Utilidad Neta",
              value = Product.formatMoney(summary?.totalProfitCents ?: 0L),
              subtitle = "Margen: ${String.format("%.1f", summary?.marginPercentage ?: 0.0)}%",
              icon = Icons.Default.TrendingUp,
              highlightColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f),
              testTag = "metric_net_profit",
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MetricCard(
              title = "Costo Mercancía",
              value = Product.formatMoney(summary?.totalCostCents ?: 0L),
              subtitle = "Costo base de venta",
              icon = Icons.Default.ShoppingCart,
              highlightColor = WarningOrange,
              modifier = Modifier.weight(1f),
              testTag = "metric_cost_amount",
            )

            MetricCard(
              title = "Roturas / Pérdidas",
              value = "${String.format("%,.0f", summary?.totalBreakage ?: 0.0)} U",
              subtitle = "Merma registrada",
              icon = Icons.Default.RemoveShoppingCart,
              highlightColor = DangerRed,
              modifier = Modifier.weight(1f),
              testTag = "metric_breakage",
            )
          }
        }
      }

      // 3. Accesos Rápidos Principales
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = onNavigateToSales,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("dashboard_quick_sales_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ventas Rápidas", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
          }

          FilledTonalButton(
            onClick = onNavigateToInventory,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("dashboard_quick_inventory_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Inventario Diario", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
          }
        }
      }

      // 4. Alertas de Stock Bajo
      if (uiState.lowStockAlerts.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                Text(
                  text = "Alertas de Stock Bajo (${uiState.lowStockAlerts.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = DangerRed,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              uiState.lowStockAlerts.take(4).forEach { alert ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = alert.product.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (alert.isOut) DangerRed else WarningOrange,
                  ) {
                    Text(
                      text = if (alert.isOut) "AGOTADO" else "${alert.currentStock.toInt()} / ${alert.minStock.toInt()} ${alert.product.unitOfMeasure}",
                      style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. Desglose por Categoría
      if (uiState.categorySummaries.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Ventas por Categoría",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Icon(
                  imageVector = Icons.Default.Category,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              val totalSales = summary?.totalSalesCents ?: 1L
              uiState.categorySummaries.forEach { cat ->
                val ratio = if (totalSales > 0) cat.totalSalesCents.toFloat() / totalSales else 0f
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = cat.categoryName,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                      text = "${Product.formatMoney(cat.totalSalesCents)} (${String.format("%.0f", ratio * 100)}%)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(modifier = Modifier.height(3.dp))
                  LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      // 6. Top Productos Vendidos
      if (uiState.topSellingItems.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Más Vendidos de la Jornada",
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
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  ProductIconView(
                    productName = item.productName,
                    categoryName = item.categoryName,
                    size = 32.dp,
                    iconSize = 18.dp,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.productName,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${item.soldQuantity.toInt()} ${item.unitOfMeasure} vendidos",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = Product.formatMoney(item.saleAmountCents),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // DIÁLOGO CALENDARIO
  if (showCalendarDialog) {
    AppDatePickerDialog(
      onDateSelected = { selectedMillis ->
        viewModel.openSessionFromDateMillis(selectedMillis)
        showCalendarDialog = false
      },
      onDismiss = { showCalendarDialog = false },
    )
  }

  // DIÁLOGO: CONFIRMACIÓN DE CIERRE
  if (showCloseConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCloseConfirmDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = DangerRed)
          Text("Cerrar Jornada")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "¿Deseas cerrar definitivamente la jornada activa del ${activeSession?.dateString ?: ""}?",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            text = "• Las existencias finales se congelarán y se transferirán automáticamente como inventario inicial de la siguiente jornada.\n• Podrás reabrir la jornada en cualquier momento si necesitas corregir datos.",
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
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Confirmar Cierre")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCloseConfirmDialog = false }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
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

  // DIÁLOGO: SELECCIONADOR DE PROYECTO
  if (showProjectSelectorDialog) {
    ProjectSelectorModal(
      projects = uiState.projects,
      currentProject = currentProject,
      onSelectProject = { projId ->
        viewModel.selectProject(projId)
        showProjectSelectorDialog = false
      },
      onCreateNew = {
        showProjectSelectorDialog = false
        showCreateProjectDialog = true
      },
      onEditProject = { proj ->
        showProjectSelectorDialog = false
        projectToEdit = proj
      },
      onDeleteProject = { proj ->
        showProjectSelectorDialog = false
        projectToDelete = proj
      },
      onDismiss = { showProjectSelectorDialog = false },
    )
  }

  // DIÁLOGO: CREAR NUEVO PROYECTO
  if (showCreateProjectDialog) {
    ProjectFormModal(
      project = null,
      onDismiss = { showCreateProjectDialog = false },
      onConfirm = { name, type, desc, color ->
        viewModel.createProject(name, type, desc, color)
        showCreateProjectDialog = false
      },
    )
  }

  // DIÁLOGO: EDITAR PROYECTO
  projectToEdit?.let { proj ->
    ProjectFormModal(
      project = proj,
      onDismiss = { projectToEdit = null },
      onConfirm = { name, type, desc, color ->
        viewModel.updateProject(proj.id, name, type, desc, color)
        projectToEdit = null
      },
    )
  }

  // DIÁLOGO: CONFIRMAR ELIMINACIÓN DE LOCAL
  projectToDelete?.let { proj ->
    AlertDialog(
      onDismissRequest = { projectToDelete = null },
      title = { Text("Eliminar Local") },
      text = {
        Text("¿Deseas eliminar el local \"${proj.name}\"? Se eliminarán todas sus jornadas e inventarios asociados.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteProject(proj.id)
            projectToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
        ) {
          Text("Eliminar Local")
        }
      },
      dismissButton = {
        TextButton(onClick = { projectToDelete = null }) {
          Text("Cancelar")
        }
      },
    )
  }
}

// -------------------------------------------------------------
// Componente: Tarjeta de Local Activo
// -------------------------------------------------------------
@Composable
private fun ProjectHeaderCard(
  project: Project?,
  onSwitchProject: () -> Unit,
  onCreateProject: () -> Unit,
  onEditProject: () -> Unit,
  onDeleteProject: () -> Unit,
  canDelete: Boolean,
) {
  val projectColor = remember(project?.colorHex) {
    try {
      Color(android.graphics.Color.parseColor(project?.colorHex ?: "#1E88E5"))
    } catch (_: Exception) {
      Color(0xFF1E88E5)
    }
  }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_project_header_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(projectColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              Icons.Default.Store,
              contentDescription = null,
              tint = projectColor,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "LOCAL DE GESTIÓN",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = project?.name ?: "Local Principal",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = projectColor.copy(alpha = 0.15f),
        ) {
          Text(
            text = project?.type ?: "GENERAL",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = projectColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      if (!project?.description.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = project?.description ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Fila de acciones para gestión de locales
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onSwitchProject,
          modifier = Modifier.weight(1.2f),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          shape = RoundedCornerShape(8.dp),
        ) {
          Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cambiar Local", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = onCreateProject,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          shape = RoundedCornerShape(8.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Nuevo", style = MaterialTheme.typography.labelMedium)
        }

        IconButton(
          onClick = onEditProject,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }

        if (canDelete) {
          IconButton(
            onClick = onDeleteProject,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRed, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Componente: Shortcut directo a Almacén Central
// -------------------------------------------------------------
@Composable
private fun WarehouseShortcutCard(
  projectName: String,
  onNavigateToWarehouse: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onNavigateToWarehouse() }
      .testTag("dashboard_warehouse_shortcut_card"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.Inventory,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Sección de Almacenes Centrales",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "Gestionar almacenes y despachar stock a \"$projectName\"",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Ir a Almacenes",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

// -------------------------------------------------------------
// Modales de Locales
// -------------------------------------------------------------
@Composable
private fun ProjectSelectorModal(
  projects: List<Project>,
  currentProject: Project?,
  onSelectProject: (Long) -> Unit,
  onCreateNew: () -> Unit,
  onEditProject: (Project) -> Unit,
  onDeleteProject: (Project) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Locales de Gestión", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        IconButton(onClick = onCreateNew) {
          Icon(Icons.Default.AddBusiness, contentDescription = "Nuevo Local", tint = MaterialTheme.colorScheme.primary)
        }
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Selecciona un local para ver sus ventas, utilidades e inventario independiente:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(projects, key = { it.id }) { proj ->
            val isSelected = proj.id == currentProject?.id
            val pColor = try {
              Color(android.graphics.Color.parseColor(proj.colorHex))
            } catch (_: Exception) {
              MaterialTheme.colorScheme.primary
            }

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectProject(proj.id) },
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) pColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              ),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(pColor),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = proj.name,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = pColor.copy(alpha = 0.2f),
                    ) {
                      Text(
                        text = proj.type,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = pColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                      )
                    }
                  }
                  if (proj.description.isNotBlank()) {
                    Text(
                      text = proj.description,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                if (isSelected) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = "Activo",
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp),
                  )
                }

                IconButton(
                  onClick = { onEditProject(proj) },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                }

                if (projects.size > 1) {
                  IconButton(
                    onClick = { onDeleteProject(proj) },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRed, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onCreateNew) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Crear Nuevo Local")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cerrar")
      }
    },
  )
}

@Composable
private fun ProjectFormModal(
  project: Project?,
  onDismiss: () -> Unit,
  onConfirm: (name: String, type: String, description: String, colorHex: String) -> Unit,
) {
  var name by remember { mutableStateOf(project?.name ?: "") }
  var type by remember { mutableStateOf(project?.type ?: "KIOSKO") }
  var description by remember { mutableStateOf(project?.description ?: "") }
  var selectedColorHex by remember { mutableStateOf(project?.colorHex ?: "#1E88E5") }

  val projectTypes = listOf("KIOSKO", "BAR", "PALADAR", "OTRO")
  val presetColors = listOf(
    "#1E88E5", // Azul
    "#43A047", // Verde
    "#E53935", // Rojo
    "#FB8C00", // Naranja
    "#8E24AA", // Morado
    "#00897B", // Verde azulado
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (project == null) "Nuevo Local de Gestión" else "Editar Local")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre del Local *") },
          placeholder = { Text("ej. Kiosko El Sol, Bar Havana, Paladar Criolla") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("project_form_name_input"),
        )

        Text("Tipo de Local:", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          projectTypes.forEach { pt ->
            FilterChip(
              selected = type == pt,
              onClick = { type = pt },
              label = { Text(pt, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción") },
          placeholder = { Text("Punto de expendio, salón principal, etc.") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Text("Color de Identificación:", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          presetColors.forEach { hex ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val isSelected = selectedColorHex == hex
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .clickable { selectedColorHex = hex }
                .padding(2.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                Icon(
                  Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
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
          if (name.isNotBlank()) {
            onConfirm(name, type, description, selectedColorHex)
          }
        },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("project_form_submit_button"),
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
