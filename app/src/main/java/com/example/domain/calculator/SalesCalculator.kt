package com.example.domain.calculator

import com.example.domain.model.CategorySummary
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.DailySession
import com.example.domain.model.DaySummary
import kotlin.math.roundToLong

object SalesCalculator {

  /**
   * Fórmula 1: DISPONIBLE = INICIO + ENTRADAS
   * Excel: E = C + D
   */
  fun calculateAvailableStock(openingStock: Double, entries: Double): Double {
    require(openingStock >= 0.0) { "El inventario inicial no puede ser negativo" }
    require(entries >= 0.0) { "Las entradas no pueden ser negativas" }
    return openingStock + entries
  }

  /**
   * Fórmula 2: VENDIDO = DISPONIBLE - FINAL - ROTURAS
   * Excel: F = E - L - M
   */
  fun calculateSoldQuantity(
    availableStock: Double,
    closingStock: Double,
    breakage: Double,
  ): Double {
    require(availableStock >= 0.0) { "El disponible no puede ser negativo" }
    require(closingStock >= 0.0) { "El inventario final no puede ser negativo" }
    require(breakage >= 0.0) { "Las roturas no pueden ser negativas" }
    if (closingStock + breakage > availableStock + 0.0001) {
      throw IllegalArgumentException(
        "El inventario final ($closingStock) más roturas ($breakage) supera el inventario disponible ($availableStock)"
      )
    }
    val sold = availableStock - closingStock - breakage
    return if (sold < 0.0) 0.0 else sold
  }

  /**
   * Fórmula 3: IMPORTE VENTA = VENDIDO * PRECIO VENTA
   * Excel: H = F * G
   */
  fun calculateSaleAmountCents(soldQuantity: Double, salePriceCents: Long): Long {
    require(soldQuantity >= 0.0) { "La cantidad vendida no puede ser negativa" }
    require(salePriceCents >= 0L) { "El precio de venta no puede ser negativo" }
    return (soldQuantity * salePriceCents).roundToLong()
  }

  /**
   * Fórmula 4: IMPORTE COSTO = VENDIDO * COSTO UNITARIO
   * Excel: J = F * I
   */
  fun calculateCostAmountCents(soldQuantity: Double, unitCostCents: Long): Long {
    require(soldQuantity >= 0.0) { "La cantidad vendida no puede ser negativa" }
    require(unitCostCents >= 0L) { "El costo unitario no puede ser negativo" }
    return (soldQuantity * unitCostCents).roundToLong()
  }

  /**
   * Fórmula 5: UTILIDAD = IMPORTE VENTA - IMPORTE COSTO
   * Excel: K = H - J
   */
  fun calculateProfitCents(saleAmountCents: Long, costAmountCents: Long): Long {
    return saleAmountCents - costAmountCents
  }

  /**
   * Margen Porcentual: (Utilidad / Ventas) * 100
   */
  fun calculateMarginPercentage(totalSalesCents: Long, totalProfitCents: Long): Double {
    if (totalSalesCents <= 0L) return 0.0
    return (totalProfitCents.toDouble() / totalSalesCents.toDouble()) * 100.0
  }

  /**
   * Completa el cálculo de una fila diaria de producto.
   */
  fun enrichInventoryItem(
    id: Long = 0,
    sessionId: Long,
    productId: Long,
    productName: String,
    categoryId: Long,
    categoryName: String,
    unitOfMeasure: String,
    openingStock: Double,
    entries: Double,
    closingStock: Double,
    breakage: Double,
    salePriceCents: Long,
    costPriceCents: Long,
  ): DailyInventoryItem {
    val available = calculateAvailableStock(openingStock, entries)
    val sold = calculateSoldQuantity(available, closingStock, breakage)
    val saleAmount = calculateSaleAmountCents(sold, salePriceCents)
    val costAmount = calculateCostAmountCents(sold, costPriceCents)
    val profit = calculateProfitCents(saleAmount, costAmount)

    return DailyInventoryItem(
      id = id,
      sessionId = sessionId,
      productId = productId,
      productName = productName,
      categoryId = categoryId,
      categoryName = categoryName,
      unitOfMeasure = unitOfMeasure,
      openingStock = openingStock,
      entries = entries,
      closingStock = closingStock,
      breakage = breakage,
      salePriceCents = salePriceCents,
      costPriceCents = costPriceCents,
      availableStock = available,
      soldQuantity = sold,
      saleAmountCents = saleAmount,
      costAmountCents = costAmount,
      profitCents = profit,
    )
  }

  /**
   * Calcula subtotales por categoría y gran total diario.
   * Excel: Subtotales y Gran Total
   */
  fun computeDaySummary(
    session: DailySession,
    items: List<DailyInventoryItem>,
  ): DaySummary {
    val categoryGroups = items.groupBy { it.categoryId }
    val categorySummaries = categoryGroups.map { (catId, catItems) ->
      val catSales = catItems.sumOf { it.saleAmountCents }
      val catCost = catItems.sumOf { it.costAmountCents }
      val catProfit = catSales - catCost
      val catUnits = catItems.sumOf { it.soldQuantity }
      val catBreakage = catItems.sumOf { it.breakage }
      val catMargin = calculateMarginPercentage(catSales, catProfit)
      val catName = catItems.firstOrNull()?.categoryName ?: "Categoría $catId"

      CategorySummary(
        categoryId = catId,
        categoryName = catName,
        totalSalesCents = catSales,
        totalCostCents = catCost,
        totalProfitCents = catProfit,
        marginPercentage = catMargin,
        unitsSold = catUnits,
        unitsBreakage = catBreakage,
        itemsCount = catItems.size,
      )
    }

    val totalSales = items.sumOf { it.saleAmountCents }
    val totalCost = items.sumOf { it.costAmountCents }
    val totalProfit = totalSales - totalCost
    val totalUnits = items.sumOf { it.soldQuantity }
    val totalBreakage = items.sumOf { it.breakage }
    val totalMargin = calculateMarginPercentage(totalSales, totalProfit)

    return DaySummary(
      sessionId = session.id,
      dateString = session.dateString,
      status = session.status,
      totalSalesCents = totalSales,
      totalCostCents = totalCost,
      totalProfitCents = totalProfit,
      marginPercentage = totalMargin,
      totalUnitsSold = totalUnits,
      totalBreakage = totalBreakage,
      totalProductsCount = items.size,
      categorySummaries = categorySummaries,
    )
  }
}
