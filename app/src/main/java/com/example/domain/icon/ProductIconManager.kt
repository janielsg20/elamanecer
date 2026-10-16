package com.example.domain.icon

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Soap
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor inteligente de iconos Material Design para productos comerciales.
 * - Resuelve el nombre del icono según el nombre del producto y categoría.
 * - Descarga el icono desde la CDN oficial de Google Fonts / Material Design.
 * - Lo guarda en el almacenamiento interno de la app (context.filesDir/product_icons/).
 * - No se vuelve a descargar si ya existe en el almacenamiento local.
 * - Ofrece un icono vectorial inmediato como fallback sin bloqueos visuales.
 */
object ProductIconManager {

  private val memoryCache = ConcurrentHashMap<String, ImageVector>()
  private val pathDataRegex = Regex("""android:pathData="([^"]+)"""")

  /**
   * Resuelve el identificador del icono de Material Design a partir del nombre del producto y categoría.
   */
  fun resolveIconName(productName: String, categoryName: String? = null): String {
    val text = normalize("${productName.lowercase()} ${categoryName?.lowercase() ?: ""}")

    if (text.containsAny("cerveza", "cristal", "bucanero", "corona", "heineken", "pilsen", "lager", "ale", "birra")) return "sports_bar"
    if (text.containsAny("vino", "tinto", "blanco", "champagne", "sidra", "espumante")) return "wine_bar"
    if (text.containsAny("ron", "vodka", "whisky", "whiskey", "tequila", "licor", "aguardiente", "ginebra", "gin", "coctel", "mojito", "trago", "havana")) return "local_bar"
    if (text.containsAny("refresco", "soda", "gaseosa", "coca", "pepsi", "sprite", "fanta", "jugo", "zumo", "malta", "bebida", "tuKola", "tukola")) return "local_drink"
    if (text.containsAny("agua", "mineral", "hielo")) return "water_drop"
    if (text.containsAny("cafe", "coffee", "espresso", "capuchino", "te", "infusion")) return "local_cafe"
    if (text.containsAny("pan", "baguette", "tostada", "harina", "croissant", "panaderia")) return "bakery_dining"
    if (text.containsAny("galleta", "cookie", "chocolate", "caramelo", "bombon", "dulce", "torta", "cake", "postre", "turron", "golosina")) return "cookie"
    if (text.containsAny("pizza", "hamburguesa", "sandwich", "bocadillo", "empanada", "hot dog", "perro")) return "lunch_dining"
    if (text.containsAny("snack", "papa", "papitas", "nacho", "chicharron", "chucheria", "platano frito")) return "fastfood"
    if (text.containsAny("carne", "pollo", "res", "cerdo", "jamon", "salchicha", "embutido", "chorizo", "bistec", "chuleta", "picadillo")) return "lunch_dining"
    if (text.containsAny("pescado", "atun", "sardina", "marisco", "camaron")) return "restaurant"
    if (text.containsAny("huevo", "leche", "queso", "yogur", "yogurt", "mantequilla", "crema")) return "egg"
    if (text.containsAny("helado", "paleta", "nieve")) return "icecream"
    if (text.containsAny("fruta", "manzana", "platano", "banana", "naranja", "limon", "mango", "pina", "verdura", "tomate", "cebolla", "vegetal", "aguacate", "vianda")) return "spa"
    if (text.containsAny("arroz", "frijol", "lenteja", "grano", "pasta", "espagueti", "macarron", "azucar", "sal", "aceite", "vinagre", "salsa", "mayonesa", "lata")) return "kitchen"
    if (text.containsAny("cigarro", "cigarrillo", "tabaco", "cuba", "cohiba", "fosforo", "encendedor", "cajetilla")) return "smoking_rooms"
    if (text.containsAny("limpieza", "detergente", "cloro", "lavaplatos", "desinfectante", "fregasuelos", "lejia", "suavizante")) return "sanitizer"
    if (text.containsAny("jabon", "shampoo", "champu", "desodorante", "colonia", "perfume", "dental", "pasta dental", "cepillo", "aseo")) return "soap"
    if (text.containsAny("papel", "higienico", "servilleta", "toalla", "panuelo")) return "clean_hands"
    if (text.containsAny("medicina", "pastilla", "farmacia", "curita", "jarabe", "alcohol", "remedio", "aspirina", "salud")) return "local_pharmacy"

    categoryName?.let { normalize(it.lowercase()) }?.let { cat ->
      if (cat.containsAny("bebida", "liquido", "bar", "cerveza")) return "sports_bar"
      if (cat.containsAny("alimento", "comida", "comestible", "cocina")) return "restaurant"
      if (cat.containsAny("aseo", "limpieza", "higiene")) return "soap"
      if (cat.containsAny("golosina", "dulce", "snack", "confiteria")) return "cookie"
      if (cat.containsAny("pan", "reposteria")) return "bakery_dining"
      if (cat.containsAny("cigarro", "tabaco")) return "smoking_rooms"
      if (cat.containsAny("farmacia", "salud")) return "local_pharmacy"
    }

    return "inventory_2"
  }

  /**
   * Obtiene el vector local predeterminado (garantía offline inmediata).
   */
  fun getFallbackVector(iconName: String): ImageVector {
    return when (iconName) {
      "sports_bar" -> Icons.Default.SportsBar
      "wine_bar" -> Icons.Default.WineBar
      "local_bar" -> Icons.Default.LocalBar
      "local_drink" -> Icons.Default.LocalDrink
      "water_drop" -> Icons.Default.WaterDrop
      "local_cafe" -> Icons.Default.LocalCafe
      "bakery_dining" -> Icons.Default.BakeryDining
      "cookie" -> Icons.Default.Cookie
      "lunch_dining" -> Icons.Default.LunchDining
      "fastfood" -> Icons.Default.Fastfood
      "restaurant" -> Icons.Default.Restaurant
      "egg" -> Icons.Default.Egg
      "icecream" -> Icons.Default.Icecream
      "spa" -> Icons.Default.Spa
      "kitchen" -> Icons.Default.Kitchen
      "smoking_rooms" -> Icons.Default.SmokingRooms
      "sanitizer" -> Icons.Default.Sanitizer
      "soap" -> Icons.Default.Soap
      "clean_hands" -> Icons.Default.CleanHands
      "local_pharmacy" -> Icons.Default.Medication
      "inventory_2" -> Icons.Default.Inventory2
      else -> Icons.Default.ShoppingBag
    }
  }

  fun hasLocalIcon(context: Context, iconName: String): Boolean {
    if (memoryCache.containsKey(iconName)) return true
    val iconFile = File(File(context.filesDir, "product_icons"), "$iconName.xml")
    return iconFile.exists()
  }

  fun getCachedOrLocalVector(context: Context, iconName: String): ImageVector {
    memoryCache[iconName]?.let { return it }
    val iconFile = File(File(context.filesDir, "product_icons"), "$iconName.xml")
    if (iconFile.exists()) {
      try {
        val xmlContent = iconFile.readText()
        val parsed = parseVectorFromXml(iconName, xmlContent)
        if (parsed != null) {
          memoryCache[iconName] = parsed
          return parsed
        }
      } catch (_: Exception) {}
    }
    return getFallbackVector(iconName)
  }

  /**
   * Obtiene el icono desde la memoria, del almacenamiento local o lo descarga una sola vez de Internet.
   * Al descargarlo, lo guarda permanentemente en la app para no volver a descargarlo.
   */
  suspend fun getOrFetchIcon(context: Context, iconName: String): ImageVector {
    // 1. Memoria RAM
    memoryCache[iconName]?.let { return it }

    val iconsDir = File(context.filesDir, "product_icons")
    if (!iconsDir.exists()) {
      iconsDir.mkdirs()
    }

    val iconFile = File(iconsDir, "$iconName.xml")

    // 2. Almacenamiento local interno permanente (si ya fue descargado previamente)
    if (iconFile.exists()) {
      try {
        val xmlContent = withContext(Dispatchers.IO) { iconFile.readText() }
        val parsed = parseVectorFromXml(iconName, xmlContent)
        if (parsed != null) {
          memoryCache[iconName] = parsed
          return parsed
        }
      } catch (_: Exception) {
        // En caso de corrupción o archivo parcial, reintenta descarga
      }
    }

    // 3. Descarga desde Internet (Google Fonts CDN) y guardado en almacenamiento interno
    return withContext(Dispatchers.IO) {
      val cdnUrls = listOf(
        "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/$iconName/default/24px.xml",
        "https://fonts.gstatic.com/s/i/materialicons/$iconName/v1/24px.xml"
      )

      for (cdnUrl in cdnUrls) {
        try {
          val url = URL(cdnUrl)
          val connection = url.openConnection() as HttpURLConnection
          connection.connectTimeout = 4000
          connection.readTimeout = 4000
          connection.requestMethod = "GET"
          connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")

          val responseCode = connection.responseCode
          if (responseCode == HttpURLConnection.HTTP_OK) {
            val xmlContent = connection.inputStream.bufferedReader().use { it.readText() }
            if (xmlContent.isNotBlank() && xmlContent.contains("pathData")) {
              Log.d("ProductIconManager", "Successfully downloaded icon $iconName from $cdnUrl")
              // Guardar permanentemente en almacenamiento interno para no descargar nunca más
              iconFile.writeText(xmlContent)

              val parsed = parseVectorFromXml(iconName, xmlContent)
              if (parsed != null) {
                memoryCache[iconName] = parsed
                return@withContext parsed
              }
            }
          } else {
            Log.w("ProductIconManager", "Failed to download icon $iconName from $cdnUrl, response code: $responseCode")
          }
        } catch (e: Exception) {
          Log.e("ProductIconManager", "Error downloading icon $iconName from $cdnUrl", e)
        }
      }

      val fallback = getFallbackVector(iconName)
      memoryCache[iconName] = fallback
      fallback
    }
  }

  /**
   * Parsea el pathData de un Vector XML de Android a un ImageVector de Compose.
   * Soporta múltiples paths en un mismo vector.
   */
  private fun parseVectorFromXml(iconName: String, xmlContent: String): ImageVector? {
    val matches = pathDataRegex.findAll(xmlContent).toList()
    if (matches.isEmpty()) return null

    return try {
      val builder = ImageVector.Builder(
        name = iconName,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      for (match in matches) {
        val pathData = match.groupValues[1]
        val pathNodes = PathParser().parsePathString(pathData).toNodes()
        builder.addPath(
          pathData = pathNodes,
          fill = SolidColor(Color.White),
        )
      }
      builder.build()
    } catch (_: Exception) {
      null
    }
  }

  private fun normalize(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
  }

  private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it) }
  }
}

