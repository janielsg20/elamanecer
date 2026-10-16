package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DailyInventoryEntity
import com.example.data.local.entity.DailySessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.WarehouseDispatchEntity
import com.example.data.local.entity.WarehouseEntity
import com.example.data.local.entity.WarehouseStockEntity
import kotlinx.coroutines.flow.Flow

data class InventoryItemQueryResult(
  val id: Long,
  val sessionId: Long,
  val productId: Long,
  val productName: String,
  val categoryId: Long,
  val categoryName: String,
  val unitOfMeasure: String,
  val openingStock: Double,
  val entries: Double,
  val closingStock: Double,
  val breakage: Double,
  val salePriceCents: Long,
  val costPriceCents: Long,
)

data class WarehouseStockQueryResult(
  val id: Long,
  val warehouseId: Long,
  val productId: Long,
  val productName: String,
  val categoryName: String,
  val unitOfMeasure: String,
  val initialStock: Double,
  val entries: Double,
  val exits: Double,
  val costPriceCents: Long,
  val salePriceCents: Long,
  val currentStock: Double,
  val minAlertStock: Double,
  val updatedAt: Long,
)

data class WarehouseDispatchQueryResult(
  val id: Long,
  val warehouseId: Long,
  val warehouseName: String,
  val projectId: Long,
  val projectName: String,
  val productId: Long,
  val productName: String,
  val unitOfMeasure: String,
  val quantity: Double,
  val remainingStockInWarehouse: Double,
  val dateString: String,
  val timestamp: Long,
  val notes: String,
)

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects WHERE active = 1 ORDER BY id ASC")
  fun getAllActiveProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects ORDER BY id ASC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: Long): ProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(project: ProjectEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(projects: List<ProjectEntity>): List<Long>

  @Update
  suspend fun update(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProjectById(id: Long)

  @Query("SELECT COUNT(*) FROM projects")
  suspend fun count(): Int
}

@Dao
interface WarehouseDao {
  @Query("SELECT * FROM warehouses WHERE active = 1 ORDER BY name ASC")
  fun getAllActiveWarehouses(): Flow<List<WarehouseEntity>>

  @Query("SELECT * FROM warehouses ORDER BY name ASC")
  fun getAllWarehouses(): Flow<List<WarehouseEntity>>

  @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1")
  suspend fun getWarehouseById(id: Long): WarehouseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(warehouse: WarehouseEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(warehouses: List<WarehouseEntity>): List<Long>

  @Update
  suspend fun update(warehouse: WarehouseEntity)

  @Query("DELETE FROM warehouses WHERE id = :id")
  suspend fun deleteWarehouseById(id: Long)

  @Query("SELECT COUNT(*) FROM warehouses")
  suspend fun count(): Int

  // Stock
  @Query(
    """
    SELECT 
      ws.id, ws.warehouseId, ws.productId, p.name AS productName,
      c.name AS categoryName, p.unitOfMeasure,
      ws.initialStock, ws.entries, ws.exits,
      COALESCE(NULLIF(ws.costPriceCents, 0), p.costPriceCents) AS costPriceCents,
      p.salePriceCents,
      (ws.initialStock + ws.entries - ws.exits) AS currentStock,
      ws.minAlertStock, ws.updatedAt
    FROM warehouse_stock ws
    INNER JOIN products p ON ws.productId = p.id
    INNER JOIN categories c ON p.categoryId = c.id
    WHERE ws.warehouseId = :warehouseId
    ORDER BY c.name ASC, p.name ASC
  """
  )
  fun getStockForWarehouse(warehouseId: Long): Flow<List<WarehouseStockQueryResult>>

  @Query("SELECT * FROM warehouse_stock WHERE warehouseId = :warehouseId AND productId = :productId LIMIT 1")
  suspend fun getStockItem(warehouseId: Long, productId: Long): WarehouseStockEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStock(stock: WarehouseStockEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllStock(stocks: List<WarehouseStockEntity>): List<Long>

  @Update
  suspend fun updateStock(stock: WarehouseStockEntity)

  @Query("UPDATE warehouse_stock SET currentStock = :newStock, updatedAt = :updatedAt WHERE warehouseId = :warehouseId AND productId = :productId")
  suspend fun updateStockQuantity(warehouseId: Long, productId: Long, newStock: Double, updatedAt: Long)

  @Query("DELETE FROM warehouse_stock WHERE warehouseId = :warehouseId AND productId = :productId")
  suspend fun deleteStockItem(warehouseId: Long, productId: Long)

  // Dispatches
  @Query(
    """
    SELECT 
      wd.id, wd.warehouseId, w.name AS warehouseName,
      wd.projectId, pr.name AS projectName,
      wd.productId, p.name AS productName, p.unitOfMeasure,
      wd.quantity, wd.remainingStockInWarehouse,
      wd.dateString, wd.timestamp, wd.notes
    FROM warehouse_dispatches wd
    INNER JOIN warehouses w ON wd.warehouseId = w.id
    INNER JOIN projects pr ON wd.projectId = pr.id
    INNER JOIN products p ON wd.productId = p.id
    ORDER BY wd.timestamp DESC
  """
  )
  fun getAllDispatches(): Flow<List<WarehouseDispatchQueryResult>>

  @Query(
    """
    SELECT 
      wd.id, wd.warehouseId, w.name AS warehouseName,
      wd.projectId, pr.name AS projectName,
      wd.productId, p.name AS productName, p.unitOfMeasure,
      wd.quantity, wd.remainingStockInWarehouse,
      wd.dateString, wd.timestamp, wd.notes
    FROM warehouse_dispatches wd
    INNER JOIN warehouses w ON wd.warehouseId = w.id
    INNER JOIN projects pr ON wd.projectId = pr.id
    INNER JOIN products p ON wd.productId = p.id
    WHERE wd.warehouseId = :warehouseId
    ORDER BY wd.timestamp DESC
  """
  )
  fun getDispatchesForWarehouse(warehouseId: Long): Flow<List<WarehouseDispatchQueryResult>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDispatch(dispatch: WarehouseDispatchEntity): Long
}

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY name ASC")
  fun getAllCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE active = 1 ORDER BY name ASC")
  fun getActiveCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE id = :id")
  suspend fun getCategoryById(id: Long): CategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(category: CategoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

  @Update
  suspend fun update(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteCategoryById(id: Long)

  @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
  suspend fun countProductsInCategory(categoryId: Long): Int

  @Query("SELECT COUNT(*) FROM categories")
  suspend fun count(): Int
}

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY name ASC")
  fun getAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
  fun getActiveProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE categoryId = :categoryId AND active = 1 ORDER BY name ASC")
  fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE id = :id")
  suspend fun getProductById(id: Long): ProductEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(product: ProductEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(products: List<ProductEntity>): List<Long>

  @Update
  suspend fun update(product: ProductEntity)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: Long)

  @Query("SELECT COUNT(*) FROM products")
  suspend fun count(): Int
}

@Dao
interface DailySessionDao {
  @Query("SELECT * FROM daily_sessions ORDER BY dateEpochDay DESC")
  fun getAllSessions(): Flow<List<DailySessionEntity>>

  @Query("SELECT * FROM daily_sessions WHERE projectId = :projectId ORDER BY dateEpochDay DESC")
  fun getSessionsByProject(projectId: Long): Flow<List<DailySessionEntity>>

  @Query("SELECT * FROM daily_sessions WHERE id = :id")
  suspend fun getSessionById(id: Long): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE dateString = :dateString LIMIT 1")
  suspend fun getSessionByDate(dateString: String): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE projectId = :projectId AND dateString = :dateString LIMIT 1")
  suspend fun getSessionByProjectAndDate(projectId: Long, dateString: String): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE status = 'OPEN' ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestOpenSession(): Flow<DailySessionEntity?>

  @Query("SELECT * FROM daily_sessions WHERE projectId = :projectId AND status = 'OPEN' ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestOpenSessionByProject(projectId: Long): Flow<DailySessionEntity?>

  @Query("SELECT * FROM daily_sessions WHERE status = 'CLOSED' ORDER BY dateEpochDay DESC LIMIT 1")
  suspend fun getLatestClosedSession(): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE projectId = :projectId AND status = 'CLOSED' ORDER BY dateEpochDay DESC LIMIT 1")
  suspend fun getLatestClosedSessionByProject(projectId: Long): DailySessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: DailySessionEntity): Long

  @Update
  suspend fun update(session: DailySessionEntity)

  @Query("SELECT * FROM daily_sessions ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestSession(): Flow<DailySessionEntity?>

  @Query("SELECT * FROM daily_sessions WHERE projectId = :projectId ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestSessionByProject(projectId: Long): Flow<DailySessionEntity?>

  @Query("UPDATE daily_sessions SET status = 'CLOSED', closedAt = :closedAt WHERE id = :sessionId")
  suspend fun closeSession(sessionId: Long, closedAt: Long)

  @Query("UPDATE daily_sessions SET status = 'OPEN', closedAt = NULL WHERE id = :sessionId")
  suspend fun reopenSession(sessionId: Long)
}

@Dao
interface DailyInventoryDao {
  @Query(
    """
    SELECT 
      di.id, di.sessionId, di.productId, p.name AS productName,
      p.categoryId, c.name AS categoryName, p.unitOfMeasure,
      di.openingStock, di.entries, di.closingStock, di.breakage,
      di.salePriceCents, di.costPriceCents
    FROM daily_inventory di
    INNER JOIN products p ON di.productId = p.id
    INNER JOIN categories c ON p.categoryId = c.id
    WHERE di.sessionId = :sessionId
    ORDER BY c.name ASC, p.name ASC
  """
  )
  fun getInventoryWithProductForSession(sessionId: Long): Flow<List<InventoryItemQueryResult>>

  @Query("SELECT * FROM daily_inventory WHERE sessionId = :sessionId AND productId = :productId LIMIT 1")
  suspend fun getItem(sessionId: Long, productId: Long): DailyInventoryEntity?

  @Query("SELECT * FROM daily_inventory WHERE sessionId = :sessionId")
  suspend fun getRawItemsForSession(sessionId: Long): List<DailyInventoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: DailyInventoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<DailyInventoryEntity>): List<Long>

  @Update
  suspend fun update(item: DailyInventoryEntity)

  @Query("DELETE FROM daily_inventory WHERE productId = :productId")
  suspend fun deleteByProductId(productId: Long)

  @Query("UPDATE daily_inventory SET entries = entries + :additionalEntries WHERE sessionId = :sessionId AND productId = :productId")
  suspend fun addEntries(sessionId: Long, productId: Long, additionalEntries: Double)

  @Query("UPDATE daily_inventory SET closingStock = :closingStock, breakage = :breakage WHERE sessionId = :sessionId AND productId = :productId")
  suspend fun updateCount(sessionId: Long, productId: Long, closingStock: Double, breakage: Double)
}
