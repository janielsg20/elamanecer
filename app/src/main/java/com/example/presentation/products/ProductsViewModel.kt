package com.example.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProductsUiState(
  val isLoading: Boolean = true,
  val products: List<Product> = emptyList(),
  val filteredProducts: List<Product> = emptyList(),
  val categories: List<Category> = emptyList(),
  val selectedCategoryId: Long? = null,
  val searchQuery: String = "",
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

class ProductsViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProductsUiState())
  val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      observeData()
    }
  }

  private fun observeData() {
    viewModelScope.launch {
      combine(
        repository.getAllProducts(),
        repository.getAllCategories()
      ) { products, categories ->
        Pair(products, categories)
      }.collect { (products, categories) ->
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          products = products,
          categories = categories,
        )
        applyFilters()
      }
    }
  }

  fun setCategoryFilter(categoryId: Long?) {
    _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    applyFilters()
  }

  fun setSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
    applyFilters()
  }

  private fun applyFilters() {
    val products = _uiState.value.products
    val catId = _uiState.value.selectedCategoryId
    val query = _uiState.value.searchQuery.trim().lowercase()

    val filtered = products.filter { p ->
      val matchesCat = (catId == null || p.categoryId == catId)
      val matchesQuery = (query.isEmpty() || p.name.lowercase().contains(query))
      matchesCat && matchesQuery
    }
    _uiState.value = _uiState.value.copy(filteredProducts = filtered)
  }

  fun saveProduct(
    id: Long,
    categoryId: Long,
    name: String,
    unitOfMeasure: String,
    salePriceCents: Long,
    costPriceCents: Long,
    minStock: Double,
    maxStock: Double,
    active: Boolean = true,
  ) {
    if (name.isBlank()) {
      _uiState.value = _uiState.value.copy(errorMessage = "El nombre del producto no puede estar vacío")
      return
    }
    if (salePriceCents < 0 || costPriceCents < 0) {
      _uiState.value = _uiState.value.copy(errorMessage = "Los precios no pueden ser negativos")
      return
    }

    viewModelScope.launch {
      try {
        val product = Product(
          id = id,
          categoryId = categoryId,
          name = name.trim(),
          unitOfMeasure = unitOfMeasure.ifBlank { "U" },
          salePriceCents = salePriceCents,
          costPriceCents = costPriceCents,
          minStock = minStock,
          maxStock = maxStock,
          active = active,
        )
        if (id == 0L) {
          repository.insertProduct(product)
          _uiState.value = _uiState.value.copy(successMessage = "Producto creado exitosamente")
        } else {
          repository.updateProduct(product)
          _uiState.value = _uiState.value.copy(successMessage = "Producto actualizado")
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al guardar producto")
      }
    }
  }

  fun toggleProductActive(product: Product) {
    viewModelScope.launch {
      try {
        repository.updateProduct(product.copy(active = !product.active))
        val statusStr = if (!product.active) "activado" else "desactivado"
        _uiState.value = _uiState.value.copy(successMessage = "Producto $statusStr")
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al actualizar estado")
      }
    }
  }

  fun createCategory(name: String, description: String) {
    if (name.isBlank()) {
      _uiState.value = _uiState.value.copy(errorMessage = "El nombre de categoría es obligatorio")
      return
    }
    viewModelScope.launch {
      try {
        repository.insertCategory(Category(name = name.trim(), description = description.trim()))
        _uiState.value = _uiState.value.copy(successMessage = "Categoría creada")
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al crear categoría")
      }
    }
  }

  fun deleteProduct(product: Product) {
    viewModelScope.launch {
      try {
        repository.deleteProduct(product.id)
        _uiState.value = _uiState.value.copy(successMessage = "Producto '${product.name}' eliminado")
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al eliminar producto")
      }
    }
  }

  fun deleteCategory(categoryId: Long, categoryName: String) {
    viewModelScope.launch {
      try {
        repository.deleteCategory(categoryId)
        _uiState.value = _uiState.value.copy(
          successMessage = "Categoría '$categoryName' eliminada",
          selectedCategoryId = if (_uiState.value.selectedCategoryId == categoryId) null else _uiState.value.selectedCategoryId
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al eliminar categoría")
      }
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }
}
