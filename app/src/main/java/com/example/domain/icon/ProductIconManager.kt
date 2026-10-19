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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Gestor inteligente y reactivo de iconos Material Design para productos comerciales.
 * - Resuelve el nombre del icono según el nombre del producto y categoría con normalización de acentos.
 * - Descarga vectores XML limpios desde la CDN oficial de Google Fonts / Material Symbols.
 * - Almacenamiento atómico en almacenamiento interno de la app (context.filesDir/product_icons/).
 * - Flujo reactivo StateFlow para que Compose actualice inmediatamente la UI al completarse la descarga.
 * - Soporte concurrente sin colisiones mediante Mutex por icono.
 * - Fallbacks vectoriales locales inmediatos sin bloqueos de pantalla ni spinners.
 */
object ProductIconManager {

  private val memoryCache = ConcurrentHashMap<String, ImageVector>()
  private val downloadMutexes = ConcurrentHashMap<String, Mutex>()

  // Flujo reactivo observado por Composables para actualizarse tan pronto un icono es descargado
  private val _downloadedIcons = MutableStateFlow<Map<String, ImageVector>>(emptyMap())
  val downloadedIcons: StateFlow<Map<String, ImageVector>> = _downloadedIcons.asStateFlow()

  private val pathDataRegex = Regex("""android:pathData=["']([^"']+)["']""")
  private val viewportWidthRegex = Regex("""android:viewportWidth=["']([0-9.]+)["']""")
  private val viewportHeightRegex = Regex("""android:viewportHeight=["']([0-9.]+)["']""")
  private val fillTypeRegex = Regex("""android:fillType=["']([^"']+)["']""")

  val ALL_SUPPORTED_ICONS = listOf(
    "sports_bar", "wine_bar", "local_bar", "local_drink", "water_drop",
    "local_cafe", "bakery_dining", "cookie", "lunch_dining", "fastfood",
    "restaurant", "egg", "icecream", "spa", "kitchen", "smoking_rooms",
    "sanitizer", "soap", "clean_hands", "local_pharmacy", "inventory_2"
  )

  private fun getMutexFor(iconName: String): Mutex {
    return downloadMutexes.computeIfAbsent(iconName) { Mutex() }
  }

  /**
   * Resuelve el identificador del icono de Material Design a partir del nombre del producto y categoría.
   */
  fun resolveIconName(productName: String, categoryName: String? = null): String {
    val prodText = normalize(productName.lowercase().trim())

    val iconFromProduct = matchProductText(prodText)
    if (iconFromProduct != null) {
      return iconFromProduct
    }

    categoryName?.let { normalize(it.lowercase().trim()) }?.let { cat ->
      if (cat.containsAny("cerveza", "bar", "licor", "alcohol", "alcoholica", "alcoholicas", "alcoholico", "alcoholicos", "trago")) return "sports_bar"
      if (cat.containsAny("bebida", "liquido", "refresco", "jugo", "soda")) return "local_drink"
      if (cat.containsAny("alimento", "comida", "comestible", "cocina", "restaurante")) return "restaurant"
      if (cat.containsAny("aseo", "limpieza", "higiene", "personal")) return "soap"
      if (cat.containsAny("golosina", "dulce", "snack", "confiteria", "chucheria")) return "cookie"
      if (cat.containsAny("pan", "reposteria", "harina")) return "bakery_dining"
      if (cat.containsAny("cigarro", "tabaco", "fumador")) return "smoking_rooms"
      if (cat.containsAny("farmacia", "salud", "medicina")) return "local_pharmacy"

      val iconFromCategory = matchProductText(cat)
      if (iconFromCategory != null) {
        return iconFromCategory
      }
    }

    return "inventory_2"
  }

