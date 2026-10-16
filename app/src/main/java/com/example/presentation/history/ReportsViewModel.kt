package com.example.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.CategorySummary
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DateHelper
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

import com.example.domain.model.Project

data class OverallPeriodSummary(
  val totalSalesCents: Long = 0L,
  val totalCostCents: Long = 0L,
  val totalProfitCents: Long = 0L,
  val marginPercentage: Double = 0.0,
  val totalUnitsSold: Double = 0.0,
  val totalBreakage: Double = 0.0,
  val totalDaysCount: Int = 0,
)

data class ReportsUiState(
  val isLoading: Boolean = true,
  val currentProject: Project? = null,
  val allSessions: List<DailySession> = emptyList(),
  val filteredSessions: List<DailySession> = emptyList(),
  val calendarFilterDate: String? = null,
  val selectedSessionId: Long? = null,
  val selectedDaySummary: DaySummary? = null,
  val selectedDayItems: List<DailyInventoryItem> = emptyList(),
  val overallSummary: OverallPeriodSummary = OverallPeriodSummary(),
  val exportData: String? = null,
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ReportsUiState())
  val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

  private val _selectedSessionId = MutableStateFlow<Long?>(null)
  private val _calendarFilterDate = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      observeReports()
    }
  }

  private fun observeReports() {
    viewModelScope.launch {
      repository.getSelectedProjectId().flatMapLatest { projectId ->
        val project = repository.getProjectById(projectId)
        combine(
          repository.getAllSessions(projectId),
          _calendarFilterDate,
          repository.getAllProjects(),
        ) { sessions, filterDate, allProjects ->
          val curProj = allProjects.find { it.id == projectId } ?: project
          val filtered = if (filterDate.isNullOrBlank()) {
            sessions
          } else {
            sessions.filter { it.dateString == filterDate }
          }

          val currentSelected = _selectedSessionId.value
          val nextSelected = if (filtered.any { it.id == currentSelected }) {
            currentSelected
          } else {
            filtered.firstOrNull()?.id
          }

          if (nextSelected != _selectedSessionId.value) {
            _selectedSessionId.value = nextSelected
          }

          Triple(curProj, sessions, filtered)
        }
      }.collect { (project, sessions, filtered) ->
        _uiState.value = _uiState.value.copy(
          currentProject = project,
          allSessions = sessions,
          filteredSessions = filtered,
          selectedSessionId = _selectedSessionId.value,
          isLoading = false,
        )
      }
    }

    viewModelScope.launch {
      _selectedSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(Pair(null, emptyList()))
        else combine(
          repository.getSessionSummary(sessionId),
          repository.getSessionInventoryItems(sessionId)
        ) { summary, items ->
          Pair(summary, items)
        }
      }.collect { (summary, items) ->
        _uiState.value = _uiState.value.copy(
          selectedDaySummary = summary,
          selectedDayItems = items,
        )
      }
    }
  }

  fun filterByDateMillis(epochMillis: Long) {
    val dateString = DateHelper.formatEpochMillisToDateString(epochMillis)
    filterByDateString(dateString)
  }

  fun filterByDateString(dateString: String) {
    _calendarFilterDate.value = dateString
  }

  fun clearCalendarFilter() {
    _calendarFilterDate.value = null
  }

  fun selectSession(sessionId: Long) {
    _selectedSessionId.value = sessionId
    _uiState.value = _uiState.value.copy(selectedSessionId = sessionId)
  }

  fun openSessionForFilterDate(dateString: String) {
    viewModelScope.launch {
      try {
        val epochDay = DateHelper.parseDateStringToEpochDay(dateString)
        val newSession = repository.getOrCreateSessionForDate(dateString, epochDay)
        _calendarFilterDate.value = dateString
        _selectedSessionId.value = newSession.id
        _uiState.value = _uiState.value.copy(
          successMessage = "Jornada $dateString creada y seleccionada",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.message ?: "Error al crear jornada",
        )
      }
    }
  }

  fun generateCsvExport(): String {
    val summary = _uiState.value.selectedDaySummary
    val items = _uiState.value.selectedDayItems
    val session = _uiState.value.allSessions.find { it.id == _uiState.value.selectedSessionId }
    val dateStr = session?.dateString ?: "Reporte"

    val project = _uiState.value.currentProject
    val localName = project?.name ?: "Local Principal"
    val localType = project?.type ?: "GENERAL"

    val sb = StringBuilder()
    sb.appendLine("REPORTE DE VENTAS E INVENTARIO - $dateStr")
    sb.appendLine("Local: $localName ($localType)")
    sb.appendLine("Estado: ${session?.status ?: ""}")
    if (summary != null) {
      sb.appendLine("Ventas Totales: ${Product.formatMoney(summary.totalSalesCents)}")
      sb.appendLine("Costo Total: ${Product.formatMoney(summary.totalCostCents)}")
      sb.appendLine("Utilidad Total: ${Product.formatMoney(summary.totalProfitCents)}")
      sb.appendLine("Margen: ${String.format("%.2f", summary.marginPercentage)}%")
      sb.appendLine("Unidades Vendidas: ${summary.totalUnitsSold}")
      sb.appendLine("Roturas: ${summary.totalBreakage}")
    }
    sb.appendLine()
    sb.appendLine("PRODUCTO,CATEGORIA,UM,INICIO,ENTRADAS,DISPONIBLE,VENDIDO,PRECIO,IMPORTE_VENTA,COSTO_UNIT,IMPORTE_COSTO,UTILIDAD,FINAL,ROTURAS")
    for (item in items) {
      sb.append("\"${item.productName}\",")
      sb.append("\"${item.categoryName}\",")
      sb.append("\"${item.unitOfMeasure}\",")
      sb.append("${item.openingStock},")
      sb.append("${item.entries},")
      sb.append("${item.availableStock},")
      sb.append("${item.soldQuantity},")
      sb.append("${Product.formatMoney(item.salePriceCents)},")
      sb.append("${Product.formatMoney(item.saleAmountCents)},")
      sb.append("${Product.formatMoney(item.costPriceCents)},")
      sb.append("${Product.formatMoney(item.costAmountCents)},")
      sb.append("${Product.formatMoney(item.profitCents)},")
      sb.append("${item.closingStock},")
      sb.append("${item.breakage}\n")
    }
    val csv = sb.toString()
    _uiState.value = _uiState.value.copy(
      exportData = csv,
      successMessage = "Reporte CSV generado correctamente",
    )
    return csv
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(exportData = null, errorMessage = null, successMessage = null)
  }
}
