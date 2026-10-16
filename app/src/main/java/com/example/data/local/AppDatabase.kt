package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.DailyInventoryDao
import com.example.data.local.dao.DailySessionDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DailyInventoryEntity
import com.example.data.local.entity.DailySessionEntity
import com.example.data.local.entity.ProductEntity

@Database(
  entities = [
    CategoryEntity::class,
    ProductEntity::class,
    DailySessionEntity::class,
    DailyInventoryEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun categoryDao(): CategoryDao
  abstract fun productDao(): ProductDao
  abstract fun dailySessionDao(): DailySessionDao
  abstract fun dailyInventoryDao(): DailyInventoryDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "sales_inventory_database",
          )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
