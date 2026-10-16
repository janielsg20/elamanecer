package com.example.presentation.quicksale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.SessionStatus
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.domain.model.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

data class QuickSaleCartItem(
  val item: DailyInventoryItem,
  val quantity: Double,
) {
  val subtotalCents: Long
    get() = (quantity * item.salePriceCents).toLong()
}

data class QuickSaleUiState(
  val isLoading: Boolean = true,
  val currentProject: Project? = null,
  val activeSession: DailySession? = null,
  val categories: List<Category> = emptyList(),
  val selectedCategoryId: Long? = null,
  val searchQuery: String = "",
  val availableItems: List<DailyInventoryItem> = emptyList(),
  val filteredItems: List<DailyInventoryItem> = emptyList(),
  val cart: Map<Long, QuickSaleCartItem> = emptyMap(),
  val totalCartCents: Long = 0L,
  val totalCartUnits: Double = 0.0,
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class QuickSaleViewModel(
  private val repository: InventoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(QuickSaleUiState())
  val uiState: StateFlow<QuickSaleUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      ensureOpenSessionAndObserve()
    }
  }

  private fun ensureOpenSessionAndObserve() {
    viewModelScope.launch {
      repository.getSelectedProjectId().flatMapLatest { projectId ->
        val project = repository.getProjectById(projectId)
        combine(
          repository.getLatestOpenSession(projectId),
          repository.getAllProjects(),
        ) { openSession, allProjects ->
          val curProj = allProjects.find { it.id == projectId } ?: project
          Pair(curProj, openSession)
        }
      }.flatMapLatest { (project, openSession) ->
        val session = openSession ?: run {
          val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
          val todayStr = sdf.format(Date())
          val epochDay = System.currentTimeMillis() / 86400000L
          val projId = project?.id ?: 1L
          repository.getOrCreateSessionForDate(todayStr, epochDay, projId)
        }

        combine(
          repository.getSessionInventoryItems(session.id),
          repository.getActiveCategories()
        ) { items, categories ->
          Triple(project, session, Pair(items, categories))
        }
      }.collect { (project, session, itemsAndCategories) ->
        val (items, categories) = itemsAndCategories
        val isNewSession = _uiState.value.activeSession?.id != session.id
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          currentProject = project,
          activeSession = session,
          categories = categories,
          availableItems = items,
          cart = if (isNewSession) emptyMap() else _uiState.value.cart,
          totalCartCents = if (isNewSession) 0L else _uiState.value.totalCartCents,
          totalCartUnits = if (isNewSession) 0.0 else _uiState.value.totalCartUnits,
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
    val items = _uiState.value.availableItems
    val catId = _uiState.value.selectedCategoryId
    val query = _uiState.value.searchQuery.trim().lowercase()

    val filtered = items.filter { item ->
      val matchesCat = (catId == null || item.categoryId == catId)
      val matchesQuery = (query.isEmpty() || item.productName.lowercase().contains(query))
      matchesCat && matchesQuery
    }
    _uiState.value = _uiState.value.copy(filteredItems = filtered)
  }

  fun addToCart(item: DailyInventoryItem, quantityToAdd: Double = 1.0) {
    val currentCart = _uiState.value.cart.toMutableMap()
    val existing = currentCart[item.productId]
    val currentQty = existing?.quantity ?: 0.0
    val newQty = currentQty + quantityToAdd

    if (newQty > item.closingStock) {
      _uiState.value = _uiState.value.copy(
        errorMessage = "Stock insuficiente de ${item.productName}. Disponible: ${item.closingStock}"
      )
      return
    }

    currentCart[item.productId] = QuickSaleCartItem(item, newQty)
    updateCartTotals(currentCart)
  }

  fun removeFromCart(productId: Long) {
    val currentCart = _uiState.value.cart.toMutableMap()
    val existing = currentCart[productId] ?: return
    if (existing.quantity > 1.0) {
      currentCart[productId] = existing.copy(quantity = existing.quantity - 1.0)
    } else {
      currentCart.remove(productId)
    }
    updateCartTotals(currentCart)
  }

  fun setCartQuantity(item: DailyInventoryItem, quantity: Double) {
    if (quantity < 0.0) {
      _uiState.value = _uiState.value.copy(errorMessage = "La cantidad no puede ser negativa")
      return
    }
    val currentCart = _uiState.value.cart.toMutableMap()
    if (quantity == 0.0) {
      currentCart.remove(item.productId)
      updateCartTotals(currentCart)
      return
    }
    if (quantity > item.closingStock) {
      _uiState.value = _uiState.value.copy(
        errorMessage = "Stock insuficiente de ${item.productName}. Disponible: ${item.closingStock} ${item.unitOfMeasure}"
      )
      return
    }
    currentCart[item.productId] = QuickSaleCartItem(item, quantity)
    updateCartTotals(currentCart)
  }

  fun clearCart() {
    updateCartTotals(emptyMap())
  }

  private fun updateCartTotals(cart: Map<Long, QuickSaleCartItem>) {
    val totalCents = cart.values.sumOf { it.subtotalCents }
    val totalUnits = cart.values.sumOf { it.quantity }
    _uiState.value = _uiState.value.copy(
      cart = cart,
      totalCartCents = totalCents,
      totalCartUnits = totalUnits,
      errorMessage = null,
    )
  }

  fun commitSale() {
    val session = _uiState.value.activeSession
    if (session == null || session.status == SessionStatus.CLOSED) {
      _uiState.value = _uiState.value.copy(errorMessage = "No hay sesión abierta para registrar ventas")
      return
    }

    val cartItems = _uiState.value.cart.values.toList()
    if (cartItems.isEmpty()) {
      _uiState.value = _uiState.value.copy(errorMessage = "El carrito de ventas está vacío")
      return
    }

    viewModelScope.launch {
      try {
        for (cartItem in cartItems) {
          repository.registerQuickSale(session.id, cartItem.item.productId, cartItem.quantity)
        }
        val totalFormatted = com.example.domain.model.Product.formatMoney(_uiState.value.totalCartCents)
        clearCart()
        _uiState.value = _uiState.value.copy(
          successMessage = "Venta por $totalFormatted registrada exitosamente",
          errorMessage = null,
        )
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al registrar la venta")
      }
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }
}
