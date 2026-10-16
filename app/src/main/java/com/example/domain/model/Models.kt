package com.example.domain.model

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
