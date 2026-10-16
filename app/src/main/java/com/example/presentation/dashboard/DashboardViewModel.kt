package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CategorySummary
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DateHelper
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.model.SessionStatus
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class LowStockAlert(
  val product: Product,
  val currentStock: Double,
  val minStock: Double,
  val isOut: Boolean,
)

data class DashboardUiState(
  val isLoading: Boolean = true,
  val activeSession: DailySession? = null,
  val summary: DaySummary? = null,
  val items: List<DailyInventoryItem> = emptyList(),
  val lowStockAlerts: List<LowStockAlert> = emptyList(),
  val topSellingItems: List<DailyInventoryItem> = emptyList(),
  val categorySummaries: List<CategorySummary> = emptyList(),
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(DashboardUiState())
  val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      observeDashboardData()
    }
  }

  private fun observeDashboardData() {
    viewModelScope.launch {
      // Prioritize open session; if none is open, show the latest closed session
      combine(
        repository.getLatestOpenSession(),
        repository.getLatestSession(),
      ) { openSession, latestSession ->
        openSession ?: latestSession
      }.flatMapLatest { session ->
        if (session == null) {
          flowOf(Triple<DailySession?, DaySummary?, Pair<List<DailyInventoryItem>, List<Product>>>(null, null, Pair(emptyList(), emptyList())))
        } else {
          combine(
            repository.getSessionSummary(session.id),
            repository.getSessionInventoryItems(session.id),
            repository.getActiveProducts()
          ) { summary, items, products ->
            Triple(session, summary, Pair(items, products))
          }
        }
      }.collect { (session, summary, itemsAndProducts) ->
        val (items, products) = itemsAndProducts
        if (session == null) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            activeSession = null,
            summary = null,
            items = emptyList(),
            categorySummaries = emptyList(),
            topSellingItems = emptyList(),
            lowStockAlerts = emptyList(),
          )
          return@collect
        }

        val productMap = products.associateBy { it.id }
        val alerts = items.mapNotNull { item ->
          val p = productMap[item.productId]
          if (p != null && p.minStock > 0 && item.closingStock <= p.minStock) {
            LowStockAlert(
              product = p,
              currentStock = item.closingStock,
              minStock = p.minStock,
              isOut = item.closingStock <= 0.0,
            )
          } else null
        }

        val topSelling = items.filter { it.soldQuantity > 0 }
          .sortedByDescending { it.saleAmountCents }
          .take(5)

        _uiState.value = _uiState.value.copy(
          isLoading = false,
          activeSession = session,
          summary = summary,
          items = items,
          lowStockAlerts = alerts,
          topSellingItems = topSelling,
          categorySummaries = summary?.categorySummaries ?: emptyList(),
        )
      }
    }
  }

  fun openTodaySession() {
    val todayStr = DateHelper.getTodayDateString()
    val todayEpoch = DateHelper.getTodayEpochDay()
    openSession(todayStr, todayEpoch)
  }

  fun openSessionFromDateMillis(epochMillis: Long) {
    val dateStr = DateHelper.formatEpochMillisToDateString(epochMillis)
    val epochDay = DateHelper.epochMillisToEpochDay(epochMillis)
    openSession(dateStr, epochDay)
  }

  fun openSession(dateString: String, epochDay: Long) {
    viewModelScope.launch {
      try {
        val existing = repository.getSessionByDate(dateString)
        if (existing != null) {
          if (existing.status == SessionStatus.CLOSED) {
            repository.reopenSession(existing.id)
            _uiState.value = _uiState.value.copy(
              successMessage = "Jornada $dateString reabierta exitosamente",
              errorMessage = null,
            )
          } else {
            _uiState.value = _uiState.value.copy(
              successMessage = "Jornada $dateString activa",
              errorMessage = null,
            )
          }
        } else {
          val session = repository.getOrCreateSessionForDate(dateString, epochDay)
          _uiState.value = _uiState.value.copy(
            successMessage = "Jornada $dateString abierta exitosamente",
            errorMessage = null,
          )
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.message ?: "Error al abrir jornada",
        )
      }
    }
  }

  fun closeActiveSession() {
    val active = _uiState.value.activeSession ?: return
    viewModelScope.launch {
      try {
        repository.closeSession(active.id)
        _uiState.value = _uiState.value.copy(
          successMessage = "Jornada ${active.dateString} cerrada con éxito",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.message ?: "Error al cerrar jornada",
        )
      }
    }
  }

  fun reopenActiveSession() {
    val active = _uiState.value.activeSession ?: return
    viewModelScope.launch {
      try {
        repository.reopenSession(active.id)
        _uiState.value = _uiState.value.copy(
          successMessage = "Jornada ${active.dateString} reabierta",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.message ?: "Error al reabrir jornada",
        )
      }
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
  }
}