  private fun matchProductText(text: String): String? {
    // 1. Cervezas
    if (text.containsAny("cerveza", "cristal", "bucanero", "corona", "heineken", "pilsen", "lager", "ale", "birra", "chopp")) return "sports_bar"
    
    // 2. Vinos y Champagne
    if (text.containsAny("vino", "tinto", "blanco", "champagne", "sidra", "espumante", "cava", "lambrusco", "rose", "oporto")) return "wine_bar"
    
    // 3. Licores fuertes y Cocteles
    if (text.containsAny("ron", "vodka", "whisky", "whiskey", "tequila", "licor", "aguardiente", "ginebra", "gin", "coctel", "mojito", "trago", "havana", "santiago", "caney", "smirnoff", "bacardi", "campari", "vermouth", "cognac", "brandy")) return "local_bar"
    
    // 4. Refrescos, Jugos y Bebidas No Alcohólicas
    if (text.containsAny("refresco", "soda", "gaseosa", "coca", "pepsi", "sprite", "fanta", "jugo", "zumo", "malta", "tukola", "energizante", "red bull", "nectar")) return "local_drink"
    
    // 5. Agua y Hielo
    if (text.containsAny("agua", "mineral", "hielo", "agua mineral")) return "water_drop"
    
    // 6. Café y Té
    if (text.containsAny("cafe", "coffee", "espresso", "capuchino", "cortado", "latte", "te", "infusion", "manzanilla", "serrano")) return "local_cafe"
    
    // 7. Panadería y Harinas
    if (text.containsAny("pan", "baguette", "tostada", "harina", "croissant", "panaderia", "flauta", "panecillo")) return "bakery_dining"
    
    // 8. Dulces, Chocolates y Galletas
    if (text.containsAny("galleta", "cookie", "chocolate", "caramelo", "bombon", "dulce", "torta", "cake", "postre", "turron", "golosina", "confitura", "mermelada")) return "cookie"
    
    // 9. Snacks y Frutos Secos (incluye Maní)
    if (text.containsAny("snack", "papa", "papitas", "nacho", "chicharron", "chucheria", "platano frito", "mariquitas", "mani", "almendra", "nuez", "pistacho", "palomitas")) return "fastfood"
    
    // 10. Comidas preparadas, Carnes y Embutidos
    if (text.containsAny("pizza", "hamburguesa", "sandwich", "bocadillo", "empanada", "hot dog", "perro caliente", "carne", "pollo", "res", "cerdo", "jamon", "salchicha", "embutido", "chorizo", "bistec", "chuleta", "picadillo", "costilla", "lomo")) return "lunch_dining"
    
    // 11. Pescados y Mariscos
    if (text.containsAny("pescado", "atun", "sardina", "marisco", "camaron", "langosta", "pulpo", "calamar", "filete")) return "restaurant"
    
    // 12. Lácteos y Huevos
    if (text.containsAny("huevo", "leche", "queso", "yogur", "yogurt", "mantequilla", "crema", "nata", "gouda")) return "egg"
    
    // 13. Helados
    if (text.containsAny("helado", "paleta", "nieve", "barquillo", "sorbete")) return "icecream"
    
    // 14. Frutas y Vegetales
    if (text.containsAny("fruta", "manzana", "platano", "banana", "naranja", "limon", "mango", "pina", "verdura", "tomate", "cebolla", "vegetal", "aguacate", "vianda", "ensalada", "lechuga", "pepino", "yuca", "malanga")) return "spa"
    
    // 15. Aseo personal y Dental (prioridad alta antes de pastas de comida)
    if (text.containsAny("jabon", "shampoo", "champu", "desodorante", "colonia", "perfume", "dental", "pasta dental", "cepillo", "colgate", "gel", "acondicionador", "talco", "afeitadora")) return "soap"
    
    // 16. Papel y Desechables
    if (text.containsAny("papel", "higienico", "servilleta", "toalla", "panuelo", "sanitario")) return "clean_hands"
    
    // 17. Limpieza del hogar
    if (text.containsAny("limpieza", "detergente", "cloro", "lavaplatos", "desinfectante", "fregasuelos", "lejia", "suavizante", "jabon en polvo")) return "sanitizer"
    
    // 18. Abarrotes, Granos y Despensa
    if (text.containsAny("arroz", "frijol", "lenteja", "grano", "pasta", "espagueti", "macarron", "fideo", "azucar", "sal", "aceite", "vinagre", "salsa", "mayonesa", "ketchup", "mostaza", "lata", "conserva", "pure", "condimento")) return "kitchen"
    
    // 19. Cigarros y Tabaco
    if (text.containsAny("cigarro", "cigarrillo", "tabaco", "cuba", "cohiba", "upmann", "popular", "fosforo", "encendedor", "cajetilla", "mechero")) return "smoking_rooms"
    
    // 20. Farmacia y Salud
    if (text.containsAny("medicina", "pastilla", "farmacia", "curita", "jarabe", "alcohol", "remedio", "aspirina", "paracetamol", "salud", "analgesico", "botiquin", "vitamina")) return "local_pharmacy"
    
    return null
  }

