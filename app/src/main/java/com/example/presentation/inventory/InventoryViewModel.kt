package com.example.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DaySummary
import com.example.domain.model.SessionStatus
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
  val isLoading: Boolean = true,
  val allSessions: List<DailySession> = emptyList(),
  val selectedSession: DailySession? = null,
  val categories: List<Category> = emptyList(),
  val selectedCategoryId: Long? = null,
  val searchQuery: String = "",
  val items: List<DailyInventoryItem> = emptyList(),
  val filteredItems: List<DailyInventoryItem> = emptyList(),
  val daySummary: DaySummary? = null,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(InventoryUiState())
  val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

  private val _selectedSessionId = MutableStateFlow<Long?>(null)
  private val _searchFilter = MutableStateFlow("")
  private val _categoryFilter = MutableStateFlow<Long?>(null)

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      loadInitialData()
    }
  }

  private fun loadInitialData() {
    viewModelScope.launch {
      combine(
        repository.getAllSessions(),
        repository.getActiveCategories()
      ) { sessions, categories ->
        val currentSelected = _selectedSessionId.value
        val targetSession = if (currentSelected != null) {
          sessions.find { it.id == currentSelected } ?: sessions.firstOrNull()
        } else {
          sessions.find { it.status == SessionStatus.OPEN } ?: sessions.firstOrNull()
        }

        if (targetSession != null && targetSession.id != _selectedSessionId.value) {
          _selectedSessionId.value = targetSession.id
        }

        Pair(sessions, categories)
      }.collect { (sessions, categories) ->
        _uiState.value = _uiState.value.copy(
          allSessions = sessions,
          categories = categories,
        )
      }
    }

    viewModelScope.launch {
      _selectedSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else repository.getSessionInventoryItems(sessionId)
      }.collect { items ->
        _uiState.value = _uiState.value.copy(
          items = items,
          isLoading = false,
        )
        applyFilters()
      }
    }

    viewModelScope.launch {
      _selectedSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(null)
        else repository.getSessionSummary(sessionId)
      }.collect { summary ->
        val currentSession = _uiState.value.allSessions.find { it.id == _selectedSessionId.value }
        _uiState.value = _uiState.value.copy(
          daySummary = summary,
          selectedSession = currentSession,
        )
      }
    }
  }

  fun selectSession(sessionId: Long) {
    _selectedSessionId.value = sessionId
    val session = _uiState.value.allSessions.find { it.id == sessionId }
    _uiState.value = _uiState.value.copy(selectedSession = session)
  }

  fun setCategoryFilter(categoryId: Long?) {
    _categoryFilter.value = categoryId
    _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    applyFilters()
  }

  fun setSearchQuery(query: String) {
    _searchFilter.value = query
    _uiState.value = _uiState.value.copy(searchQuery = query)
    applyFilters()
  }

  private fun applyFilters() {
    val currentItems = _uiState.value.items
    val query = _searchFilter.value.trim().lowercase()
    val catId = _categoryFilter.value

    val filtered = currentItems.filter { item ->
      val matchesCat = (catId == null || item.categoryId == catId)
      val matchesQuery = (query.isEmpty() || item.productName.lowercase().contains(query))
      matchesCat && matchesQuery
    }
    _uiState.value = _uiState.value.copy(filteredItems = filtered)
  }

  fun addEntries(productId: Long, quantity: Double) {
    val session = _uiState.value.selectedSession
    if (session == null || session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "No se pueden modificar sesiones cerradas")
      return
    }
    if (quantity <= 0.0) {
      _uiState.value = _uiState.value.copy(errorMessage = "La cantidad de entrada debe ser mayor a 0")
      return
    }

    viewModelScope.launch {
      try {
        repository.addEntries(session.id, productId, quantity)
        _uiState.value = _uiState.value.copy(
          successMessage = "Entrada de $quantity registrada correctamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al registrar entradas")
      }
    }
  }

  fun updateClosingAndBreakage(productId: Long, closingStock: Double, breakage: Double) {
    val session = _uiState.value.selectedSession
    if (session == null || session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "No se pueden modificar sesiones cerradas")
      return
    }
    if (closingStock < 0.0 || breakage < 0.0) {
      _uiState.value = _uiState.value.copy(errorMessage = "Los valores no pueden ser negativos")
      return
    }

    val item = _uiState.value.items.find { it.productId == productId }
    if (item != null) {
      val available = item.availableStock
      if (closingStock + breakage > available + 0.0001) {
        _uiState.value = _uiState.value.copy(
          errorMessage = "El inventario final ($closingStock) más roturas ($breakage) no puede ser mayor al disponible ($available)"
        )
        return
      }
    }

    viewModelScope.launch {
      try {
        repository.updateInventoryCount(session.id, productId, closingStock, breakage)
        _uiState.value = _uiState.value.copy(
          successMessage = "Inventario actualizado correctamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al actualizar inventario")
      }
    }
  }

  fun updateSoldQuantity(productId: Long, newSoldQuantity: Double) {
    val session = _uiState.value.selectedSession
    if (session == null || session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "No se pueden modificar sesiones cerradas")
      return
    }
    if (newSoldQuantity < 0.0) {
      _uiState.value = _uiState.value.copy(errorMessage = "La cantidad vendida no puede ser negativa")
      return
    }

    val item = _uiState.value.items.find { it.productId == productId }
    if (item != null && newSoldQuantity + item.breakage > item.availableStock + 0.0001) {
      _uiState.value = _uiState.value.copy(
        errorMessage = "La venta ($newSoldQuantity) más roturas (${item.breakage}) supera el disponible (${item.availableStock})"
      )
      return
    }

    viewModelScope.launch {
      try {
        repository.updateSoldQuantity(session.id, productId, newSoldQuantity)
        _uiState.value = _uiState.value.copy(
          successMessage = "Venta corregida a $newSoldQuantity unidades",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al corregir venta")
      }
    }
  }

  fun cancelSale(productId: Long) {
    val session = _uiState.value.selectedSession
    if (session == null || session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "No se pueden modificar sesiones cerradas")
      return
    }

    viewModelScope.launch {
      try {
        repository.cancelSale(session.id, productId)
        _uiState.value = _uiState.value.copy(
          successMessage = "Venta anulada y existencias restablecidas a 0 ventas",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al anular venta")
      }
    }
  }

  fun closeCurrentSession() {
    val session = _uiState.value.selectedSession ?: return
    if (session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "La sesión ya está cerrada")
      return
    }

    viewModelScope.launch {
      try {
        repository.closeSession(session.id)
        _uiState.value = _uiState.value.copy(
          successMessage = "Día ${session.dateString} cerrado exitosamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al cerrar el día")
      }
    }
  }

  fun createNewSession(dateString: String, epochDay: Long) {
    viewModelScope.launch {
      try {
        val newSession = repository.getOrCreateSessionForDate(dateString, epochDay)
        _selectedSessionId.value = newSession.id
        _uiState.value = _uiState.value.copy(
          successMessage = "Jornada para $dateString lista",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al crear la jornada")
      }
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }
}
