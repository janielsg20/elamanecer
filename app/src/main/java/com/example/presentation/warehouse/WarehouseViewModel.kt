package com.example.presentation.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Project
import com.example.domain.model.Warehouse
import com.example.domain.model.WarehouseDispatch
import com.example.domain.model.WarehouseStockItem
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class WarehouseUiState(
  val isLoading: Boolean = true,
  val warehouses: List<Warehouse> = emptyList(),
  val selectedWarehouse: Warehouse? = null,
  val stockItems: List<WarehouseStockItem> = emptyList(),
  val filteredStockItems: List<WarehouseStockItem> = emptyList(),
  val projects: List<Project> = emptyList(),
  val products: List<Product> = emptyList(),
  val dispatches: List<WarehouseDispatch> = emptyList(),
  val searchQuery: String = "",
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WarehouseViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(WarehouseUiState())
  val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()

  private val _selectedWarehouseId = MutableStateFlow<Long?>(null)
  private val _searchFilter = MutableStateFlow("")

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      observeWarehousesAndMetadata()
      observeWarehouseStock()
      observeDispatches()
    }
  }

  private fun observeWarehousesAndMetadata() {
    viewModelScope.launch {
      combine(
        repository.getAllWarehouses(),
        repository.getAllProjects(),
        repository.getActiveProducts()
      ) { warehouses, projects, products ->
        val currentId = _selectedWarehouseId.value
        val selected = if (currentId != null) {
          warehouses.find { it.id == currentId } ?: warehouses.firstOrNull()
        } else {
          warehouses.firstOrNull()
        }

        if (selected != null && selected.id != _selectedWarehouseId.value) {
          _selectedWarehouseId.value = selected.id
        }

        Triple(warehouses, projects, products)
      }.collect { (warehouses, projects, products) ->
        val selected = warehouses.find { it.id == _selectedWarehouseId.value }
        _uiState.value = _uiState.value.copy(
          warehouses = warehouses,
          selectedWarehouse = selected,
          projects = projects,
          products = products,
        )
      }
    }
  }

  private fun observeWarehouseStock() {
    viewModelScope.launch {
      combine(
        _selectedWarehouseId.flatMapLatest { id ->
          if (id == null) flowOf(emptyList())
          else repository.getWarehouseStock(id)
        },
        _searchFilter
      ) { items, query ->
        val filtered = if (query.isBlank()) {
          items
        } else {
          items.filter {
            it.productName.contains(query, ignoreCase = true) ||
              it.categoryName.contains(query, ignoreCase = true)
          }
        }
        Pair(items, filtered)
      }.collect { (items, filtered) ->
        _uiState.value = _uiState.value.copy(
          stockItems = items,
          filteredStockItems = filtered,
          isLoading = false,
        )
      }
    }
  }

  private fun observeDispatches() {
    viewModelScope.launch {
      repository.getAllDispatches().collect { dispatches ->
        _uiState.value = _uiState.value.copy(dispatches = dispatches)
      }
    }
  }

  fun selectWarehouse(warehouseId: Long) {
    _selectedWarehouseId.value = warehouseId
    val warehouse = _uiState.value.warehouses.find { it.id == warehouseId }
    _uiState.value = _uiState.value.copy(
      selectedWarehouse = warehouse,
    )
  }

  fun setSearchQuery(query: String) {
    _searchFilter.value = query
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun createWarehouse(name: String, targetType: String, location: String, notes: String) {
    viewModelScope.launch {
      try {
        if (name.isBlank()) {
          _uiState.value = _uiState.value.copy(errorMessage = "El nombre del almacén es obligatorio")
          return@launch
        }
        val newWarehouse = Warehouse(
          name = name.trim(),
          targetType = targetType,
          location = location.trim(),
          notes = notes.trim(),
        )
        val id = repository.insertWarehouse(newWarehouse)
        _selectedWarehouseId.value = id
        _uiState.value = _uiState.value.copy(
          successMessage = "Almacén \"$name\" creado exitosamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al crear almacén")
      }
    }
  }

  fun updateWarehouse(id: Long, name: String, targetType: String, location: String, notes: String) {
    viewModelScope.launch {
      try {
        if (name.isBlank()) {
          _uiState.value = _uiState.value.copy(errorMessage = "El nombre del almacén es obligatorio")
          return@launch
        }
        val existing = repository.getWarehouseById(id) ?: return@launch
        repository.updateWarehouse(
          existing.copy(
            name = name.trim(),
            targetType = targetType,
            location = location.trim(),
            notes = notes.trim(),
          )
        )
        _uiState.value = _uiState.value.copy(
          successMessage = "Almacén actualizado correctamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al actualizar almacén")
      }
    }
  }

  fun deleteWarehouse(warehouseId: Long) {
    viewModelScope.launch {
      try {
        if (_uiState.value.warehouses.size <= 1) {
          _uiState.value = _uiState.value.copy(errorMessage = "No se puede eliminar el único almacén disponible")
          return@launch
        }
        val warehouse = _uiState.value.warehouses.find { it.id == warehouseId }
        repository.deleteWarehouse(warehouseId)
        val remaining = _uiState.value.warehouses.filter { it.id != warehouseId }
        _selectedWarehouseId.value = remaining.firstOrNull()?.id
        _uiState.value = _uiState.value.copy(
          successMessage = "Almacén \"${warehouse?.name ?: ""}\" eliminado",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al eliminar almacén")
      }
    }
  }

  fun addStock(warehouseId: Long, productId: Long, quantity: Double, minAlert: Double? = null) {
    viewModelScope.launch {
      try {
        if (quantity <= 0) {
          _uiState.value = _uiState.value.copy(errorMessage = "La cantidad a añadir debe ser mayor a 0")
          return@launch
        }
        repository.addWarehouseStock(warehouseId, productId, quantity, minAlert)
        val product = _uiState.value.products.find { it.id == productId }
        _uiState.value = _uiState.value.copy(
          successMessage = "Se agregaron $quantity unidades de \"${product?.name ?: "Producto"}\" al almacén",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al reabastecer stock")
      }
    }
  }

  fun updateStock(warehouseId: Long, productId: Long, newStock: Double, minAlert: Double? = null) {
    viewModelScope.launch {
      try {
        if (newStock < 0) {
          _uiState.value = _uiState.value.copy(errorMessage = "El stock no puede ser negativo")
          return@launch
        }
        repository.updateWarehouseStock(warehouseId, productId, newStock, minAlert)
        val product = _uiState.value.products.find { it.id == productId }
        _uiState.value = _uiState.value.copy(
          successMessage = "Stock de \"${product?.name ?: "Producto"}\" ajustado a $newStock",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al actualizar stock")
      }
    }
  }

  fun dispatchStock(
    warehouseId: Long,
    projectId: Long,
    productId: Long,
    quantity: Double,
    notes: String = "",
  ) {
    viewModelScope.launch {
      try {
        if (quantity <= 0) {
          _uiState.value = _uiState.value.copy(errorMessage = "La cantidad a despachar debe ser mayor que 0")
          return@launch
        }
        val dispatch = repository.dispatchStockToProject(
          warehouseId = warehouseId,
          projectId = projectId,
          productId = productId,
          quantity = quantity,
          notes = notes,
        )
        _uiState.value = _uiState.value.copy(
          successMessage = "¡Despacho completado! Salida: $quantity ${dispatch.unitOfMeasure} de ${dispatch.productName} para ${dispatch.projectName}. Quedan ${dispatch.remainingStockInWarehouse} en almacén.",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.message ?: "Error al procesar el despacho de almacén",
        )
      }
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
  }
}