  /**
   * Obtiene el vector local predeterminado de Material Design (garantía offline instantánea).
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

  val ICON_LABELS = mapOf(
    "sports_bar" to "Cervezas y Bares",
    "wine_bar" to "Vinos y Licores",
    "local_bar" to "Tragos y Coctelería",
    "local_drink" to "Refrescos y Jugos",
    "water_drop" to "Agua y Hielo",
    "local_cafe" to "Café y Té",
    "bakery_dining" to "Panadería y Dulces",
    "cookie" to "Galletas y Golosinas",
    "lunch_dining" to "Comidas y Carnes",
    "fastfood" to "Snacks y Comida Rápida",
    "restaurant" to "Pescados y Mariscos",
    "egg" to "Lácteos y Huevos",
    "icecream" to "Helados y Postres",
    "spa" to "Frutas y Vegetales",
    "kitchen" to "Abarrotes y Despensa",
    "smoking_rooms" to "Tabaco y Cigarros",
    "sanitizer" to "Limpieza del Hogar",
    "soap" to "Aseo Personal",
    "clean_hands" to "Higiene y Desechables",
    "local_pharmacy" to "Farmacia y Salud",
    "inventory_2" to "Artículos Generales",
  )

  /**
   * Comprueba si el icono está disponible localmente (en memoria RAM, almacenamiento interno o assets empaquetados).
   */
  fun hasDownloadedIcon(context: Context, iconName: String): Boolean {
    val safeName = iconName.lowercase().trim()
    if (memoryCache.containsKey(safeName)) return true
    val iconFile = File(File(context.filesDir, "product_icons"), "$safeName.xml")
    if (iconFile.exists() && iconFile.length() > 0) return true
    return isAssetIconAvailable(context, safeName)
  }

  fun hasLocalIcon(context: Context, iconName: String): Boolean = hasDownloadedIcon(context, iconName)

