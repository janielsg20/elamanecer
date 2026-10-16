package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import com.example.domain.icon.ProductIconView
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyInventoryItem
import com.example.domain.model.Product
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
  initialDateMillis: Long? = null,
  title: String = "Seleccionar Fecha",
  onDateSelected: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
  )
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = {
          datePickerState.selectedDateMillis?.let { onDateSelected(it) }
          onDismiss()
        },
        modifier = Modifier.testTag("date_picker_confirm_btn")
      ) {
        Text("Aceptar")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("date_picker_cancel_btn")
      ) {
        Text("Cancelar")
      }
    }
  ) {
    DatePicker(
      state = datePickerState,
      title = {
        Text(
          text = title,
          modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    )
  }
}

@Composable
fun ThemeToggleIconButton(
  modifier: Modifier = Modifier,
) {
  val currentMode by ThemeManager.themeMode.collectAsState()
  val isDark = currentMode == AppThemeMode.DARK

  IconButton(
    onClick = { ThemeManager.toggleTheme() },
    modifier = modifier.testTag("btn_theme_toggle"),
  ) {
    Icon(
      imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
      contentDescription = if (isDark) "Cambiar a Tema Claro" else "Cambiar a Tema Oscuro",
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(22.dp),
    )
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  highlightColor: Color = MaterialTheme.colorScheme.primary,
  testTag: String = "metric_card",
) {
  ElevatedCard(
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
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
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (icon != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(highlightColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = highlightColor,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          letterSpacing = (-0.3).sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = highlightColor,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun StockBadge(
  currentStock: Double,
  minStock: Double,
  modifier: Modifier = Modifier,
) {
  val (badgeText, badgeColor, containerColor, badgeIcon) = when {
    currentStock <= 0.0 -> Quadruple("Agotado", DangerRed, DangerRed.copy(alpha = 0.12f), Icons.Default.RemoveCircleOutline)
    minStock > 0 && currentStock <= minStock -> Quadruple("Stock bajo", WarningOrange, WarningOrange.copy(alpha = 0.15f), Icons.Default.Warning)
    else -> Quadruple("Disponible", SuccessGreen, SuccessGreen.copy(alpha = 0.12f), Icons.Default.CheckCircle)
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = containerColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(11.dp))
      Text(
        text = badgeText,
        color = badgeColor,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ExpandableInventoryCard(
  item: DailyInventoryItem,
  onAddEntriesClick: () -> Unit,
  onAdjustClosingClick: () -> Unit,
  onEditSaleClick: () -> Unit,
  isSessionClosed: Boolean,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  OutlinedCard(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { expanded = !expanded }
      .testTag("item_card_${item.productId}"),
    shape = RoundedCornerShape(12.dp),
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
            productName = item.productName,
            categoryName = item.categoryName,
            size = 40.dp,
            iconSize = 22.dp,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.productName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${item.categoryName} • ${item.unitOfMeasure}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (expanded) "Contraer" else "Expandir",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Resumen rápido: Vendido, Ventas, Utilidad (distribuidos en columnas iguales con elipsis)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Vendido",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = "${item.soldQuantity} ${item.unitOfMeasure}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Venta",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = Product.formatMoney(item.saleAmountCents),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Utilidad",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = Product.formatMoney(item.profitCents),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = SuccessGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                "FÓRMULAS Y AUDITORÍA DE INVENTARIO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(2.dp))
              AuditRow("Inventario Inicial (C):", "${item.openingStock}")
              AuditRow("Entradas del día (D):", "+${item.entries}", highlightColor = MaterialTheme.colorScheme.primary)
              AuditRow("DISPONIBLE [C+D] (E):", "${item.availableStock}", isBold = true)
              AuditRow("Inventario Final (L):", "${item.closingStock}")
              AuditRow("Roturas / Mermas (M):", "${item.breakage}", highlightColor = DangerRed)
              AuditRow("Precio Venta / Costo:", "${Product.formatMoney(item.salePriceCents)} / ${Product.formatMoney(item.costPriceCents)}")
            }
          }

          if (!isSessionClosed) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = onAddEntriesClick,
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_entry_${item.productId}"),
                shape = RoundedCornerShape(8.dp),
              ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Entrada", style = MaterialTheme.typography.labelMedium, maxLines = 1)
              }
              Button(
                onClick = onAdjustClosingClick,
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_count_${item.productId}"),
                shape = RoundedCornerShape(8.dp),
              ) {
                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Conteo Final", style = MaterialTheme.typography.labelMedium, maxLines = 1)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
              onClick = onEditSaleClick,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_edit_sale_${item.productId}"),
              shape = RoundedCornerShape(8.dp),
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Editar o Anular Venta", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuditRow(
  label: String,
  value: String,
  isBold: Boolean = false,
  highlightColor: Color = Color.Unspecified,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f, fill = false),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
      ),
      color = if (highlightColor != Color.Unspecified) highlightColor else MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
      maxLines = 1,
    )
  }
}