/**
 * Componente visual Composable para mostrar el icono de un producto en cualquier pantalla.
 * Muestra un indicador de carga mientras se descarga de Internet si no está guardado localmente.
 */
@Composable
fun ProductIconView(
  productName: String,
  categoryName: String? = null,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  iconSize: Dp = 22.dp,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
  contentColor: Color = MaterialTheme.colorScheme.primary,
) {
  val context = LocalContext.current
  val iconName = remember(productName, categoryName) {
    ProductIconManager.resolveIconName(productName, categoryName)
  }

  val hasLocal = remember(iconName) {
    ProductIconManager.hasLocalIcon(context, iconName)
  }

  var isLoading by remember(iconName) {
    mutableStateOf(!hasLocal)
  }

  var iconVector by remember(iconName) {
    mutableStateOf(ProductIconManager.getCachedOrLocalVector(context, iconName))
  }

  LaunchedEffect(iconName) {
    if (!hasLocal) {
      isLoading = true
    }
    val fetched = ProductIconManager.getOrFetchIcon(context, iconName)
    iconVector = fetched
    isLoading = false
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(10.dp))
      .background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(iconSize * 0.8f),
        strokeWidth = 2.dp,
        color = contentColor,
      )
    } else {
      Icon(
        imageVector = iconVector,
        contentDescription = productName,
        tint = contentColor,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}
