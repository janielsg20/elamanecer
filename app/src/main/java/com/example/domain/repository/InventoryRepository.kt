package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DaySummary
import com.example.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
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

  // Daily Sessions
  fun getAllSessions(): Flow<List<DailySession>>
  fun getLatestOpenSession(): Flow<DailySession?>
  fun getLatestSession(): Flow<DailySession?>
  suspend fun getSessionById(sessionId: Long): DailySession?
  suspend fun getSessionByDate(dateString: String): DailySession?
  suspend fun getOrCreateSessionForDate(dateString: String, dateEpochDay: Long): DailySession
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
