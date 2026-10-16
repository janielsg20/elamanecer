package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
  tableName = "daily_sessions",
  indices = [Index(value = ["dateString"], unique = true), Index(value = ["dateEpochDay"])],
)
data class DailySessionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
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
