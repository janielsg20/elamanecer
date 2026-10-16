package com.example.presentation.history

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import com.example.domain.icon.ProductIconView
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Product
import com.example.domain.model.SessionStatus
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  viewModel: ReportsViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showCalendarDialog by remember { mutableStateOf(false) }

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

  val selectedSummary = uiState.selectedDaySummary
  val selectedSession = uiState.allSessions.find { it.id == uiState.selectedSessionId }
  val activeFilter = uiState.calendarFilterDate

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Historial y Reportes",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${uiState.currentProject?.name ?: "Local"} • Jornadas: ${uiState.filteredSessions.size}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showCalendarDialog = true },
            modifier = Modifier.testTag("btn_filter_calendar"),
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = "Filtrar por Calendario",
              tint = if (activeFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
          }
          IconButton(
            onClick = {
              val csv = viewModel.generateCsvExport()
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, csv)
                putExtra(Intent.EXTRA_TITLE, "Reporte de Ventas - ${selectedSession?.dateString}")
                type = "text/plain"
              }
              val shareIntent = Intent.createChooser(sendIntent, "Exportar Reporte")
              context.startActivity(shareIntent)
            },
            enabled = selectedSession != null,
            modifier = Modifier.testTag("btn_export_csv"),
          ) {
            Icon(Icons.Default.Share, contentDescription = "Exportar Reporte", modifier = Modifier.size(22.dp))
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // 1. Barra de Filtro Activo de Calendario
      if (activeFilter != null) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                Icons.Default.FilterAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Filtro activo: $activeFilter",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(
              onClick = { viewModel.clearCalendarFilter() },
              modifier = Modifier
                .size(24.dp)
                .testTag("btn_clear_calendar_filter"),
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Quitar Filtro",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      // 2. Selector Horizontal de Jornadas con Botón Directo de Calendario
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Botón rápido de Calendario
        FilterChip(
          selected = activeFilter != null,
          onClick = { showCalendarDialog = true },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
              Text(if (activeFilter != null) "Filtrado" else "Calendario", style = MaterialTheme.typography.labelMedium)
            }
          },
          shape = RoundedCornerShape(8.dp),
        )

        if (activeFilter != null) {
          FilterChip(
            selected = false,
            onClick = { viewModel.clearCalendarFilter() },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Ver Todas", style = MaterialTheme.typography.labelMedium)
              }
            },
            shape = RoundedCornerShape(8.dp),
          )
        }

        uiState.filteredSessions.forEach { session ->
          val isSelected = session.id == uiState.selectedSessionId
          val isClosed = session.status == SessionStatus.CLOSED
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectSession(session.id) },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                  imageVector = if (isClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                  tint = if (isClosed) DangerRed else SuccessGreen,
                )
                Text(session.dateString, style = MaterialTheme.typography.labelMedium)
              }
            },
            shape = RoundedCornerShape(8.dp),
          )
        }
      }

      // 3. Caso cuando el filtro no arroja resultados
      if (uiState.filteredSessions.isEmpty() && activeFilter != null) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Sin jornadas el $activeFilter",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "No se encontraron registros de ventas ni inventario para la fecha seleccionada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { viewModel.openSessionForFilterDate(activeFilter) },
              modifier = Modifier.testTag("btn_create_session_from_filter"),
            ) {
              Text("Abrir Jornada $activeFilter")
            }
            OutlinedButton(
              onClick = { viewModel.clearCalendarFilter() },
            ) {
              Text("Ver Todas")
            }
          }
        }
      } else {
        // 4. Resumen Ejecutivo del Día Seleccionado (Grid 2x2 para no desbordar)
        if (selectedSummary != null) {
          ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 4.dp),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Resumen ${selectedSummary.dateString}",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (selectedSummary.status == SessionStatus.CLOSED) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                ) {
                  Text(
                    text = if (selectedSummary.status == SessionStatus.CLOSED) "Cerrado" else "En curso",
                    color = if (selectedSummary.status == SessionStatus.CLOSED) DangerRed else SuccessGreen,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Fila 1: Ventas y Costo
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Ventas Totales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = Product.formatMoney(selectedSummary.totalSalesCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Costo Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = Product.formatMoney(selectedSummary.totalCostCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Fila 2: Utilidad y Margen
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Utilidad Neta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = Product.formatMoney(selectedSummary.totalProfitCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Margen General", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = "${String.format("%.1f", selectedSummary.marginPercentage)}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Fila 3: Unidades y Roturas
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Unidades Vendidas: ${selectedSummary.totalUnitsSold}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
                Text(
                  text = "Roturas: ${selectedSummary.totalBreakage} un.",
                  style = MaterialTheme.typography.labelSmall,
                  color = DangerRed,
                  maxLines = 1,
                )
              }
            }
          }
        }

        // 5. Desglose detallado por producto del día
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(uiState.selectedDayItems, key = { it.id }) { item ->
            OutlinedCard(
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
              ),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    ProductIconView(
                      productName = item.productName,
                      categoryName = item.categoryName,
                      size = 40.dp,
                      iconSize = 22.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "${item.categoryName} • ${item.unitOfMeasure}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = Product.formatMoney(item.saleAmountCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Disp: ${item.availableStock} • Vend: ${item.soldQuantity} • Fin: ${item.closingStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (item.breakage > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Rot: ${item.breakage}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = DangerRed,
                      maxLines = 1,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Precio: ${Product.formatMoney(item.salePriceCents)} | Costo: ${Product.formatMoney(item.costPriceCents)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "Utilidad: ${Product.formatMoney(item.profitCents)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
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

  // DIÁLOGO: FILTRAR POR CALENDARIO
  if (showCalendarDialog) {
    AppDatePickerDialog(
      title = "Filtrar Jornadas por Fecha",
      onDateSelected = { selectedMillis ->
        viewModel.filterByDateMillis(selectedMillis)
        showCalendarDialog = false
      },
      onDismiss = { showCalendarDialog = false },
    )
  }
}
