package com.example.presentation.quicksale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.Product
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.DangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(
  viewModel: QuickSaleViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var manualQuantityItem by remember { mutableStateOf<DailyInventoryItem?>(null) }

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
          Column {
            Text(
              text = "Punto de Venta Rápida",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = uiState.activeSession?.let { "Jornada: ${it.dateString}" } ?: "Iniciando jornada...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          ThemeToggleIconButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    bottomBar = {
      if (uiState.cart.isNotEmpty()) {
        Surface(
          shadowElevation = 8.dp,
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${uiState.totalCartUnits} un. seleccionadas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
              Text(
                text = Product.formatMoney(uiState.totalCartCents),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                ),
                maxLines = 1,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(
                onClick = { viewModel.clearCart() },
                modifier = Modifier.testTag("btn_clear_cart"),
              ) {
                Text("Limpiar", style = MaterialTheme.typography.labelMedium)
              }
              Button(
                onClick = { viewModel.commitSale() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_commit_sale"),
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cobrar", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
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
        placeholder = { Text("Buscar producto para venta...") },
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
          .testTag("quicksale_search_input"),
      )

      // 2. Filtros de categoría
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
          label = { Text("Todos", style = MaterialTheme.typography.labelMedium) },
        )
        uiState.categories.forEach { cat ->
          FilterChip(
            selected = uiState.selectedCategoryId == cat.id,
            onClick = { viewModel.setCategoryFilter(cat.id) },
            label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
          )
        }
      }

      // 3. Grid de Productos para Venta Rápida
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(uiState.filteredItems, key = { it.productId }) { item ->
          val cartItem = uiState.cart[item.productId]
          val selectedQty = cartItem?.quantity ?: 0.0

          OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sale_card_${item.productId}"),
            colors = CardDefaults.outlinedCardColors(
              containerColor = if (selectedQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
              else MaterialTheme.colorScheme.surface,
            ),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.productName,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${item.categoryName} • Stock: ${item.closingStock} ${item.unitOfMeasure}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = Product.formatMoney(item.salePriceCents),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  ),
                  maxLines = 1,
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              // Controles de cantidad (Botones +/- Y entrada manual directa)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (selectedQty > 0) {
                  FilledTonalIconButton(
                    onClick = { viewModel.removeFromCart(item.productId) },
                    modifier = Modifier
                      .size(34.dp)
                      .testTag("btn_decrease_${item.productId}"),
                  ) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                  }

                  // Cantidad con toque para ingresar manual
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                      .clickable { manualQuantityItem = item }
                      .testTag("btn_manual_qty_${item.productId}"),
                  ) {
                    Text(
                      text = "${selectedQty.toInt()}",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      maxLines = 1,
                    )
                  }

                  FilledTonalIconButton(
                    onClick = { viewModel.addToCart(item, 1.0) },
                    enabled = item.closingStock > selectedQty,
                    modifier = Modifier
                      .size(34.dp)
                      .testTag("btn_increase_${item.productId}"),
                  ) {
                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                  }
                } else {
                  Button(
                    onClick = { viewModel.addToCart(item, 1.0) },
                    enabled = item.closingStock > 0,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_add_to_cart_${item.productId}"),
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vender", style = MaterialTheme.typography.labelMedium)
                  }
                }

                // Botón de lápiz / entrada manual de cantidad para acceso directo
                IconButton(
                  onClick = { manualQuantityItem = item },
                  modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_open_manual_qty_${item.productId}"),
                ) {
                  Icon(
                    Icons.Default.Edit,
                    contentDescription = "Ingresar cantidad manual",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // DIÁLOGO: INGRESAR CANTIDAD MANUALMENTE
  manualQuantityItem?.let { item ->
    val currentQty = uiState.cart[item.productId]?.quantity ?: 0.0
    var qtyInput by remember { mutableStateOf(if (currentQty > 0) currentQty.toInt().toString() else "1") }
    val typedQty = qtyInput.toDoubleOrNull() ?: 0.0
    val isExceeded = typedQty > item.closingStock
    val subtotalPreview = (typedQty * item.salePriceCents).toLong()

    AlertDialog(
      onDismissRequest = { manualQuantityItem = null },
      title = {
        Text(
          text = "Cantidad Manual: ${item.productName}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Disponible: ${item.closingStock} ${item.unitOfMeasure} • Precio: ${Product.formatMoney(item.salePriceCents)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = qtyInput,
            onValueChange = { qtyInput = it },
            label = { Text("Cantidad a Vender") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeded,
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_manual_sale_qty"),
          )

          if (isExceeded) {
            Text(
              text = "La cantidad ($typedQty) supera el stock disponible (${item.closingStock})",
              color = DangerRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Importe Venta:", style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = Product.formatMoney(subtotalPreview),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (!isExceeded && typedQty >= 0) {
              viewModel.setCartQuantity(item, typedQty)
              manualQuantityItem = null
            }
          },
          enabled = !isExceeded && qtyInput.isNotBlank(),
          modifier = Modifier.testTag("btn_confirm_manual_qty"),
        ) {
          Text("Establecer")
        }
      },
      dismissButton = {
        TextButton(onClick = { manualQuantityItem = null }) {
          Text("Cancelar")
        }
      },
    )
  }
}
