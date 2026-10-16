package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepositoryImpl
import com.example.domain.model.Project
import com.example.domain.model.Warehouse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WarehouseAndProjectTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: InventoryRepositoryImpl

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = InventoryRepositoryImpl(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testSeedingInitialProjectsAndWarehouses() = runBlocking {
    repository.seedInitialDataIfEmpty()

    val projects = repository.getAllProjects().first()
    assertEquals(3, projects.size)
    assertEquals("Kiosko Central", projects[0].name)
    assertEquals("KIOSKO", projects[0].type)
    assertEquals("Bar Tropical", projects[1].name)
    assertEquals("BAR", projects[1].type)
    assertEquals("Paladar Doña Rosa", projects[2].name)
    assertEquals("PALADAR", projects[2].type)

    val warehouses = repository.getAllWarehouses().first()
    assertEquals(3, warehouses.size)
    assertNotNull(warehouses.find { it.name == "Almacén Central Kioskos" })
    assertNotNull(warehouses.find { it.name == "Almacén Bebidas & Bar" })
    assertNotNull(warehouses.find { it.name == "Almacén Insumos Paladar" })
  }

  @Test
  fun testProjectCreationEditionDeletion() = runBlocking {
    val newId = repository.insertProject(
      Project(
        name = "Nuevo Kiosko Playa",
        type = "KIOSKO",
        description = "Punto playero",
        colorHex = "#00897B",
      )
    )
    assertTrue(newId > 0)

    val created = repository.getProjectById(newId)
    assertNotNull(created)
    assertEquals("Nuevo Kiosko Playa", created!!.name)

    repository.updateProject(created.copy(name = "Kiosko Playa Azul"))
    val updated = repository.getProjectById(newId)
    assertEquals("Kiosko Playa Azul", updated!!.name)

    repository.deleteProject(newId)
    val deleted = repository.getProjectById(newId)
    assertEquals(null, deleted)
  }

  @Test
  fun testWarehouseDispatchStockToProject() = runBlocking {
    repository.seedInitialDataIfEmpty()

    val warehouses = repository.getAllWarehouses().first()
    val warehouseKioskos = checkNotNull(warehouses.find { it.name == "Almacén Central Kioskos" })
    val projects = repository.getAllProjects().first()
    val kioskoProject = checkNotNull(projects.find { it.name == "Kiosko Central" })

    // TuKola is product id 4
    val initialStock = repository.getWarehouseStock(warehouseKioskos.id).first()
    val tuKolaItem = initialStock.find { it.productName.contains("TuKola") }
    assertNotNull(tuKolaItem)
    val stockBefore = tuKolaItem!!.currentStock // 500

    // Despacho de 45 refrescos para el Kiosko
    val dispatch = repository.dispatchStockToProject(
      warehouseId = warehouseKioskos.id,
      projectId = kioskoProject.id,
      productId = tuKolaItem.productId,
      quantity = 45.0,
      notes = "Despacho matutino al kiosko",
    )

    assertEquals(45.0, dispatch.quantity, 0.001)
    assertEquals(stockBefore - 45.0, dispatch.remainingStockInWarehouse, 0.001)

    // Verificar que el stock en el almacén disminuyó exactamente en 45
    val stockAfter = repository.getWarehouseStock(warehouseKioskos.id).first()
    val tuKolaItemAfter = stockAfter.find { it.productId == tuKolaItem.productId }
    assertEquals(stockBefore - 45.0, tuKolaItemAfter!!.currentStock, 0.001)

    // Verificar que el historial de despachos registró el movimiento
    val dispatches = repository.getAllDispatches().first()
    assertTrue(dispatches.any { it.id == dispatch.id && it.quantity == 45.0 })
  }
}
