package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DailyInventoryEntity
import com.example.data.local.entity.DailySessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import com.example.domain.model.SessionStatus
import com.example.domain.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InventoryRepositoryImpl(
  private val database: AppDatabase,
) : InventoryRepository {

  private val categoryDao = database.categoryDao()
  private val productDao = database.productDao()
  private val sessionDao = database.dailySessionDao()
  private val inventoryDao = database.dailyInventoryDao()

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
    productDao.insert(product.toEntity())
  }

  override suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
    productDao.update(product.toEntity().copy(updatedAt = System.currentTimeMillis()))
  }

  override suspend fun deleteProduct(productId: Long) = withContext(Dispatchers.IO) {
    inventoryDao.deleteByProductId(productId)
    productDao.deleteProductById(productId)
  }

  override fun getAllSessions(): Flow<List<DailySession>> {
    return sessionDao.getAllSessions().map { list ->
      list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)
  }

  override fun getLatestOpenSession(): Flow<DailySession?> {
    return sessionDao.getLatestOpenSession().map { it?.toDomain() }.flowOn(Dispatchers.IO)
  }

  override fun getLatestSession(): Flow<DailySession?> {
    return sessionDao.getLatestSession().map { it?.toDomain() }.flowOn(Dispatchers.IO)
  }

  override suspend fun getSessionById(sessionId: Long): DailySession? = withContext(Dispatchers.IO) {
    sessionDao.getSessionById(sessionId)?.toDomain()
  }

  override suspend fun getSessionByDate(dateString: String): DailySession? = withContext(Dispatchers.IO) {
    sessionDao.getSessionByDate(dateString)?.toDomain()
  }

  override suspend fun getOrCreateSessionForDate(
    dateString: String,
    dateEpochDay: Long,
  ): DailySession = withContext(Dispatchers.IO) {
    val existing = sessionDao.getSessionByDate(dateString)
    if (existing != null) {
      return@withContext existing.toDomain()
    }

    // Buscar la última sesión cerrada para heredar existencias
    val lastClosed = sessionDao.getLatestClosedSession()
    val previousStocks = mutableMapOf<Long, Double>()
    if (lastClosed != null) {
      val rawPrevItems = inventoryDao.getRawItemsForSession(lastClosed.id)
      for (prev in rawPrevItems) {
        previousStocks[prev.productId] = prev.closingStock
      }
    }

    // Crear nueva sesión
    val newSessionEntity = DailySessionEntity(
      dateString = dateString,
      dateEpochDay = dateEpochDay,
      status = "OPEN",
      notes = "",
    )
    val sessionId = sessionDao.insert(newSessionEntity)

    // Crear entradas de inventario para todos los productos activos
    val activeProducts = productDao.getProductById(1)?.let { listOf(it) } ?: emptyList()
    // Consultar todos los productos directamente en una transacción
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
            closingStock = initialStock, // Por defecto, sin ventas aún
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
    inventoryDao.addEntries(sessionId, productId, additionalEntries)
    // También ajustamos el conteo final sugerido en la misma proporción si no se ha hecho conteo
    val item = inventoryDao.getItem(sessionId, productId)
    if (item != null) {
      val newClosing = item.closingStock + additionalEntries
      inventoryDao.updateCount(sessionId, productId, newClosing, item.breakage)
    }
  }

  override suspend fun registerQuickSale(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  ) = withContext(Dispatchers.IO) {
    require(soldQuantity > 0.0) { "La cantidad vendida debe ser mayor a 0" }
    val item = inventoryDao.getItem(sessionId, productId) ?: return@withContext
    val newClosing = maxOf(0.0, item.closingStock - soldQuantity)
    inventoryDao.updateCount(sessionId, productId, newClosing, item.breakage)
  }

  override suspend fun updateSoldQuantity(
    sessionId: Long,
    productId: Long,
    soldQuantity: Double,
  ) = withContext(Dispatchers.IO) {
    require(soldQuantity >= 0.0) { "La cantidad vendida no puede ser negativa" }
    val item = inventoryDao.getItem(sessionId, productId) ?: return@withContext
    val available = SalesCalculator.calculateAvailableStock(item.openingStock, item.entries)
    if (soldQuantity + item.breakage > available + 0.0001) {
      throw IllegalArgumentException(
        "La venta ($soldQuantity) más roturas (${item.breakage}) no puede superar el disponible ($available)"
      )
    }
    val newClosing = maxOf(0.0, available - item.breakage - soldQuantity)
    inventoryDao.updateCount(sessionId, productId, newClosing, item.breakage)
  }

  override suspend fun cancelSale(
    sessionId: Long,
    productId: Long,
  ) = withContext(Dispatchers.IO) {
    val item = inventoryDao.getItem(sessionId, productId) ?: return@withContext
    val available = SalesCalculator.calculateAvailableStock(item.openingStock, item.entries)
    val restoredClosing = maxOf(0.0, available - item.breakage)
    inventoryDao.updateCount(sessionId, productId, restoredClosing, item.breakage)
  }

  override suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
    if (categoryDao.count() > 0) return@withContext

    // 1. Categorías
    val catBebidasId = categoryDao.insert(CategoryEntity(name = "Bebidas", description = "Cervezas, refrescos, licores y aguas"))
    val catAlimentosId = categoryDao.insert(CategoryEntity(name = "Alimentos", description = "Snacks, golosinas y confitería"))
    val catCigarrillosId = categoryDao.insert(CategoryEntity(name = "Cigarrillos", description = "Cajetillas y tabaco"))
    val catOtrosId = categoryDao.insert(CategoryEntity(name = "Otros", description = "Artículos de uso y conveniencia"))

    // 2. Productos representativos del archivo Excel Jun-2026
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

    // 3. Crear sesiones históricas para reflejar la continuidad del Excel
    // Día 19-06-2026 (Cerrado)
    val session19Id = sessionDao.insert(
      DailySessionEntity(
        dateString = "19-06-2026",
        dateEpochDay = 20624L,
        status = "CLOSED",
        notes = "Cierre diario consolidado 19/06",
        closedAt = System.currentTimeMillis() - 86400000L * 2,
      )
    )

    // Inventario para el día 19
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

    // Día 20-06-2026 (Cerrado, hereda exactamente del día 19)
    val session20Id = sessionDao.insert(
      DailySessionEntity(
        dateString = "20-06-2026",
        dateEpochDay = 20625L,
        status = "CLOSED",
        notes = "Cierre diario consolidado 20/06",
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

    // Día Actual (Abierto: 28-06-2026, hereda del último inventario)
    val sessionCurrentId = sessionDao.insert(
      DailySessionEntity(
        dateString = "28-06-2026",
        dateEpochDay = 20633L,
        status = "OPEN",
        notes = "Jornada en curso 28/06",
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
  }

  // Mappers
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
    dateString = dateString,
    dateEpochDay = dateEpochDay,
    status = if (status == "CLOSED") SessionStatus.CLOSED else SessionStatus.OPEN,
    notes = notes,
    createdAt = createdAt,
    closedAt = closedAt,
  )
}
