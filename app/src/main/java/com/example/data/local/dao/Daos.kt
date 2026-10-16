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

  @Query("SELECT * FROM daily_sessions WHERE id = :id")
  suspend fun getSessionById(id: Long): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE dateString = :dateString LIMIT 1")
  suspend fun getSessionByDate(dateString: String): DailySessionEntity?

  @Query("SELECT * FROM daily_sessions WHERE status = 'OPEN' ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestOpenSession(): Flow<DailySessionEntity?>

  @Query("SELECT * FROM daily_sessions WHERE status = 'CLOSED' ORDER BY dateEpochDay DESC LIMIT 1")
  suspend fun getLatestClosedSession(): DailySessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: DailySessionEntity): Long

  @Update
  suspend fun update(session: DailySessionEntity)

  @Query("SELECT * FROM daily_sessions ORDER BY dateEpochDay DESC LIMIT 1")
  fun getLatestSession(): Flow<DailySessionEntity?>

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
