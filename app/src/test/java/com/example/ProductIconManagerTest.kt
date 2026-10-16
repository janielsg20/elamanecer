package com.example

import com.example.domain.icon.ProductIconManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductIconManagerTest {

  @Test
  fun testBeerIconResolution() {
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Cerveza Cristal"))
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Bucanero Fuerte"))
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Corona Extra"))
  }

  @Test
  fun testWineAndLiquorResolution() {
    assertEquals("wine_bar", ProductIconManager.resolveIconName("Vino Tinto Cabernet"))
    assertEquals("local_bar", ProductIconManager.resolveIconName("Ron Havana Club 7 Años"))
  }

  @Test
  fun testDrinksAndWaterResolution() {
    assertEquals("local_drink", ProductIconManager.resolveIconName("Refresco Cola 355ml"))
    assertEquals("water_drop", ProductIconManager.resolveIconName("Agua Mineral 500ml"))
    assertEquals("local_cafe", ProductIconManager.resolveIconName("Café Serrano Molido"))
  }

  @Test
  fun testFoodAndBakeryResolution() {
    assertEquals("bakery_dining", ProductIconManager.resolveIconName("Pan de Molde"))
    assertEquals("cookie", ProductIconManager.resolveIconName("Galletas María"))
    assertEquals("fastfood", ProductIconManager.resolveIconName("Papas Fritas"))
    assertEquals("lunch_dining", ProductIconManager.resolveIconName("Pollo Entero"))
  }

  @Test
  fun testCleaningAndHygieneResolution() {
    assertEquals("sanitizer", ProductIconManager.resolveIconName("Detergente Omo 500g"))
    assertEquals("soap", ProductIconManager.resolveIconName("Jabón de Baño"))
    assertEquals("smoking_rooms", ProductIconManager.resolveIconName("Cigarros Criollos"))
  }

  @Test
  fun testCategoryFallback() {
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Producto Desconocido", "Bebidas Alcohólicas"))
    assertEquals("restaurant", ProductIconManager.resolveIconName("Producto X", "Alimentos Congelados"))
  }
}