  private fun isAssetIconAvailable(context: Context, safeName: String): Boolean {
    return try {
      context.assets.open("product_icons/$safeName.xml").use { true }
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Carga inmediatamente al inicio de la app todos los iconos locales y de assets en memoria y disco.
   */
  fun loadExistingLocalIcons(context: Context) {
    try {
      val iconsDir = File(context.filesDir, "product_icons")
      if (!iconsDir.exists()) {
        iconsDir.mkdirs()
      }

      val newlyLoaded = mutableMapOf<String, ImageVector>()

      // 1. Cargar desde assets empaquetados primero para garantizar disponibilidad 100% offline
      for (iconName in ALL_SUPPORTED_ICONS) {
        val safeName = iconName.lowercase().trim()
        val diskFile = File(iconsDir, "$safeName.xml")

        var xmlContent: String? = null
        if (diskFile.exists() && diskFile.length() > 0) {
          try {
            xmlContent = diskFile.readText()
          } catch (_: Exception) {}
        }

        if (xmlContent.isNullOrBlank()) {
          try {
            xmlContent = context.assets.open("product_icons/$safeName.xml").bufferedReader().use { it.readText() }
            if (!xmlContent.isNullOrBlank()) {
              diskFile.writeText(xmlContent)
            }
          } catch (_: Exception) {}
        }

        if (!xmlContent.isNullOrBlank()) {
          val parsed = parseVectorFromXml(safeName, xmlContent)
          if (parsed != null) {
            memoryCache[safeName] = parsed
            newlyLoaded[safeName] = parsed
          }
        }
      }

      // 2. Cargar cualquier otro icono XML adicional en el directorio product_icons
      val diskFiles = iconsDir.listFiles { f -> f.extension == "xml" } ?: emptyArray()
      for (file in diskFiles) {
        val safeName = file.nameWithoutExtension
        if (!memoryCache.containsKey(safeName) && file.length() > 0) {
          try {
            val xml = file.readText()
            val parsed = parseVectorFromXml(safeName, xml)
            if (parsed != null) {
              memoryCache[safeName] = parsed
              newlyLoaded[safeName] = parsed
            }
          } catch (_: Exception) {}
        }
      }

      if (newlyLoaded.isNotEmpty()) {
        _downloadedIcons.update { it + newlyLoaded }
      }
    } catch (e: Exception) {
      Log.w("ProductIconManager", "Error loading existing local icons", e)
    }
  }

  /**
   * Inicializa la carga de todos los iconos comerciales disponibles en el sistema.
   */
  fun initialize(context: Context) {
    loadExistingLocalIcons(context)
  }

  /**
   * Obtiene el vector desde la memoria RAM, almacenamiento interno, assets empaquetados,
   * o el fallback vectorial predeterminado.
   */
  fun getCachedOrLocalVector(context: Context, iconName: String): ImageVector {
    val safeName = iconName.lowercase().trim()
    memoryCache[safeName]?.let { return it }

    val iconsDir = File(context.filesDir, "product_icons")
    val iconFile = File(iconsDir, "$safeName.xml")
    if (iconFile.exists() && iconFile.length() > 0) {
      try {
        val xmlContent = iconFile.readText()
        val parsed = parseVectorFromXml(safeName, xmlContent)
        if (parsed != null) {
          memoryCache[safeName] = parsed
          _downloadedIcons.update { it + (safeName to parsed) }
          return parsed
        } else {
          iconFile.delete()
        }
      } catch (_: Exception) {
        iconFile.delete()
      }
    }

    // Intentar leer desde assets
    try {
      val assetXml = context.assets.open("product_icons/$safeName.xml").bufferedReader().use { it.readText() }
      val parsed = parseVectorFromXml(safeName, assetXml)
      if (parsed != null) {
        if (!iconsDir.exists()) iconsDir.mkdirs()
        iconFile.writeText(assetXml)
        memoryCache[safeName] = parsed
        _downloadedIcons.update { it + (safeName to parsed) }
        return parsed
      }
    } catch (_: Exception) {}

    return getFallbackVector(safeName)
  }

  /**
   * Obtiene el icono desde memoria, almacenamiento local o lo descarga de Internet de forma segura.
   * Utiliza sincronización Mutex para evitar descargas duplicadas simultáneas y escritura atómica.
   */
  suspend fun getOrFetchIcon(context: Context, iconName: String): ImageVector {
    val safeName = iconName.lowercase().trim()

    // 1. Memoria RAM
    memoryCache[safeName]?.let { return it }

    val mutex = getMutexFor(safeName)
    return mutex.withLock {
      // Doble verificación dentro del lock
      memoryCache[safeName]?.let { return@withLock it }

      val iconsDir = File(context.filesDir, "product_icons")
      if (!iconsDir.exists()) {
        iconsDir.mkdirs()
      }

      val iconFile = File(iconsDir, "$safeName.xml")

      // 2. Almacenamiento local interno permanente
      if (iconFile.exists() && iconFile.length() > 0) {
        try {
          val xmlContent = withContext(Dispatchers.IO) { iconFile.readText() }
          val parsed = parseVectorFromXml(safeName, xmlContent)
          if (parsed != null) {
            memoryCache[safeName] = parsed
            _downloadedIcons.update { it + (safeName to parsed) }
            return@withLock parsed
          } else {
            iconFile.delete()
          }
        } catch (_: Exception) {
          iconFile.delete()
        }
      }

      // 3. Descarga desde Internet (Google Fonts CDN con múltiples espejos)
      val downloaded = withContext(Dispatchers.IO) {
        val cdnUrls = listOf(
          "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/$safeName/default/24px.xml",
          "https://fonts.gstatic.com/s/i/materialsymbolsoutlined/$safeName/24px.xml",
          "https://fonts.gstatic.com/s/i/materialicons/$safeName/v1/24px.xml",
          "https://fonts.gstatic.com/s/i/materialsymbolsrounded/$safeName/default/24px.xml",
          "https://fonts.gstatic.com/s/i/materialiconsoutlined/$safeName/v1/24px.xml"
        )

        for (cdnUrl in cdnUrls) {
          try {
            val url = URL(cdnUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connection.setRequestProperty("Accept", "application/xml,text/xml,*/*")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
              val xmlContent = connection.inputStream.bufferedReader().use { it.readText() }
              if (xmlContent.isNotBlank() && xmlContent.contains("pathData")) {
                val parsed = parseVectorFromXml(safeName, xmlContent)
                if (parsed != null) {
                  // Guardado atómico: escribir en .tmp y renombrar para evitar lecturas de archivo incompleto
                  val tmpFile = File(iconsDir, "$safeName.tmp")
                  tmpFile.writeText(xmlContent)
                  if (!tmpFile.renameTo(iconFile)) {
                    iconFile.writeText(xmlContent)
                    tmpFile.delete()
                  }

                  memoryCache[safeName] = parsed
                  _downloadedIcons.update { it + (safeName to parsed) }
                  Log.d("ProductIconManager", "Successfully downloaded and saved icon $safeName from $cdnUrl")
                  return@withContext parsed
                }
              }
            }
          } catch (e: Exception) {
            Log.w("ProductIconManager", "Attempt failed for icon $safeName from $cdnUrl: ${e.message}")
          }
        }
        null
      }

      if (downloaded != null) {
        return@withLock downloaded
      }

      // Si falló temporalmente la descarga de red, devolver el fallback SIN contaminar memoryCache
      // para permitir reintentos automáticos posteriores.
      getFallbackVector(safeName)
    }
  }

  /**
   * Pre-descarga concurrente en segundo plano de los iconos comerciales para que estén listos.
   */
  suspend fun preloadDefaultIcons(context: Context) = withContext(Dispatchers.IO) {
    // 1. Cargar existentes en disco de inmediato
    loadExistingLocalIcons(context)

    // 2. Descargar los que falten concurrentemente
    coroutineScope {
      ALL_SUPPORTED_ICONS.map { iconName ->
        async {
          try {
            getOrFetchIcon(context, iconName)
          } catch (e: Exception) {
            Log.w("ProductIconManager", "Preload error for $iconName", e)
          }
        }
      }.awaitAll()
    }
  }

  /**
   * Cuenta cuántos de los iconos soportados están actualmente descargados en disco o memoria.
   */
  fun getDownloadedIconsCount(context: Context): Int {
    return ALL_SUPPORTED_ICONS.count { hasDownloadedIcon(context, it) }
  }

  /**
   * Parsea el pathData de un Vector XML de Android a un ImageVector de Compose.
   * Maneja tanto viewport de 960x960 (Material Symbols) como 24x24 (Material Icons).
   */
  fun parseVectorFromXml(iconName: String, xmlContent: String): ImageVector? {
    val matches = pathDataRegex.findAll(xmlContent).toList()
    if (matches.isEmpty()) return null

    val vpWidth = viewportWidthRegex.find(xmlContent)?.groupValues?.get(1)?.toFloatOrNull() ?: 24f
    val vpHeight = viewportHeightRegex.find(xmlContent)?.groupValues?.get(1)?.toFloatOrNull() ?: 24f
    val isEvenOdd = fillTypeRegex.find(xmlContent)?.groupValues?.get(1)?.equals("evenOdd", ignoreCase = true) == true

    return try {
      val builder = ImageVector.Builder(
        name = iconName,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = vpWidth,
        viewportHeight = vpHeight,
      )
      for (match in matches) {
        val pathData = match.groupValues[1]
        val pathNodes = PathParser().parsePathString(pathData).toNodes()
        builder.addPath(
          pathData = pathNodes,
          pathFillType = if (isEvenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
          fill = SolidColor(Color.White),
        )
      }
      builder.build()
    } catch (e: Exception) {
      Log.e("ProductIconManager", "Error parsing vector XML for $iconName", e)
      null
    }
  }

  private fun normalize(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
  }

  private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { kw ->
      val cleanKw = normalize(kw.lowercase().trim())
      if (cleanKw.length <= 3) {
        Regex("""\b${Regex.escape(cleanKw)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(this)
      } else {
        this.contains(cleanKw)
      }
    }
  }
}

/**
 * Componente visual Composable para mostrar el icono de un producto en cualquier pantalla.
 * Carga instantáneamente el vector disponible y reacciona de forma automática y fluida
 * en tiempo real cuando el icono de alta resolución termina de descargarse desde la CDN.
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

  // Colección reactiva de iconos descargados
  val downloadedIconsMap by ProductIconManager.downloadedIcons.collectAsStateWithLifecycle()

  // Si ya está en el mapa reactivo, úsalo de inmediato; si no, busca en disco, assets o fallback
  val currentVector = downloadedIconsMap[iconName]
    ?: ProductIconManager.getCachedOrLocalVector(context, iconName)

  // Solicitar descarga/actualización en segundo plano si aún no está guardado
  LaunchedEffect(iconName) {
    if (!ProductIconManager.hasDownloadedIcon(context, iconName)) {
      ProductIconManager.getOrFetchIcon(context, iconName)
    }
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(10.dp))
      .background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = currentVector,
      contentDescription = productName,
      tint = contentColor,
      modifier = Modifier.size(iconSize),
    )
  }
}
