package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.model.Project
import com.example.domain.model.Warehouse
import com.example.domain.model.WarehouseDispatch
import com.example.domain.model.WarehouseStockItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
  // Projects
  fun getAllProjects(): Flow<List<Project>>
  fun getActiveProjects(): Flow<List<Project>>
  suspend fun getProjectById(id: Long): Project?
  suspend fun insertProject(project: Project): Long
  suspend fun updateProject(project: Project)
  suspend fun deleteProject(projectId: Long)
  fun getSelectedProjectId(): Flow<Long>
  suspend fun setSelectedProjectId(projectId: Long)

  // Warehouses
  fun getAllWarehouses(): Flow<List<Warehouse>>
  fun getActiveWarehouses(): Flow<List<Warehouse>>
  suspend fun getWarehouseById(id: Long): Warehouse?
  suspend fun insertWarehouse(warehouse: Warehouse): Long
  suspend fun updateWarehouse(warehouse: Warehouse)
  suspend fun deleteWarehouse(warehouseId: Long)

  // Warehouse Stock
  fun getWarehouseStock(warehouseId: Long): Flow<List<WarehouseStockItem>>
  suspend fun saveWarehouseStockItem(
    warehouseId: Long,
    productId: Long?,
    productName: String,
    unitOfMeasure: String,
    initialStock: Double,
    entries: Double,
    exits: Double,
    costPrice: Double,
    minAlertStock: Double? = null,
  ): Long
  suspend fun addWarehouseStockEntry(
    warehouseId: Long,
    productId: Long,
    quantity: Double,
  )
  suspend fun addWarehouseStockExit(
    warehouseId: Long,
    productId: Long,
    quantity: Double,
  )
  suspend fun deleteWarehouseStockItem(
    warehouseId: Long,
    productId: Long,
  )
  suspend fun addWarehouseStock(
    warehouseId: Long,
    productId: Long,
    additionalQuantity: Double,
    minAlert: Double? = null,
  )
  suspend fun updateWarehouseStock(
    warehouseId: Long,
    productId: Long,
    newStock: Double,
    minAlert: Double? = null,
  )

  // Warehouse Dispatches (Stock Transfers to Projects)
  suspend fun dispatchStockToProject(
    warehouseId: Long,
    projectId: Long,
    productId: Long,
    quantity: Double,
    notes: String = "",
  ): WarehouseDispatch
  fun getAllDispatches(): Flow<List<WarehouseDispatch>>
  fun getDispatchesForWarehouse(warehouseId: Long): Flow<List<WarehouseDispatch>>

  // Categories
  fun getAllCategories(): Flow<List<Category>>
  fun getActiveCategories(): Flow<List<Category>>
  suspend fun insertCategory(category: Category): Long
  suspend fun updateCategory(category: Category)
  suspend fun deleteCategory(categoryId: Long)
  suspend fun countProductsInCategory(categoryId: Long): Int

  // Products
  fun getAllProducts(): Flow<List<Product>>
  fun getActiveProducts(): Flow<List<Product>>
  suspend fun getProductById(id: Long): Product?
  suspend fun insertProduct(product: Product): Long
  suspend fun updateProduct(product: Product)
  suspend fun deleteProduct(productId: Long)

  // Daily Sessions (Scoped to Project)
  fun getAllSessions(projectId: Long? = null): Flow<List<DailySession>>
  fun getLatestOpenSession(projectId: Long? = null): Flow<DailySession?>
  fun getLatestSession(projectId: Long? = null): Flow<DailySession?>
  suspend fun getSessionById(sessionId: Long): DailySession?
  suspend fun getSessionByDate(dateString: String, projectId: Long? = null): DailySession?
  suspend fun getOrCreateSessionForDate(
    dateString: String,
    dateEpochDay: Long,
    projectId: Long? = null,
  ): DailySession
  suspend fun closeSession(sessionId: Long)
  suspend fun reopenSession(sessionId: Long)

  // Daily Inventory Operations
  fun getSessionInventoryItems(sessionId: Long): Flow<List<DailyInventoryItem>>
  fun getSessionSummary(sessionId: Long): Flow<DaySummary?>
  suspend fun updateInventoryCount(
    sessionId: Long,
    productId: Long,
    closingStock: Double,
    breakage: Double,
  )
  suspend fun addEntries(
    sessionId: Long,
    productId: Long,
    additionalEntries: Double,
  )
  suspend fun registerQuickSale(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  )
  suspend fun updateSoldQuantity(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  )
  suspend fun cancelSale(
    sessionId: Long,
    productId: Long,
  )

  // Seeding
  suspend fun seedInitialDataIfEmpty()
}
