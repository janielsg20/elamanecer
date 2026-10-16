package com.example

import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.DailySession
import com.example.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SalesCalculatorTest {

  @Test
  fun testFormulasFromUserSpecification() {
    // opening = 100, entries = 20 -> available = 120
    val available = SalesCalculator.calculateAvailableStock(100.0, 20.0)
    assertEquals(120.0, available, 0.001)

    // closing = 70, breakage = 2 -> sold = 48
    val sold = SalesCalculator.calculateSoldQuantity(available, 70.0, 2.0)
    assertEquals(48.0, sold, 0.001)

    // salePrice = $390 (39000 cents) -> sales = $18,720 (1872000 cents)
    val salePriceCents = 390_00L
    val sales = SalesCalculator.calculateSaleAmountCents(sold, salePriceCents)
    assertEquals(18_720_00L, sales)

    // cost = $330 (33000 cents) -> costAmount = $15,840 (1584000 cents)
    val costPriceCents = 330_00L
    val costAmount = SalesCalculator.calculateCostAmountCents(sold, costPriceCents)
    assertEquals(15_840_00L, costAmount)

    // profit = $2,880 (288000 cents)
    val profit = SalesCalculator.calculateProfitCents(sales, costAmount)
    assertEquals(2_880_00L, profit)

    // Margin = (2880 / 18720) * 100 = 15.38%
    val margin = SalesCalculator.calculateMarginPercentage(sales, profit)
    assertEquals(15.3846, margin, 0.01)
  }

  @Test
  fun testZeroEntries() {
    val available = SalesCalculator.calculateAvailableStock(50.0, 0.0)
    assertEquals(50.0, available, 0.001)

    val sold = SalesCalculator.calculateSoldQuantity(available, 40.0, 0.0)
    assertEquals(10.0, sold, 0.001)
  }

  @Test
  fun testZeroSales() {
    // If closing stock equals available, sold is 0
    val available = SalesCalculator.calculateAvailableStock(30.0, 10.0)
    val sold = SalesCalculator.calculateSoldQuantity(available, 40.0, 0.0)
    assertEquals(0.0, sold, 0.001)

    val sales = SalesCalculator.calculateSaleAmountCents(sold, 500_00L)
    assertEquals(0L, sales)
  }

  @Test
  fun testProductDepleted_ClosingStockZero() {
    val available = SalesCalculator.calculateAvailableStock(20.0, 5.0) // 25
    val sold = SalesCalculator.calculateSoldQuantity(available, 0.0, 0.0)
    assertEquals(25.0, sold, 0.001)
  }

  @Test
  fun testBreakageReducesSoldQuantity() {
    // If available is 20, closing is 15, breakage is 3 -> sold is 2
    val sold = SalesCalculator.calculateSoldQuantity(20.0, 15.0, 3.0)
    assertEquals(2.0, sold, 0.001)
  }

  @Test
  fun testInvalidClosingStockExceedingAvailableThrowsException() {
    assertThrows(IllegalArgumentException::class.java) {
      SalesCalculator.calculateSoldQuantity(availableStock = 20.0, closingStock = 25.0, breakage = 0.0)
    }
  }

  @Test
  fun testCostHigherThanSalePrice_NegativeProfit() {
    val sold = 10.0
    val salePrice = 300_00L
    val costPrice = 350_00L

    val sales = SalesCalculator.calculateSaleAmountCents(sold, salePrice)
    val cost = SalesCalculator.calculateCostAmountCents(sold, costPrice)
    val profit = SalesCalculator.calculateProfitCents(sales, cost)

    assertEquals(-500_00L, profit)
  }

  @Test
  fun testDaySummaryAndCategoriesAggregation() {
    val session = DailySession(
      id = 1L,
      dateString = "2026-06-19",
      dateEpochDay = 20624L,
      status = SessionStatus.OPEN,
    )

    val item1 = SalesCalculator.enrichInventoryItem(
      sessionId = 1L,
      productId = 101L,
      productName = "Cerveza Cristal",
      categoryId = 1L,
      categoryName = "Bebidas",
      unitOfMeasure = "Botella",
      openingStock = 100.0,
      entries = 20.0,
      closingStock = 70.0,
      breakage = 2.0,
      salePriceCents = 390_00L,
      costPriceCents = 330_00L,
    )

    val item2 = SalesCalculator.enrichInventoryItem(
      sessionId = 1L,
      productId = 201L,
      productName = "Galletas",
      categoryId = 2L,
      categoryName = "Alimentos",
      unitOfMeasure = "Paquete",
      openingStock = 50.0,
      entries = 0.0,
      closingStock = 40.0,
      breakage = 0.0,
      salePriceCents = 150_00L,
      costPriceCents = 100_00L,
    )

    val summary = SalesCalculator.computeDaySummary(session, listOf(item1, item2))

    assertEquals(2, summary.totalProductsCount)
    // item1 sales: 48 * 390 = 18,720.00
    // item2 sales: 10 * 150 = 1,500.00
    // Total: 20,220.00 -> 2022000 cents
    assertEquals(20_220_00L, summary.totalSalesCents)
    // item1 cost: 48 * 330 = 15,840.00
    // item2 cost: 10 * 100 = 1,000.00
    // Total: 16,840.00 -> 1684000 cents
    assertEquals(16_840_00L, summary.totalCostCents)
    // Total profit: 20220 - 16840 = 3,380.00 -> 338000 cents
    assertEquals(3_380_00L, summary.totalProfitCents)

    assertEquals(2, summary.categorySummaries.size)
  }

  @Test
  fun testDateHelperMethods() {
    val today = com.example.domain.model.DateHelper.getTodayDateString()
    org.junit.Assert.assertNotNull(today)
    org.junit.Assert.assertTrue(today.matches(Regex("\\d{2}-\\d{2}-\\d{4}")))

    val parsedDay = com.example.domain.model.DateHelper.parseDateStringToEpochDay(today)
    org.junit.Assert.assertTrue(parsedDay > 0)

    val fixedMillis = 1782259200000L // 24-06-2026 UTC
    val formatted = com.example.domain.model.DateHelper.formatEpochMillisToDateString(fixedMillis)
    assertEquals("24-06-2026", formatted)

    val epochDay = com.example.domain.model.DateHelper.epochMillisToEpochDay(fixedMillis)
    val parsedBack = com.example.domain.model.DateHelper.parseDateStringToEpochDay("24-06-2026")
    assertEquals(epochDay, parsedBack)
  }
}
