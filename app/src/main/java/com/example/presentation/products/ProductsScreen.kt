package com.example.presentation.products

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Warning
import com.example.domain.icon.ProductIconManager
import com.example.domain.icon.ProductIconView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.calculator.SalesCalculator
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
  viewModel: ProductsViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var productToDelete by remember { mutableStateOf<Product?>(null) }
  var isCreatingProduct by remember { mutableStateOf(false) }
  var isManagingCategories by remember { mutableStateOf(false) }
  var categoryToDelete by remember { mutableStateOf<Category?>(null) }

  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Catálogo de Productos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        actions = {
          ThemeToggleIconButton()
          IconButton(
            onClick = {
              scope.launch {
                snackbarHostState.showSnackbar("Sincronizando iconos de productos...")
                ProductIconManager.preloadDefaultIcons(context)
                val count = ProductIconManager.getDownloadedIconsCount(context)
                snackbarHostState.showSnackbar("Iconos actualizados: $count/${ProductIconManager.ALL_SUPPORTED_ICONS.size} guardados localmente")
              }
            },
            modifier = Modifier.testTag("btn_sync_icons"),
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = "Descargar Iconos", modifier = Modifier.size(22.dp))
          }
          IconButton(
            onClick = { isManagingCategories = true },
            modifier = Modifier.testTag("btn_manage_categories"),
          ) {
            Icon(Icons.Default.Category, contentDescription = "Gestionar Categorías", modifier = Modifier.size(22.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { isCreatingProduct = true },
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("fab_add_product"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Crear Producto")
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier,
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // 1. Buscador
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Buscar producto...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
            }
          }
        },
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp)
          .testTag("products_search_input"),
      )

      // 2. Filtro de Categoría
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 2.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = uiState.selectedCategoryId == null,
          onClick = { viewModel.setCategoryFilter(null) },
          leadingIcon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp)) },
          label = { Text("Todos (${uiState.products.size})", style = MaterialTheme.typography.labelMedium) },
        )
        uiState.categories.forEach { cat ->
          val count = uiState.products.count { it.categoryId == cat.id }
          FilterChip(
            selected = uiState.selectedCategoryId == cat.id,
            onClick = { viewModel.setCategoryFilter(cat.id) },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = { Text("${cat.name} ($count)", style = MaterialTheme.typography.labelMedium) },
          )
        }
      }

      // 3. Lista de Productos
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(uiState.filteredProducts, key = { it.id }) { product ->
          val profitPerUnitCents = product.salePriceCents - product.costPriceCents
          val margin = SalesCalculator.calculateMarginPercentage(product.salePriceCents, profitPerUnitCents)

          OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("product_card_${product.id}"),
            colors = CardDefaults.outlinedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ProductIconView(
                    productName = product.name,
                    categoryName = product.categoryName,
                    size = 42.dp,
                    iconSize = 24.dp,
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = product.name,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${product.categoryName} • ${product.unitOfMeasure}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { viewModel.toggleProductActive(product) },
                    modifier = Modifier.testTag("btn_toggle_active_${product.id}"),
                  ) {
                    Icon(
                      imageVector = if (product.active) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                      contentDescription = if (product.active) "Activo" else "Inactivo",
                      tint = if (product.active) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(26.dp),
                    )
                  }
                  IconButton(
                    onClick = { editingProduct = product },
                    modifier = Modifier.testTag("btn_edit_product_${product.id}"),
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                  }
                  IconButton(
                    onClick = { productToDelete = product },
                    modifier = Modifier.testTag("btn_delete_product_${product.id}"),
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRed, modifier = Modifier.size(18.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Precio Venta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                  Text(product.salePriceFormatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Costo Unitario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                  Text(product.costPriceFormatted, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Margen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                  Text("${String.format("%.1f", margin)}%", fontWeight = FontWeight.Bold, color = SuccessGreen, maxLines = 1)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Stock Mínimo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                  Text("${product.minStock} ${product.unitOfMeasure}", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }
    }
  }

  // DIÁLOGO CREAR O EDITAR PRODUCTO
  val activeProductToEdit = editingProduct
  if (isCreatingProduct || activeProductToEdit != null) {
    val initialProd = activeProductToEdit
    var nameInput by remember { mutableStateOf(initialProd?.name ?: "") }
    var unitInput by remember { mutableStateOf(initialProd?.unitOfMeasure ?: "Botella") }
    var salePriceInput by remember {
      mutableStateOf(if (initialProd != null) (initialProd.salePriceCents / 100.0).toString() else "")
    }
    var costPriceInput by remember {
      mutableStateOf(if (initialProd != null) (initialProd.costPriceCents / 100.0).toString() else "")
    }
    var minStockInput by remember {
      mutableStateOf(if (initialProd != null) initialProd.minStock.toString() else "10")
    }
    var selectedCatId by remember {
      mutableStateOf(initialProd?.categoryId ?: uiState.categories.firstOrNull()?.id ?: 1L)
    }

    AlertDialog(
      onDismissRequest = {
        isCreatingProduct = false
        editingProduct = null
      },
      icon = {
        val selectedCategory = uiState.categories.find { it.id == selectedCatId }?.name
        ProductIconView(
          productName = nameInput.ifBlank { "Producto" },
          categoryName = selectedCategory,
          size = 46.dp,
          iconSize = 26.dp,
        )
      },
      title = { Text(if (initialProd == null) "Nuevo Producto" else "Editar Producto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Nombre del Producto") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_product_name"),
          )

          Text("Categoría:", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            uiState.categories.forEach { cat ->
              FilterChip(
                selected = selectedCatId == cat.id,
                onClick = { selectedCatId = cat.id },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
              value = unitInput,
              onValueChange = { unitInput = it },
              label = { Text("Unidad (UM)") },
              leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) },
              modifier = Modifier.weight(1f),
              singleLine = true,
            )
            OutlinedTextField(
              value = minStockInput,
              onValueChange = { minStockInput = it },
              label = { Text("Stock Mínimo") },
              leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              singleLine = true,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
              value = salePriceInput,
              onValueChange = { salePriceInput = it },
              label = { Text("Precio Venta ($)") },
              leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier
                .weight(1f)
                .testTag("input_sale_price"),
              singleLine = true,
            )
            OutlinedTextField(
              value = costPriceInput,
              onValueChange = { costPriceInput = it },
              label = { Text("Costo Unitario ($)") },
              leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier
                .weight(1f)
                .testTag("input_cost_price"),
              singleLine = true,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val saleCents = ((salePriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            val costCents = ((costPriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            val minStock = minStockInput.toDoubleOrNull() ?: 0.0

            viewModel.saveProduct(
              id = initialProd?.id ?: 0L,
              categoryId = selectedCatId,
              name = nameInput,
              unitOfMeasure = unitInput,
              salePriceCents = saleCents,
              costPriceCents = costCents,
              minStock = minStock,
              maxStock = 0.0,
            )
            isCreatingProduct = false
            editingProduct = null
          },
          enabled = nameInput.isNotBlank(),
          modifier = Modifier.testTag("btn_save_product"),
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Guardar")
        }
      },
      dismissButton = {
        TextButton(onClick = {
          isCreatingProduct = false
          editingProduct = null
        }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO CONFIRMAR ELIMINACIÓN DE PRODUCTO
  productToDelete?.let { prod ->
    AlertDialog(
      onDismissRequest = { productToDelete = null },
      icon = {
        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
      },
      title = { Text("Eliminar Producto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Text("¿Deseas eliminar '${prod.name}'? El producto será removido del catálogo y de las jornadas asociadas.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteProduct(prod)
            productToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
          modifier = Modifier.testTag("btn_confirm_delete_product"),
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Eliminar")
        }
      },
      dismissButton = {
        TextButton(onClick = { productToDelete = null }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }

  // DIÁLOGO GESTIONAR CATEGORÍAS (LISTAR, CREAR Y ELIMINAR)
  if (isManagingCategories) {
    var newCatName by remember { mutableStateOf("") }
    var newCatDesc by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { isManagingCategories = false },
      icon = {
        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      title = { Text("Gestionar Categorías", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Categorías Existentes:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            items(uiState.categories, key = { it.id }) { cat ->
              val productCount = uiState.products.count { it.categoryId == cat.id }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "$productCount productos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  IconButton(
                    onClick = { categoryToDelete = cat },
                    modifier = Modifier
                      .size(28.dp)
                      .testTag("btn_delete_cat_${cat.id}"),
                  ) {
                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "Eliminar Categoría",
                      tint = DangerRed,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text("Crear Nueva Categoría:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))

          OutlinedTextField(
            value = newCatName,
            onValueChange = { newCatName = it },
            label = { Text("Nombre de Categoría") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_category_name"),
          )

          OutlinedTextField(
            value = newCatDesc,
            onValueChange = { newCatDesc = it },
            label = { Text("Descripción (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newCatName.isNotBlank()) {
              viewModel.createCategory(newCatName, newCatDesc)
              newCatName = ""
              newCatDesc = ""
            }
          },
          enabled = newCatName.isNotBlank(),
          modifier = Modifier.testTag("btn_save_category"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Agregar")
        }
      },
      dismissButton = {
        TextButton(onClick = { isManagingCategories = false }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cerrar")
        }
      },
    )
  }

  // DIÁLOGO CONFIRMAR ELIMINACIÓN DE CATEGORÍA
  categoryToDelete?.let { cat ->
    val count = uiState.products.count { it.categoryId == cat.id }
    AlertDialog(
      onDismissRequest = { categoryToDelete = null },
      icon = {
        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
      },
      title = { Text("Eliminar Categoría", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Text("¿Deseas eliminar la categoría '${cat.name}'? Contiene $count productos asociados que también serán eliminados.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCategory(cat.id, cat.name)
            categoryToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
          modifier = Modifier.testTag("btn_confirm_delete_category"),
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Eliminar Categoría")
        }
      },
      dismissButton = {
        TextButton(onClick = { categoryToDelete = null }) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancelar")
        }
      },
    )
  }
}
