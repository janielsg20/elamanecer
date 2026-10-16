package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DailyInventoryEntity
import com.example.data.local.entity.DailySessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.WarehouseDispatchEntity
import com.example.data.local.entity.WarehouseEntity
import com.example.data.local.entity.WarehouseStockEntity
import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DateHelper
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.model.Project
import com.example.domain.model.SessionStatus
import com.example.domain.model.Warehouse
import com.example.domain.model.WarehouseDispatch
import com.example.domain.model.WarehouseStockItem
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InventoryRepositoryImpl(
  private val database: AppDatabase,
) : InventoryRepository {

  private val projectDao = database.projectDao()
  private val warehouseDao = database.warehouseDao()
  private val categoryDao = database.categoryDao()
  private val productDao = database.productDao()
  private val sessionDao = database.dailySessionDao()
  private val inventoryDao = database.dailyInventoryDao()

  private val selectedProjectIdFlow = MutableStateFlow(1L)

  // -------------------------------------------------------------
  // Projects
  // -------------------------------------------------------------
  override fun getAllProjects(): Flow<List<Project>> {
    return projectDao.getAllProjects().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override fun getActiveProjects(): Flow<List<Project>> {
    return projectDao.getAllActiveProjects().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun getProjectById(id: Long): Project? = withContext(Dispatchers.IO) {
    projectDao.getProjectById(id)?.toDomain()
  }

  override suspend fun insertProject(project: Project): Long = withContext(Dispatchers.IO) {
    projectDao.insert(project.toEntity())
  }

  override suspend fun updateProject(project: Project) = withContext(Dispatchers.IO) {
    projectDao.update(project.toEntity())
  }

  override suspend fun deleteProject(projectId: Long) = withContext(Dispatchers.IO) {
    projectDao.deleteProjectById(projectId)
    if (selectedProjectIdFlow.value == projectId) {
      val firstRemaining = projectDao.getProjectById(1)?.id ?: 1L
      selectedProjectIdFlow.value = firstRemaining
    }
  }

  override fun getSelectedProjectId(): Flow<Long> = selectedProjectIdFlow.asStateFlow()

  override suspend fun setSelectedProjectId(projectId: Long) {
    selectedProjectIdFlow.value = projectId
  }

  // -------------------------------------------------------------
  // Warehouses
  // -------------------------------------------------------------
  override fun getAllWarehouses(): Flow<List<Warehouse>> {
    return warehouseDao.getAllWarehouses().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override fun getActiveWarehouses(): Flow<List<Warehouse>> {
    return warehouseDao.getAllActiveWarehouses().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun getWarehouseById(id: Long): Warehouse? = withContext(Dispatchers.IO) {
    warehouseDao.getWarehouseById(id)?.toDomain()
  }

  override suspend fun insertWarehouse(warehouse: Warehouse): Long = withContext(Dispatchers.IO) {
    val warehouseId = warehouseDao.insert(warehouse.toEntity())
    // Inicializar inventario en cero para todos los productos existentes
    val products = productDao.getActiveProducts()
    val allActive = database.openHelper.readableDatabase
    val cursor = allActive.query("SELECT id FROM products WHERE active = 1")
    val stocks = mutableListOf<WarehouseStockEntity>()
    if (cursor.moveToFirst()) {
      do {
        stocks.add(
          WarehouseStockEntity(
            warehouseId = warehouseId,
            productId = cursor.getLong(0),
            currentStock = 0.0,
            minAlertStock = 10.0,
            updatedAt = System.currentTimeMillis(),
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
    if (stocks.isNotEmpty()) {
      warehouseDao.insertAllStock(stocks)
    }
    warehouseId
  }

  override suspend fun updateWarehouse(warehouse: Warehouse) = withContext(Dispatchers.IO) {
    warehouseDao.update(warehouse.toEntity())
  }

  override suspend fun deleteWarehouse(warehouseId: Long) = withContext(Dispatchers.IO) {
    warehouseDao.deleteWarehouseById(warehouseId)
  }

  // -------------------------------------------------------------
  // Warehouse Stock
  // -------------------------------------------------------------
  override fun getWarehouseStock(warehouseId: Long): Flow<List<WarehouseStockItem>> {
    return warehouseDao.getStockForWarehouse(warehouseId).map { queryResults ->
      queryResults.map { qr ->
        WarehouseStockItem(
          id = qr.id,
          warehouseId = qr.warehouseId,
          productId = qr.productId,
          productName = qr.productName,
          categoryName = qr.categoryName,
          unitOfMeasure = qr.unitOfMeasure,
          costPriceCents = qr.costPriceCents,
          salePriceCents = qr.salePriceCents,
          currentStock = qr.currentStock,
          minAlertStock = qr.minAlertStock,
          updatedAt = qr.updatedAt,
        )
      }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun addWarehouseStock(
    warehouseId: Long,
    productId: Long,
    additionalQuantity: Double,
    minAlert: Double?,
  ): Unit = withContext(Dispatchers.IO) {
    require(additionalQuantity >= 0) { "La cantidad agregada no puede ser negativa" }
    val existing = warehouseDao.getStockItem(warehouseId, productId)
    if (existing != null) {
      val newStock = existing.currentStock + additionalQuantity
      val updatedAlert = minAlert ?: existing.minAlertStock
      warehouseDao.updateStock(
        existing.copy(
          currentStock = newStock,
          minAlertStock = updatedAlert,
          updatedAt = System.currentTimeMillis(),
        )
      )
    } else {
      warehouseDao.insertStock(
        WarehouseStockEntity(
          warehouseId = warehouseId,
          productId = productId,
          currentStock = additionalQuantity,
          minAlertStock = minAlert ?: 10.0,
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  override suspend fun updateWarehouseStock(
    warehouseId: Long,
    productId: Long,
    newStock: Double,
    minAlert: Double?,
  ): Unit = withContext(Dispatchers.IO) {
    require(newStock >= 0) { "El stock no puede ser negativo" }
    val existing = warehouseDao.getStockItem(warehouseId, productId)
    if (existing != null) {
      val updatedAlert = minAlert ?: existing.minAlertStock
      warehouseDao.updateStock(
        existing.copy(
          currentStock = newStock,
          minAlertStock = updatedAlert,
          updatedAt = System.currentTimeMillis(),
        )
      )
    } else {
      warehouseDao.insertStock(
        WarehouseStockEntity(
          warehouseId = warehouseId,
          productId = productId,
          currentStock = newStock,
          minAlertStock = minAlert ?: 10.0,
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  // -------------------------------------------------------------
  // Warehouse Dispatches (Stock transfers to Projects)
  // -------------------------------------------------------------
  override suspend fun dispatchStockToProject(
    warehouseId: Long,
    projectId: Long,
    productId: Long,
    quantity: Double,
    notes: String,
  ): WarehouseDispatch = withContext(Dispatchers.IO) {
    require(quantity > 0) { "La cantidad a despachar debe ser mayor que cero" }
    val stockItem = warehouseDao.getStockItem(warehouseId, productId)
      ?: throw IllegalArgumentException("El producto no está registrado en el almacén seleccionado")

    if (stockItem.currentStock < quantity) {
      throw IllegalArgumentException(
        "Stock insuficiente en almacén. Disponible: ${stockItem.currentStock}, Solicitado: $quantity"
      )
    }

    val remaining = stockItem.currentStock - quantity
    warehouseDao.updateStockQuantity(warehouseId, productId, remaining, System.currentTimeMillis())

    val todayStr = DateHelper.getTodayDateString()
    val todayEpoch = DateHelper.getTodayEpochDay()

    // Obtener o crear la sesión activa para el proyecto de destino
    val session = getOrCreateSessionForDate(todayStr, todayEpoch, projectId)

    // Sumar a las entradas de este producto en la sesión diaria del proyecto
    addEntries(session.id, productId, quantity)

    val dispatchEntity = WarehouseDispatchEntity(
      warehouseId = warehouseId,
      projectId = projectId,
      productId = productId,
      quantity = quantity,
      remainingStockInWarehouse = remaining,
      dateString = todayStr,
      timestamp = System.currentTimeMillis(),
      notes = notes,
    )
    val dispatchId = warehouseDao.insertDispatch(dispatchEntity)

    val warehouse = warehouseDao.getWarehouseById(warehouseId)
    val project = projectDao.getProjectById(projectId)
    val product = productDao.getProductById(productId)

    WarehouseDispatch(
      id = dispatchId,
      warehouseId = warehouseId,
      warehouseName = warehouse?.name ?: "Almacén",
      projectId = projectId,
      projectName = project?.name ?: "Local",
      productId = productId,
      productName = product?.name ?: "Producto",
      unitOfMeasure = product?.unitOfMeasure ?: "U",
      quantity = quantity,
      remainingStockInWarehouse = remaining,
      dateString = todayStr,
      timestamp = dispatchEntity.timestamp,
      notes = notes,
    )
  }

  override fun getAllDispatches(): Flow<List<WarehouseDispatch>> {
    return warehouseDao.getAllDispatches().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override fun getDispatchesForWarehouse(warehouseId: Long): Flow<List<WarehouseDispatch>> {
    return warehouseDao.getDispatchesForWarehouse(warehouseId).map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  // -------------------------------------------------------------
  // Categories & Products
  // -------------------------------------------------------------
  override fun getAllCategories(): Flow<List<Category>> {
    return categoryDao.getAllCategories().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override fun getActiveCategories(): Flow<List<Category>> {
    return categoryDao.getActiveCategories().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun insertCategory(category: Category): Long = withContext(Dispatchers.IO) {
    categoryDao.insert(category.toEntity())
  }

  override suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
    categoryDao.update(category.toEntity())
  }

  override suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
    categoryDao.deleteCategoryById(categoryId)
  }

  override suspend fun countProductsInCategory(categoryId: Long): Int = withContext(Dispatchers.IO) {
    categoryDao.countProductsInCategory(categoryId)
  }

  override fun getAllProducts(): Flow<List<Product>> {
    return combine(productDao.getAllProducts(), categoryDao.getAllCategories()) { products, categories ->
      val catMap = categories.associate { it.id to it.name }
      products.map { it.toDomain(catMap[it.categoryId] ?: "") }
    }.flowOn(Dispatchers.IO)
  }

  override fun getActiveProducts(): Flow<List<Product>> {
    return combine(productDao.getActiveProducts(), categoryDao.getAllCategories()) { products, categories ->
      val catMap = categories.associate { it.id to it.name }
      products.map { it.toDomain(catMap[it.categoryId] ?: "") }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
    val entity = productDao.getProductById(id) ?: return@withContext null
    val cat = categoryDao.getCategoryById(entity.categoryId)
    entity.toDomain(cat?.name ?: "")
  }

  override suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
    val productId = productDao.insert(product.toEntity())
    // Crear registro de stock en todos los almacenes existentes
    val warehouses = warehouseDao.getAllWarehouses()
    val allActive = database.openHelper.readableDatabase
    val cursor = allActive.query("SELECT id FROM warehouses")
    if (cursor.moveToFirst()) {
      do {
        val wId = cursor.getLong(0)
        warehouseDao.insertStock(
          WarehouseStockEntity(
            warehouseId = wId,
            productId = productId,
            currentStock = 0.0,
            minAlertStock = 10.0,
            updatedAt = System.currentTimeMillis(),
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
    productId
  }

  override suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
    productDao.update(product.toEntity().copy(updatedAt = System.currentTimeMillis()))
  }

  override suspend fun deleteProduct(productId: Long) = withContext(Dispatchers.IO) {
    inventoryDao.deleteByProductId(productId)
    productDao.deleteProductById(productId)
  }

  // -------------------------------------------------------------
  // Daily Sessions (Scoped by Project)
  // -------------------------------------------------------------
  override fun getAllSessions(projectId: Long?): Flow<List<DailySession>> {
    return if (projectId != null) {
      sessionDao.getSessionsByProject(projectId).map { list ->
        list.map { it.toDomain() }
      }.flowOn(Dispatchers.IO)
    } else {
      selectedProjectIdFlow.flatMapLatest { id ->
        sessionDao.getSessionsByProject(id).map { list ->
          list.map { it.toDomain() }
        }
      }.flowOn(Dispatchers.IO)
    }
  }

  override fun getLatestOpenSession(projectId: Long?): Flow<DailySession?> {
    return if (projectId != null) {
      sessionDao.getLatestOpenSessionByProject(projectId).map { it?.toDomain() }.flowOn(Dispatchers.IO)
    } else {
      selectedProjectIdFlow.flatMapLatest { id ->
        sessionDao.getLatestOpenSessionByProject(id).map { it?.toDomain() }
      }.flowOn(Dispatchers.IO)
    }
  }

  override fun getLatestSession(projectId: Long?): Flow<DailySession?> {
    return if (projectId != null) {
      sessionDao.getLatestSessionByProject(projectId).map { it?.toDomain() }.flowOn(Dispatchers.IO)
    } else {
      selectedProjectIdFlow.flatMapLatest { id ->
        sessionDao.getLatestSessionByProject(id).map { it?.toDomain() }
      }.flowOn(Dispatchers.IO)
    }
  }

  override suspend fun getSessionById(sessionId: Long): DailySession? = withContext(Dispatchers.IO) {
    sessionDao.getSessionById(sessionId)?.toDomain()
  }

  override suspend fun getSessionByDate(dateString: String, projectId: Long?): DailySession? = withContext(Dispatchers.IO) {
    val targetProjectId = projectId ?: selectedProjectIdFlow.value
    sessionDao.getSessionByProjectAndDate(targetProjectId, dateString)?.toDomain()
  }

  override suspend fun getOrCreateSessionForDate(
    dateString: String,
    dateEpochDay: Long,
    projectId: Long?,
  ): DailySession = withContext(Dispatchers.IO) {
    val targetProjectId = projectId ?: selectedProjectIdFlow.value
    val existing = sessionDao.getSessionByProjectAndDate(targetProjectId, dateString)
    if (existing != null) {
      return@withContext existing.toDomain()
    }

    // Buscar la última sesión cerrada para este proyecto específico para heredar existencias
    val lastClosed = sessionDao.getLatestClosedSessionByProject(targetProjectId)
    val previousStocks = mutableMapOf<Long, Double>()
    if (lastClosed != null) {
      val rawPrevItems = inventoryDao.getRawItemsForSession(lastClosed.id)
      for (prev in rawPrevItems) {
        previousStocks[prev.productId] = prev.closingStock
      }
    }

    // Crear nueva sesión para este proyecto
    val newSessionEntity = DailySessionEntity(
      projectId = targetProjectId,
      dateString = dateString,
      dateEpochDay = dateEpochDay,
      status = "OPEN",
      notes = "",
    )
    val sessionId = sessionDao.insert(newSessionEntity)

    // Crear entradas de inventario para todos los productos activos
    val allActive = database.openHelper.readableDatabase
    val cursor = allActive.query("SELECT id, salePriceCents, costPriceCents FROM products WHERE active = 1")
    val itemsToInsert = mutableListOf<DailyInventoryEntity>()

    if (cursor.moveToFirst()) {
      do {
        val pId = cursor.getLong(0)
        val pSale = cursor.getLong(1)
        val pCost = cursor.getLong(2)
        val initialStock = previousStocks[pId] ?: 0.0

        itemsToInsert.add(
          DailyInventoryEntity(
            sessionId = sessionId,
            productId = pId,
            openingStock = initialStock,
            entries = 0.0,
            closingStock = initialStock,
            breakage = 0.0,
            salePriceCents = pSale,
            costPriceCents = pCost,
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()

    if (itemsToInsert.isNotEmpty()) {
      inventoryDao.insertAll(itemsToInsert)
    }

    newSessionEntity.copy(id = sessionId).toDomain()
  }

  override suspend fun closeSession(sessionId: Long) = withContext(Dispatchers.IO) {
    sessionDao.closeSession(sessionId, System.currentTimeMillis())
  }

  override suspend fun reopenSession(sessionId: Long) = withContext(Dispatchers.IO) {
    sessionDao.reopenSession(sessionId)
  }

  // -------------------------------------------------------------
  // Daily Inventory Operations
  // -------------------------------------------------------------
  override fun getSessionInventoryItems(sessionId: Long): Flow<List<DailyInventoryItem>> {
    return inventoryDao.getInventoryWithProductForSession(sessionId).map { queryResults ->
      queryResults.map { qr ->
        SalesCalculator.enrichInventoryItem(
          id = qr.id,
          sessionId = qr.sessionId,
          productId = qr.productId,
          productName = qr.productName,
          categoryId = qr.categoryId,
          categoryName = qr.categoryName,
          unitOfMeasure = qr.unitOfMeasure,
          openingStock = qr.openingStock,
          entries = qr.entries,
          closingStock = qr.closingStock,
          breakage = qr.breakage,
          salePriceCents = qr.salePriceCents,
          costPriceCents = qr.costPriceCents,
        )
      }
    }.flowOn(Dispatchers.IO)
  }

  override fun getSessionSummary(sessionId: Long): Flow<DaySummary?> {
    return combine(
      sessionDao.getAllSessions().map { list -> list.find { it.id == sessionId } },
      getSessionInventoryItems(sessionId)
    ) { sessionEntity, items ->
      if (sessionEntity == null) null
      else SalesCalculator.computeDaySummary(sessionEntity.toDomain(), items)
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun updateInventoryCount(
    sessionId: Long,
    productId: Long,
    closingStock: Double,
    breakage: Double,
  ) = withContext(Dispatchers.IO) {
    val existing = inventoryDao.getItem(sessionId, productId) ?: return@withContext
    val available = SalesCalculator.calculateAvailableStock(existing.openingStock, existing.entries)
    if (closingStock + breakage > available + 0.0001) {
      throw IllegalArgumentException(
        "El inventario final ($closingStock) más roturas ($breakage) no puede superar el disponible ($available)"
      )
    }
    inventoryDao.updateCount(sessionId, productId, closingStock, breakage)
  }

  override suspend fun addEntries(
    sessionId: Long,
    productId: Long,
    additionalEntries: Double,
  ) = withContext(Dispatchers.IO) {
    require(additionalEntries >= 0.0) { "Las entradas no pueden ser negativas" }
    val existing = inventoryDao.getItem(sessionId, productId)
    if (existing == null) {
      val product = productDao.getProductById(productId)
      if (product != null) {
        inventoryDao.insert(
          DailyInventoryEntity(
            sessionId = sessionId,
            productId = productId,
            openingStock = 0.0,
            entries = additionalEntries,
            closingStock = additionalQuantityToClosing(0.0, additionalEntries),
            breakage = 0.0,
            salePriceCents = product.salePriceCents,
            costPriceCents = product.costPriceCents,
          )
        )
      }
      return@withContext
    }

    inventoryDao.addEntries(sessionId, productId, additionalEntries)
    val updatedItem = inventoryDao.getItem(sessionId, productId)
    if (updatedItem != null) {
      val newClosing = updatedItem.closingStock + additionalEntries
      inventoryDao.updateCount(sessionId, productId, newClosing, updatedItem.breakage)
    }
  }

  private fun additionalQuantityToClosing(opening: Double, entries: Double): Double {
    return opening + entries
  }

  override suspend fun registerQuickSale(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  ) = withContext(Dispatchers.IO) {
    require(soldQuantity > 0) { "La cantidad vendida debe ser mayor que cero" }
    val existing = inventoryDao.getItem(sessionId, productId)
      ?: throw IllegalArgumentException("Producto no encontrado en inventario de la sesión")

    val available = SalesCalculator.calculateAvailableStock(existing.openingStock, existing.entries)
    val currentSold = SalesCalculator.calculateSoldQuantity(available, existing.closingStock, existing.breakage)
    val newTotalSold = currentSold + soldQuantity

    if (newTotalSold + existing.breakage > available + 0.0001) {
      throw IllegalArgumentException("Stock insuficiente para realizar la venta (Disponible: ${available - currentSold - existing.breakage})")
    }

    val newClosingStock = available - newTotalSold - existing.breakage
    inventoryDao.updateCount(sessionId, productId, newClosingStock, existing.breakage)
  }

  override suspend fun updateSoldQuantity(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  ) = withContext(Dispatchers.IO) {
    require(soldQuantity >= 0) { "La cantidad vendida no puede ser negativa" }
    val existing = inventoryDao.getItem(sessionId, productId)
      ?: throw IllegalArgumentException("Producto no encontrado en inventario de la sesión")

    val available = SalesCalculator.calculateAvailableStock(existing.openingStock, existing.entries)
    if (soldQuantity + existing.breakage > available + 0.0001) {
      throw IllegalArgumentException("La cantidad vendida ($soldQuantity) supera el stock disponible ($available)")
    }

    val newClosingStock = available - soldQuantity - existing.breakage
    inventoryDao.updateCount(sessionId, productId, newClosingStock, existing.breakage)
  }

  override suspend fun cancelSale(sessionId: Long, productId: Long) = withContext(Dispatchers.IO) {
    val existing = inventoryDao.getItem(sessionId, productId)
      ?: throw IllegalArgumentException("Producto no encontrado en inventario de la sesión")

    val available = SalesCalculator.calculateAvailableStock(existing.openingStock, existing.entries)
    val newClosingStock = available - existing.breakage
    inventoryDao.updateCount(sessionId, productId, newClosingStock, existing.breakage)
  }

  // -------------------------------------------------------------
  // Initial Seeding
  // -------------------------------------------------------------
  override suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
    if (projectDao.count() > 0) return@withContext

    // 1. Proyectos independientes
    val projectKioskoId = projectDao.insert(
      ProjectEntity(
        id = 1,
        name = "Kiosko Central",
        type = "KIOSKO",
        description = "Punto de venta rápido, golosinas y bebidas",
        colorHex = "#1E88E5",
      )
    )
    val projectBarId = projectDao.insert(
      ProjectEntity(
        id = 2,
        name = "Bar Tropical",
        type = "BAR",
        description = "Servicio de coctelería, cervezas y barra",
        colorHex = "#43A047",
      )
    )
    val projectPaladarId = projectDao.insert(
      ProjectEntity(
        id = 3,
        name = "Paladar Doña Rosa",
        type = "PALADAR",
        description = "Restaurante criollo con servicio a mesa y cocina",
        colorHex = "#E53935",
      )
    )

    // 2. Almacenes independientes
    val warehouseKioskosId = warehouseDao.insert(
      WarehouseEntity(
        id = 1,
        name = "Almacén Central Kioskos",
        targetType = "KIOSKOS",
        location = "Nave A - Depósito Principal",
        notes = "Abastecimiento general para kioskos y puntos de expendio",
      )
    )
    val warehouseBarId = warehouseDao.insert(
      WarehouseEntity(
        id = 2,
        name = "Almacén Bebidas & Bar",
        targetType = "BAR",
        location = "Bodega Climatizada",
        notes = "Cervezas importadas, refrescos, vinos y licores",
      )
    )
    val warehousePaladarId = warehouseDao.insert(
      WarehouseEntity(
        id = 3,
        name = "Almacén Insumos Paladar",
        targetType = "PALADAR",
        location = "Cámara Fría y Despensa Central",
        notes = "Alimentos, abarrotes y provisiones de cocina",
      )
    )

    // 3. Categorías y Catálogo de Productos
    val catBebidasId = categoryDao.insert(CategoryEntity(name = "Bebidas", description = "Cervezas, refrescos y agua"))
    val catAlimentosId = categoryDao.insert(CategoryEntity(name = "Alimentos", description = "Snacks, galletas y comida rápida"))
    val catCigarrillosId = categoryDao.insert(CategoryEntity(name = "Cigarros", description = "Tabacos y cigarrillos"))
    val catOtrosId = categoryDao.insert(CategoryEntity(name = "Varios", description = "Artículos varios y encendedores"))

    val products = listOf(
      ProductEntity(categoryId = catBebidasId, name = "Cerveza Cristal", unitOfMeasure = "Botella", salePriceCents = 560_00L, costPriceCents = 450_00L, minStock = 24.0),
      ProductEntity(categoryId = catBebidasId, name = "Cerveza Bucanero", unitOfMeasure = "Botella", salePriceCents = 580_00L, costPriceCents = 470_00L, minStock = 24.0),
      ProductEntity(categoryId = catBebidasId, name = "Cerveza Corona", unitOfMeasure = "Botella", salePriceCents = 650_00L, costPriceCents = 520_00L, minStock = 12.0),
      ProductEntity(categoryId = catBebidasId, name = "Refresco TuKola 330ml", unitOfMeasure = "Lata", salePriceCents = 250_00L, costPriceCents = 180_00L, minStock = 24.0),
      ProductEntity(categoryId = catBebidasId, name = "Agua Mineral 1.5L", unitOfMeasure = "Botella", salePriceCents = 200_00L, costPriceCents = 130_00L, minStock = 12.0),
      ProductEntity(categoryId = catAlimentosId, name = "Papas Fritas Gourmet", unitOfMeasure = "Bolsa", salePriceCents = 180_00L, costPriceCents = 110_00L, minStock = 10.0),
      ProductEntity(categoryId = catAlimentosId, name = "Maní Salado 100g", unitOfMeasure = "Bolsa", salePriceCents = 90_00L, costPriceCents = 50_00L, minStock = 15.0),
      ProductEntity(categoryId = catCigarrillosId, name = "Cigarros H. Upmann", unitOfMeasure = "Cajetilla", salePriceCents = 350_00L, costPriceCents = 280_00L, minStock = 10.0),
      ProductEntity(categoryId = catCigarrillosId, name = "Cigarros Popular", unitOfMeasure = "Cajetilla", salePriceCents = 300_00L, costPriceCents = 230_00L, minStock = 10.0),
      ProductEntity(categoryId = catOtrosId, name = "Encendedor Candela", unitOfMeasure = "U", salePriceCents = 50_00L, costPriceCents = 25_00L, minStock = 10.0),
    )
    val insertedProductIds = productDao.insertAll(products)

    // 4. Stock en Almacenes
    // Stock en Almacén Kioskos
    val stockKioskos = listOf(
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[0], currentStock = 300.0, minAlertStock = 48.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[1], currentStock = 250.0, minAlertStock = 48.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[2], currentStock = 120.0, minAlertStock = 24.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[3], currentStock = 500.0, minAlertStock = 100.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[4], currentStock = 200.0, minAlertStock = 36.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[5], currentStock = 150.0, minAlertStock = 30.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[6], currentStock = 200.0, minAlertStock = 30.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[7], currentStock = 180.0, minAlertStock = 20.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[8], currentStock = 220.0, minAlertStock = 25.0),
      WarehouseStockEntity(warehouseId = warehouseKioskosId, productId = insertedProductIds[9], currentStock = 100.0, minAlertStock = 20.0),
    )
    warehouseDao.insertAllStock(stockKioskos)

    // Stock en Almacén Bar
    val stockBar = listOf(
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[0], currentStock = 600.0, minAlertStock = 80.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[1], currentStock = 450.0, minAlertStock = 60.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[2], currentStock = 300.0, minAlertStock = 40.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[3], currentStock = 400.0, minAlertStock = 50.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[4], currentStock = 250.0, minAlertStock = 30.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[5], currentStock = 80.0, minAlertStock = 15.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[6], currentStock = 120.0, minAlertStock = 20.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[7], currentStock = 100.0, minAlertStock = 15.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[8], currentStock = 100.0, minAlertStock = 15.0),
      WarehouseStockEntity(warehouseId = warehouseBarId, productId = insertedProductIds[9], currentStock = 50.0, minAlertStock = 10.0),
    )
    warehouseDao.insertAllStock(stockBar)

    // Stock en Almacén Paladar
    val stockPaladar = listOf(
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[0], currentStock = 200.0, minAlertStock = 30.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[1], currentStock = 150.0, minAlertStock = 25.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[2], currentStock = 100.0, minAlertStock = 20.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[3], currentStock = 350.0, minAlertStock = 50.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[4], currentStock = 300.0, minAlertStock = 40.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[5], currentStock = 250.0, minAlertStock = 40.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[6], currentStock = 180.0, minAlertStock = 30.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[7], currentStock = 50.0, minAlertStock = 10.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[8], currentStock = 50.0, minAlertStock = 10.0),
      WarehouseStockEntity(warehouseId = warehousePaladarId, productId = insertedProductIds[9], currentStock = 30.0, minAlertStock = 5.0),
    )
    warehouseDao.insertAllStock(stockPaladar)

    // 5. Historial de sesiones para Kiosko Principal (Día 19, 20, 28)
    val session19Id = sessionDao.insert(
      DailySessionEntity(
        projectId = projectKioskoId,
        dateString = "19-06-2026",
        dateEpochDay = 20624L,
        status = "CLOSED",
        notes = "Cierre diario consolidado 19/06 - Kiosko Central",
        closedAt = System.currentTimeMillis() - 86400000L * 2,
      )
    )

    val items19 = listOf(
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[0], openingStock = 180.0, entries = 48.0, closingStock = 168.0, breakage = 2.0, salePriceCents = 560_00L, costPriceCents = 450_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[1], openingStock = 120.0, entries = 24.0, closingStock = 110.0, breakage = 0.0, salePriceCents = 580_00L, costPriceCents = 470_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[2], openingStock = 50.0, entries = 0.0, closingStock = 42.0, breakage = 0.0, salePriceCents = 650_00L, costPriceCents = 520_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[3], openingStock = 90.0, entries = 24.0, closingStock = 85.0, breakage = 1.0, salePriceCents = 250_00L, costPriceCents = 180_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[4], openingStock = 40.0, entries = 12.0, closingStock = 38.0, breakage = 0.0, salePriceCents = 200_00L, costPriceCents = 130_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[5], openingStock = 30.0, entries = 10.0, closingStock = 28.0, breakage = 0.0, salePriceCents = 180_00L, costPriceCents = 110_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[6], openingStock = 45.0, entries = 0.0, closingStock = 40.0, breakage = 0.0, salePriceCents = 90_00L, costPriceCents = 50_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[7], openingStock = 50.0, entries = 20.0, closingStock = 55.0, breakage = 0.0, salePriceCents = 350_00L, costPriceCents = 280_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[8], openingStock = 60.0, entries = 0.0, closingStock = 48.0, breakage = 0.0, salePriceCents = 300_00L, costPriceCents = 230_00L),
      DailyInventoryEntity(sessionId = session19Id, productId = insertedProductIds[9], openingStock = 25.0, entries = 0.0, closingStock = 22.0, breakage = 0.0, salePriceCents = 50_00L, costPriceCents = 25_00L),
    )
    inventoryDao.insertAll(items19)

    val session20Id = sessionDao.insert(
      DailySessionEntity(
        projectId = projectKioskoId,
        dateString = "20-06-2026",
        dateEpochDay = 20625L,
        status = "CLOSED",
        notes = "Cierre diario consolidado 20/06 - Kiosko Central",
        closedAt = System.currentTimeMillis() - 86400000L,
      )
    )
    val items20 = listOf(
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[0], openingStock = 168.0, entries = 0.0, closingStock = 145.0, breakage = 1.0, salePriceCents = 560_00L, costPriceCents = 450_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[1], openingStock = 110.0, entries = 24.0, closingStock = 98.0, breakage = 0.0, salePriceCents = 580_00L, costPriceCents = 470_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[2], openingStock = 42.0, entries = 0.0, closingStock = 35.0, breakage = 0.0, salePriceCents = 650_00L, costPriceCents = 520_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[3], openingStock = 85.0, entries = 24.0, closingStock = 70.0, breakage = 0.0, salePriceCents = 250_00L, costPriceCents = 180_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[4], openingStock = 38.0, entries = 0.0, closingStock = 32.0, breakage = 0.0, salePriceCents = 200_00L, costPriceCents = 130_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[5], openingStock = 28.0, entries = 0.0, closingStock = 22.0, breakage = 0.0, salePriceCents = 180_00L, costPriceCents = 110_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[6], openingStock = 40.0, entries = 10.0, closingStock = 36.0, breakage = 0.0, salePriceCents = 90_00L, costPriceCents = 50_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[7], openingStock = 55.0, entries = 0.0, closingStock = 44.0, breakage = 0.0, salePriceCents = 350_00L, costPriceCents = 280_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[8], openingStock = 48.0, entries = 20.0, closingStock = 52.0, breakage = 0.0, salePriceCents = 300_00L, costPriceCents = 230_00L),
      DailyInventoryEntity(sessionId = session20Id, productId = insertedProductIds[9], openingStock = 22.0, entries = 0.0, closingStock = 18.0, breakage = 0.0, salePriceCents = 50_00L, costPriceCents = 25_00L),
    )
    inventoryDao.insertAll(items20)

    val sessionCurrentId = sessionDao.insert(
      DailySessionEntity(
        projectId = projectKioskoId,
        dateString = "28-06-2026",
        dateEpochDay = 20633L,
        status = "OPEN",
        notes = "Jornada en curso 28/06 - Kiosko Central",
      )
    )
    val itemsCurrent = listOf(
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[0], openingStock = 145.0, entries = 24.0, closingStock = 130.0, breakage = 0.0, salePriceCents = 560_00L, costPriceCents = 450_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[1], openingStock = 98.0, entries = 0.0, closingStock = 90.0, breakage = 0.0, salePriceCents = 580_00L, costPriceCents = 470_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[2], openingStock = 35.0, entries = 12.0, closingStock = 38.0, breakage = 0.0, salePriceCents = 650_00L, costPriceCents = 520_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[3], openingStock = 70.0, entries = 0.0, closingStock = 62.0, breakage = 0.0, salePriceCents = 250_00L, costPriceCents = 180_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[4], openingStock = 32.0, entries = 0.0, closingStock = 28.0, breakage = 0.0, salePriceCents = 200_00L, costPriceCents = 130_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[5], openingStock = 22.0, entries = 10.0, closingStock = 24.0, breakage = 0.0, salePriceCents = 180_00L, costPriceCents = 110_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[6], openingStock = 36.0, entries = 0.0, closingStock = 30.0, breakage = 0.0, salePriceCents = 90_00L, costPriceCents = 50_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[7], openingStock = 44.0, entries = 0.0, closingStock = 39.0, breakage = 0.0, salePriceCents = 350_00L, costPriceCents = 280_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[8], openingStock = 52.0, entries = 0.0, closingStock = 45.0, breakage = 0.0, salePriceCents = 300_00L, costPriceCents = 230_00L),
      DailyInventoryEntity(sessionId = sessionCurrentId, productId = insertedProductIds[9], openingStock = 18.0, entries = 0.0, closingStock = 15.0, breakage = 0.0, salePriceCents = 50_00L, costPriceCents = 25_00L),
    )
    inventoryDao.insertAll(itemsCurrent)

    // Registrar un despacho de ejemplo en el historial para mostrar la trazabilidad completa
    val sampleDispatch = WarehouseDispatchEntity(
      warehouseId = warehouseKioskosId,
      projectId = projectKioskoId,
      productId = insertedProductIds[3], // Refresco TuKola
      quantity = 45.0,
      remainingStockInWarehouse = 455.0,
      dateString = "28-06-2026",
      timestamp = System.currentTimeMillis() - 7200000L,
      notes = "Despacho matutino inicial de refrescos para venta diaria",
    )
    warehouseDao.insertDispatch(sampleDispatch)
  }

  // -------------------------------------------------------------
  // Mappers
  // -------------------------------------------------------------
  private fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    type = type,
    description = description,
    colorHex = colorHex,
    active = active,
    createdAt = createdAt,
  )

  private fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    type = type,
    description = description,
    colorHex = colorHex,
    active = active,
    createdAt = createdAt,
  )

  private fun WarehouseEntity.toDomain() = Warehouse(
    id = id,
    name = name,
    targetType = targetType,
    location = location,
    notes = notes,
    active = active,
    createdAt = createdAt,
  )

  private fun Warehouse.toEntity() = WarehouseEntity(
    id = id,
    name = name,
    targetType = targetType,
    location = location,
    notes = notes,
    active = active,
    createdAt = createdAt,
  )

  private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    description = description,
    active = active,
  )

  private fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    description = description,
    active = active,
  )

  private fun ProductEntity.toDomain(categoryName: String) = Product(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    name = name,
    unitOfMeasure = unitOfMeasure,
    salePriceCents = salePriceCents,
    costPriceCents = costPriceCents,
    minStock = minStock,
    maxStock = maxStock,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

  private fun Product.toEntity() = ProductEntity(
    id = id,
    categoryId = categoryId,
    name = name,
    unitOfMeasure = unitOfMeasure,
    salePriceCents = salePriceCents,
    costPriceCents = costPriceCents,
    minStock = minStock,
    maxStock = maxStock,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

  private fun DailySessionEntity.toDomain() = DailySession(
    id = id,
    projectId = projectId,
    dateString = dateString,
    dateEpochDay = dateEpochDay,
    status = if (status == "CLOSED") SessionStatus.CLOSED else SessionStatus.OPEN,
    notes = notes,
    createdAt = createdAt,
    closedAt = closedAt,
  )

  private fun com.example.data.local.dao.WarehouseDispatchQueryResult.toDomain() = WarehouseDispatch(
    id = id,
    warehouseId = warehouseId,
    warehouseName = warehouseName,
    projectId = projectId,
    projectName = projectName,
    productId = productId,
    productName = productName,
    unitOfMeasure = unitOfMeasure,
    quantity = quantity,
    remainingStockInWarehouse = remainingStockInWarehouse,
    dateString = dateString,
    timestamp = timestamp,
    notes = notes,
  )
}
