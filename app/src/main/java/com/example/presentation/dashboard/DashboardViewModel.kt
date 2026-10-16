package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CategorySummary
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DateHelper
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.model.Project
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
  val projects: List<Project> = emptyList(),
  val currentProject: Project? = null,
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
      observeProjects()
      observeDashboardData()
    }
  }

  private fun observeProjects() {
    viewModelScope.launch {
      combine(
        repository.getAllProjects(),
        repository.getSelectedProjectId()
      ) { projects, selectedId ->
        val current = projects.find { it.id == selectedId } ?: projects.firstOrNull()
        Pair(projects, current)
      }.collect { (projects, current) ->
        _uiState.value = _uiState.value.copy(
          projects = projects,
          currentProject = current,
        )
      }
    }
  }

  private fun observeDashboardData() {
    viewModelScope.launch {
      // Re-evalúa los datos cuando cambia la sesión o el proyecto
      repository.getSelectedProjectId().flatMapLatest { projectId ->
        combine(
          repository.getLatestOpenSession(projectId),
          repository.getLatestSession(projectId),
        ) { openSession, latestSession ->
          openSession ?: latestSession
        }
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

  fun selectProject(projectId: Long) {
    viewModelScope.launch {
      repository.setSelectedProjectId(projectId)
      val project = _uiState.value.projects.find { it.id == projectId }
      _uiState.value = _uiState.value.copy(
        currentProject = project,
        successMessage = "Local activo cambiado a: ${project?.name ?: "Local $projectId"}",
      )
    }
  }

  fun createProject(name: String, type: String, description: String, colorHex: String) {
    viewModelScope.launch {
      try {
        if (name.isBlank()) {
          _uiState.value = _uiState.value.copy(errorMessage = "El nombre del local no puede estar vacío")
          return@launch
        }
        val newProj = Project(
          name = name.trim(),
          type = type,
          description = description.trim(),
          colorHex = colorHex,
        )
        val newId = repository.insertProject(newProj)
        repository.setSelectedProjectId(newId)
        _uiState.value = _uiState.value.copy(
          successMessage = "Local \"$name\" creado exitosamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al crear local")
      }
    }
  }

  fun updateProject(id: Long, name: String, type: String, description: String, colorHex: String) {
    viewModelScope.launch {
      try {
        if (name.isBlank()) {
          _uiState.value = _uiState.value.copy(errorMessage = "El nombre del local no puede estar vacío")
          return@launch
        }
        val existing = repository.getProjectById(id) ?: return@launch
        val updated = existing.copy(
          name = name.trim(),
          type = type,
          description = description.trim(),
          colorHex = colorHex,
        )
        repository.updateProject(updated)
        _uiState.value = _uiState.value.copy(
          successMessage = "Local \"$name\" actualizado exitosamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al actualizar local")
      }
    }
  }

  fun deleteProject(projectId: Long) {
    viewModelScope.launch {
      try {
        val totalProjects = _uiState.value.projects.size
        if (totalProjects <= 1) {
          _uiState.value = _uiState.value.copy(errorMessage = "No se puede eliminar el único local existente")
          return@launch
        }
        val toDelete = _uiState.value.projects.find { it.id == projectId }
        repository.deleteProject(projectId)
        _uiState.value = _uiState.value.copy(
          successMessage = "Local \"${toDelete?.name ?: ""}\" eliminado",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al eliminar local")
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
        val currentProjId = _uiState.value.currentProject?.id ?: 1L
        val existing = repository.getSessionByDate(dateString, currentProjId)
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
          repository.getOrCreateSessionForDate(dateString, epochDay, currentProjId)
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
