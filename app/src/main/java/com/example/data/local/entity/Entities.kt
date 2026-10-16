package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val type: String = "KIOSKO", // KIOSKO, BAR, PALADAR, OTRO
  val description: String = "",
  val colorHex: String = "#1E88E5",
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val targetType: String = "GENERAL", // KIOSKOS, BAR, PALADAR, GENERAL
  val location: String = "",
  val notes: String = "",
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val description: String = "",
  val active: Boolean = true,
)

@Entity(
  tableName = "products",
  foreignKeys = [
    ForeignKey(
      entity = CategoryEntity::class,
      parentColumns = ["id"],
      childColumns = ["categoryId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["categoryId"]), Index(value = ["name"])],
)
data class ProductEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val categoryId: Long,
  val name: String,
  val unitOfMeasure: String = "U",
  val salePriceCents: Long = 0L,
  val costPriceCents: Long = 0L,
  val minStock: Double = 0.0,
  val maxStock: Double = 0.0,
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "warehouse_stock",
  foreignKeys = [
    ForeignKey(
      entity = WarehouseEntity::class,
      parentColumns = ["id"],
      childColumns = ["warehouseId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = ProductEntity::class,
      parentColumns = ["id"],
      childColumns = ["productId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["warehouseId", "productId"], unique = true),
    Index(value = ["warehouseId"]),
    Index(value = ["productId"]),
  ],
)
data class WarehouseStockEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val warehouseId: Long,
  val productId: Long,
  val initialStock: Double = 0.0,
  val entries: Double = 0.0,
  val exits: Double = 0.0,
  val currentStock: Double = 0.0,
  val costPriceCents: Long = 0L,
  val minAlertStock: Double = 10.0,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "warehouse_dispatches",
  foreignKeys = [
    ForeignKey(
      entity = WarehouseEntity::class,
      parentColumns = ["id"],
      childColumns = ["warehouseId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = ProjectEntity::class,
      parentColumns = ["id"],
      childColumns = ["projectId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = ProductEntity::class,
      parentColumns = ["id"],
      childColumns = ["productId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["warehouseId"]),
    Index(value = ["projectId"]),
    Index(value = ["productId"]),
    Index(value = ["timestamp"]),
  ],
)
data class WarehouseDispatchEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val warehouseId: Long,
  val projectId: Long,
  val productId: Long,
  val quantity: Double,
  val remainingStockInWarehouse: Double,
  val dateString: String,
  val timestamp: Long = System.currentTimeMillis(),
  val notes: String = "",
)

@Entity(
  tableName = "daily_sessions",
  foreignKeys = [
    ForeignKey(
      entity = ProjectEntity::class,
      parentColumns = ["id"],
      childColumns = ["projectId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["projectId", "dateString"], unique = true),
    Index(value = ["projectId"]),
    Index(value = ["dateEpochDay"]),
  ],
)
data class DailySessionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val projectId: Long = 1L,
  val dateString: String,
  val dateEpochDay: Long,
  val status: String = "OPEN", // OPEN, CLOSED
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val closedAt: Long? = null,
)

@Entity(
  tableName = "daily_inventory",
  foreignKeys = [
    ForeignKey(
      entity = DailySessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = ProductEntity::class,
      parentColumns = ["id"],
      childColumns = ["productId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["sessionId", "productId"], unique = true),
    Index(value = ["sessionId"]),
    Index(value = ["productId"]),
  ],
)
data class DailyInventoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val sessionId: Long,
  val productId: Long,
  val openingStock: Double = 0.0,
  val entries: Double = 0.0,
  val closingStock: Double = 0.0,
  val breakage: Double = 0.0,
  val salePriceCents: Long = 0L,
  val costPriceCents: Long = 0L,
)
