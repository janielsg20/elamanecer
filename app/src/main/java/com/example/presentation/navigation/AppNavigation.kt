package com.example.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.common.AppViewModelProvider
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.history.ReportsScreen
import com.example.presentation.history.ReportsViewModel
import com.example.presentation.inventory.InventoryScreen
import com.example.presentation.inventory.InventoryViewModel
import com.example.presentation.products.ProductsScreen
import com.example.presentation.products.ProductsViewModel
import com.example.presentation.quicksale.QuickSaleScreen
import com.example.presentation.quicksale.QuickSaleViewModel
import com.example.presentation.warehouse.WarehouseScreen
import com.example.presentation.warehouse.WarehouseViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val tag: String) {
  object Dashboard : Screen("dashboard", "Inicio", Icons.Default.Dashboard, "nav_dashboard")
  object Warehouse : Screen("warehouse", "Almacén", Icons.Default.Store, "nav_warehouse")
  object QuickSale : Screen("sales", "Ventas", Icons.Default.ShoppingCart, "nav_sales")
  object Inventory : Screen("inventory", "Inventario", Icons.Default.Inventory, "nav_inventory")
  object Products : Screen("products", "Productos", Icons.Default.Category, "nav_products")
  object Reports : Screen("reports", "Reportes", Icons.Default.BarChart, "nav_reports")
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

  val items = listOf(
    Screen.Dashboard,
    Screen.Warehouse,
    Screen.QuickSale,
    Screen.Inventory,
    Screen.Products,
    Screen.Reports,
  )

  Scaffold(
    bottomBar = {
      NavigationBar {
        items.forEach { screen ->
          val selected = currentRoute == screen.route
          NavigationBarItem(
            icon = { Icon(screen.icon, contentDescription = screen.title) },
            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
            selected = selected,
            modifier = Modifier.testTag(screen.tag),
            onClick = {
              if (currentRoute != screen.route) {
                navController.navigate(screen.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
          )
        }
      }
    },
    modifier = modifier,
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Screen.Dashboard.route,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      composable(Screen.Dashboard.route) {
        val dashboardViewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
        DashboardScreen(
          viewModel = dashboardViewModel,
          onNavigateToSales = {
            navController.navigate(Screen.QuickSale.route) {
              popUpTo(navController.graph.findStartDestination().id) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onNavigateToInventory = {
            navController.navigate(Screen.Inventory.route) {
              popUpTo(navController.graph.findStartDestination().id) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onNavigateToWarehouse = {
            navController.navigate(Screen.Warehouse.route) {
              popUpTo(navController.graph.findStartDestination().id) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
        )
      }

      composable(Screen.Warehouse.route) {
        val warehouseViewModel: WarehouseViewModel = viewModel(factory = AppViewModelProvider.Factory)
        WarehouseScreen(viewModel = warehouseViewModel)
      }

      composable(Screen.QuickSale.route) {
        val quickSaleViewModel: QuickSaleViewModel = viewModel(factory = AppViewModelProvider.Factory)
        QuickSaleScreen(viewModel = quickSaleViewModel)
      }

      composable(Screen.Inventory.route) {
        val inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
        InventoryScreen(viewModel = inventoryViewModel)
      }

      composable(Screen.Products.route) {
        val productsViewModel: ProductsViewModel = viewModel(factory = AppViewModelProvider.Factory)
        ProductsScreen(viewModel = productsViewModel)
      }

      composable(Screen.Reports.route) {
        val reportsViewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
        ReportsScreen(viewModel = reportsViewModel)
      }
    }
  }
}
