package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.icon.ProductIconManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductIconManagerTest {

  @Test
  fun testBeerIconResolution() {
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Cerveza Cristal"))
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Bucanero Fuerte"))
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Corona Extra"))
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Cerveza Heineken Lata"))
  }

  @Test
  fun testWineAndLiquorResolution() {
    assertEquals("wine_bar", ProductIconManager.resolveIconName("Vino Tinto Cabernet"))
    assertEquals("wine_bar", ProductIconManager.resolveIconName("Sidra Española"))
    assertEquals("local_bar", ProductIconManager.resolveIconName("Ron Havana Club 7 Años"))
    assertEquals("local_bar", ProductIconManager.resolveIconName("Vodka Smirnoff 750ml"))
    assertEquals("local_bar", ProductIconManager.resolveIconName("Whisky Chivas Regal"))
  }

  @Test
  fun testDrinksAndWaterResolution() {
    assertEquals("local_drink", ProductIconManager.resolveIconName("Refresco Cola 355ml"))
    assertEquals("local_drink", ProductIconManager.resolveIconName("Refresco TuKola 330ml"))
    assertEquals("local_drink", ProductIconManager.resolveIconName("Jugo de Naranja 1L"))
    assertEquals("water_drop", ProductIconManager.resolveIconName("Agua Mineral 500ml"))
    assertEquals("water_drop", ProductIconManager.resolveIconName("Hielo en Bolsa"))
    assertEquals("local_cafe", ProductIconManager.resolveIconName("Café Serrano Molido"))
  }

  @Test
  fun testFoodAndBakeryResolution() {
    assertEquals("bakery_dining", ProductIconManager.resolveIconName("Pan de Molde"))
    assertEquals("cookie", ProductIconManager.resolveIconName("Galletas María"))
    assertEquals("fastfood", ProductIconManager.resolveIconName("Papas Fritas"))
    assertEquals("fastfood", ProductIconManager.resolveIconName("Maní Salado 100g"))
    assertEquals("lunch_dining", ProductIconManager.resolveIconName("Pollo Entero"))
    assertEquals("lunch_dining", ProductIconManager.resolveIconName("Jamón Viking"))
    assertEquals("restaurant", ProductIconManager.resolveIconName("Filete de Pescado"))
    assertEquals("egg", ProductIconManager.resolveIconName("Queso Gouda Holandés"))
  }

  @Test
  fun testCleaningAndHygieneResolution() {
    assertEquals("sanitizer", ProductIconManager.resolveIconName("Detergente Omo 500g"))
    assertEquals("soap", ProductIconManager.resolveIconName("Jabón de Baño"))
    assertEquals("soap", ProductIconManager.resolveIconName("Pasta Dental Colgate"))
    assertEquals("clean_hands", ProductIconManager.resolveIconName("Papel Higiénico"))
    assertEquals("smoking_rooms", ProductIconManager.resolveIconName("Cigarros Criollos"))
    assertEquals("smoking_rooms", ProductIconManager.resolveIconName("Encendedor Candela"))
  }

  @Test
  fun testCategoryFallback() {
    assertEquals("sports_bar", ProductIconManager.resolveIconName("Producto Desconocido", "Bebidas Alcohólicas"))
    assertEquals("restaurant", ProductIconManager.resolveIconName("Producto X", "Alimentos Congelados"))
    assertEquals("soap", ProductIconManager.resolveIconName("Producto Y", "Aseo Personal"))
  }

  @Test
  fun testParseVectorFromXml() {
    val xml = """
      <vector xmlns:android="http://schemas.android.com/apk/res/android"
          android:width="24dp"
          android:height="24dp"
          android:viewportWidth="960"
          android:viewportHeight="960">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="M320,760L600,760L600,360L520,360Q492,360 474,374Q456,388 431,415Q411,437 384.5,460.5Q358,484 320,495L320,760ZM240,840L240,494Q188,480 154,438Q120,396 120,340L680,680Z"/>
      </vector>
    """.trimIndent()
    val parsed = ProductIconManager.parseVectorFromXml("sports_bar", xml)
    assertNotNull(parsed)
    assertEquals(960f, parsed!!.viewportWidth)
    val rootGroup = parsed.root
    val firstPath = rootGroup.iterator().next() as androidx.compose.ui.graphics.vector.VectorPath
    println("Path nodes count: ${firstPath.pathData.size}")
    println("Path nodes first 5: ${firstPath.pathData.take(5)}")
  }

  @Test
  fun testDownloadAndCacheSingleIcon() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val icon = ProductIconManager.getOrFetchIcon(context, "sports_bar")
    assertNotNull(icon)
    assertTrue(ProductIconManager.hasDownloadedIcon(context, "sports_bar"))

    // Second call should return from memory/disk immediately
    val cached = ProductIconManager.getCachedOrLocalVector(context, "sports_bar")
    assertNotNull(cached)

    // Verify the ImageVector has valid paths
    assertTrue("Should have non-empty root group", cached.root.iterator().hasNext())
  }

  @Test
  fun testAllIconsParsing() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    for (iconName in ProductIconManager.ALL_SUPPORTED_ICONS) {
      val icon = ProductIconManager.getOrFetchIcon(context, iconName)
      assertNotNull("Icon $iconName should not be null", icon)
      assertTrue("Icon $iconName should be downloaded", ProductIconManager.hasDownloadedIcon(context, iconName))
    }
    assertEquals(ProductIconManager.ALL_SUPPORTED_ICONS.size, ProductIconManager.getDownloadedIconsCount(context))
  }
}
