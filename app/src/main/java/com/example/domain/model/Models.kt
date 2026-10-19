package com.example.domain.model

data class Project(
  val id: Long = 0,
  val name: String,
  val type: String = "KIOSKO", // KIOSKO, BAR, PALADAR, OTRO
  val description: String = "",
  val colorHex: String = "#1E88E5",
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
)

data class Warehouse(
  val id: Long = 0,
  val name: String,
  val targetType: String = "GENERAL", // KIOSKOS, BAR, PALADAR, GENERAL
  val location: String = "",
  val notes: String = "",
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
)

data class WarehouseStockItem(
  val id: Long = 0,
  val warehouseId: Long,
  val productId: Long,
  val productName: String = "",        // Producto = campo de texto
  val unitOfMeasure: String = "U",     // Unidad = campo de texto
  val categoryName: String = "",
  val initialStock: Double = 0.0,      // Inicio = campo de numeros
  val entries: Double = 0.0,           // entrada = campo de numeros
  val exits: Double = 0.0,             // salida = campo de numero
  val availableStock: Double = 0.0,    // disponible = inicio + entrada - salida
  val costPrice: Double = 0.0,         // costo = campo de numero
  val costPriceCents: Long = 0L,
  val salePriceCents: Long = 0L,
  val totalAmount: Double = 0.0,       // importe = disponibilidad * costo
  val totalAmountCents: Long = 0L,
  val currentStock: Double = 0.0,      // alias de compatibilidad con availableStock
  val minAlertStock: Double = 10.0,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val costFormatted: String
    get() = formatMoneyDouble(if (costPrice > 0.0) costPrice else costPriceCents / 100.0)

  val totalAmountFormatted: String
    get() = formatMoneyDouble(if (totalAmount > 0.0) totalAmount else (availableStock * (if (costPrice > 0.0) costPrice else costPriceCents / 100.0)))

  companion object {
    fun formatMoneyDouble(amount: Double): String {
      val isNegative = amount < 0
      val absAmount = kotlin.math.abs(amount)
      val formatted = "$${String.format(java.util.Locale.US, "%,.2f", absAmount)}"
      return if (isNegative) "-$formatted" else formatted
    }

    fun formatMoney(cents: Long): String {
      val isNegative = cents < 0
      val absCents = kotlin.math.abs(cents)
      val dollars = absCents / 100
      val remainder = absCents % 100
      val formatted = "$${String.format(java.util.Locale.US, "%,d.%02d", dollars, remainder)}"
      return if (isNegative) "-$formatted" else formatted
    }
  }
}

data class WarehouseDispatch(
  val id: Long = 0,
  val warehouseId: Long,
  val warehouseName: String = "",
  val projectId: Long,
  val projectName: String = "",
  val productId: Long,
  val productName: String = "",
  val unitOfMeasure: String = "U",
  val quantity: Double,
  val remainingStockInWarehouse: Double,
  val dateString: String,
  val timestamp: Long = System.currentTimeMillis(),
  val notes: String = "",
)

data class Category(
  val id: Long = 0,
  val name: String,
  val description: String = "",
  val active: Boolean = true,
)

data class Product(
  val id: Long = 0,
  val categoryId: Long,
  val categoryName: String = "",
  val name: String,
  val unitOfMeasure: String = "U",
  val salePriceCents: Long = 0L,
  val costPriceCents: Long = 0L,
  val minStock: Double = 0.0,
  val maxStock: Double = 0.0,
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val salePriceFormatted: String
    get() = formatMoney(salePriceCents)

  val costPriceFormatted: String
    get() = formatMoney(costPriceCents)

  companion object {
    fun formatMoney(cents: Long): String {
      val isNegative = cents < 0
      val absCents = kotlin.math.abs(cents)
      val dollars = absCents / 100
      val remainder = absCents % 100
      val formatted = "$${String.format("%,d.%02d", dollars, remainder)}"
      return if (isNegative) "-$formatted" else formatted
    }
  }
}

enum class SessionStatus {
  OPEN,
  CLOSED,
}

data class DailySession(
  val id: Long = 0,
  val projectId: Long = 1L,
  val dateString: String,
  val dateEpochDay: Long,
  val status: SessionStatus = SessionStatus.OPEN,
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val closedAt: Long? = null,
)

data class DailyInventoryItem(
  val id: Long = 0,
  val sessionId: Long,
  val productId: Long,
  val productName: String = "",
  val categoryId: Long = 0,
  val categoryName: String = "",
  val unitOfMeasure: String = "U",
  val openingStock: Double = 0.0,
  val entries: Double = 0.0,
  val closingStock: Double = 0.0,
  val breakage: Double = 0.0,
  val salePriceCents: Long = 0L,
  val costPriceCents: Long = 0L,
  val availableStock: Double = 0.0,
  val soldQuantity: Double = 0.0,
  val saleAmountCents: Long = 0L,
  val costAmountCents: Long = 0L,
  val profitCents: Long = 0L,
)

data class CategorySummary(
  val categoryId: Long,
  val categoryName: String,
  val totalSalesCents: Long,
  val totalCostCents: Long,
  val totalProfitCents: Long,
  val marginPercentage: Double,
  val unitsSold: Double,
  val unitsBreakage: Double,
  val itemsCount: Int,
)

data class DaySummary(
  val sessionId: Long,
  val dateString: String,
  val status: SessionStatus,
  val totalSalesCents: Long,
  val totalCostCents: Long,
  val totalProfitCents: Long,
  val marginPercentage: Double,
  val totalUnitsSold: Double,
  val totalBreakage: Double,
  val totalProductsCount: Int,
  val categorySummaries: List<CategorySummary> = emptyList(),
)
