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

  @Test
  fun testWarehouseStockFormulasAndCalculations() = runBlocking {
    repository.seedInitialDataIfEmpty()
    val warehouses = repository.getAllWarehouses().first()
    val warehouse = warehouses.first()

    // 1. Guardar un nuevo producto en almacén con los 8 campos:
    // Producto: "Jugo de Naranja 1L", Unidad: "Caja", Inicio: 100, Entrada: 50, Salida: 20, Costo: 3.50
    val savedId = repository.saveWarehouseStockItem(
      warehouseId = warehouse.id,
      productId = null,
      productName = "Jugo de Naranja 1L",
      unitOfMeasure = "Caja",
      initialStock = 100.0,
      entries = 50.0,
      exits = 20.0,
      costPrice = 3.50,
      minAlertStock = 15.0,
    )
    assertTrue(savedId > 0)

    val stockItems = repository.getWarehouseStock(warehouse.id).first()
    val jugoItem = checkNotNull(stockItems.find { it.productName == "Jugo de Naranja 1L" })

    // Validar fórmula: disponible = inicio + entrada - salida (100 + 50 - 20 = 130)
    assertEquals(100.0, jugoItem.initialStock, 0.001)
    assertEquals(50.0, jugoItem.entries, 0.001)
    assertEquals(20.0, jugoItem.exits, 0.001)
    assertEquals(130.0, jugoItem.availableStock, 0.001)
    assertEquals(130.0, jugoItem.currentStock, 0.001)
    assertEquals(3.50, jugoItem.costPrice, 0.001)

    // Validar fórmula: importe = disponible * costo (130 * 3.50 = 455.0)
    assertEquals(455.0, jugoItem.totalAmount, 0.001)
    assertEquals("$455.00", jugoItem.totalAmountFormatted)

    // 2. Probar entrada rápida (+20)
    repository.addWarehouseStockEntry(warehouse.id, jugoItem.productId, 20.0)
    val afterEntry = repository.getWarehouseStock(warehouse.id).first().find { it.productId == jugoItem.productId }!!
    assertEquals(70.0, afterEntry.entries, 0.001)
    assertEquals(150.0, afterEntry.availableStock, 0.001) // 100 + 70 - 20 = 150
    assertEquals(525.0, afterEntry.totalAmount, 0.001) // 150 * 3.50 = 525.0

    // 3. Probar salida rápida (-10)
    repository.addWarehouseStockExit(warehouse.id, jugoItem.productId, 10.0)
    val afterExit = repository.getWarehouseStock(warehouse.id).first().find { it.productId == jugoItem.productId }!!
    assertEquals(30.0, afterExit.exits, 0.001)
    assertEquals(140.0, afterExit.availableStock, 0.001) // 100 + 70 - 30 = 140
    assertEquals(490.0, afterExit.totalAmount, 0.001) // 140 * 3.50 = 490.0

    // 4. Probar despacho a un local donde el producto no existía en su sesión
    val projects = repository.getAllProjects().first()
    val barProject = projects.find { it.type == "BAR" }!!

    val dispatch = repository.dispatchStockToProject(
      warehouseId = warehouse.id,
      projectId = barProject.id,
      productId = jugoItem.productId,
      quantity = 25.0,
      notes = "Despacho jugo al bar",
    )
    assertEquals(25.0, dispatch.quantity, 0.001)
    assertEquals(115.0, dispatch.remainingStockInWarehouse, 0.001)

    // Verificar que en el local (proyecto) se creó la entrada en su sesión diaria
    val todayStr = com.example.domain.model.DateHelper.getTodayDateString()
    val session = repository.getSessionByDate(todayStr, barProject.id)
    assertNotNull(session)
    val localItems = repository.getSessionInventoryItems(session!!.id).first()
    val localJugo = localItems.find { it.productId == jugoItem.productId }
    assertNotNull(localJugo)
    assertEquals(25.0, localJugo!!.entries, 0.001)
    assertEquals(25.0, localJugo.closingStock, 0.001)
  }
}
