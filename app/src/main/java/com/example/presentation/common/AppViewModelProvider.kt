package com.example.presentation.common

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepositoryImpl
import com.example.domain.repository.InventoryRepository
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.history.ReportsViewModel
import com.example.presentation.inventory.InventoryViewModel
import com.example.presentation.products.ProductsViewModel
import com.example.presentation.quicksale.QuickSaleViewModel

object AppContainer {
  private var repositoryInstance: InventoryRepository? = null

  fun getRepository(application: Application): InventoryRepository {
    return repositoryInstance ?: synchronized(this) {
      val db = AppDatabase.getDatabase(application)
      val repo = InventoryRepositoryImpl(db)
      repositoryInstance = repo
      repo
    }
  }
}

object AppViewModelProvider {
  val Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
      val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
      val repository = AppContainer.getRepository(application)

      return when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
          DashboardViewModel(repository) as T
        modelClass.isAssignableFrom(InventoryViewModel::class.java) ->
          InventoryViewModel(repository) as T
        modelClass.isAssignableFrom(QuickSaleViewModel::class.java) ->
          QuickSaleViewModel(repository) as T
        modelClass.isAssignableFrom(ProductsViewModel::class.java) ->
          ProductsViewModel(repository) as T
        modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
          ReportsViewModel(repository) as T
        modelClass.isAssignableFrom(com.example.presentation.warehouse.WarehouseViewModel::class.java) ->
          com.example.presentation.warehouse.WarehouseViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
      }
    }
  }
}
