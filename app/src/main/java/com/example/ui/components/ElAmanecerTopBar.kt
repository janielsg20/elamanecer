package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * TopBar oficial y unificada para la aplicación "El Amanecer".
 * Muestra el logotipo moderno y minimalista de la app, el nombre comercial "El Amanecer",
 * el subtítulo de la sección activa y los botones de acción contextuales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElAmanecerTopBar(
  subtitle: String? = null,
  navigationIcon: @Composable (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  onLogoClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
  ),
) {
  val appName = stringResource(id = R.string.app_name)

  TopAppBar(
    modifier = modifier.testTag("el_amanecer_top_bar"),
    navigationIcon = {
      if (navigationIcon != null) {
        navigationIcon()
      }
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(vertical = 4.dp),
      ) {
        // Logotipo moderno y estilizado de El Amanecer
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(
              width = 1.5.dp,
              brush = Brush.linearGradient(
                colors = listOf(
                  Color(0xFFF59E0B), // Dorado / Amanecer
                  Color(0xFFF97316), // Ámbar cálido
                )
              ),
              shape = CircleShape
            )
            .background(Color(0xFF0F172A))
            .clickable(enabled = onLogoClick != null) { onLogoClick?.invoke() }
            .testTag("el_amanecer_app_logo"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.logo_el_amanecer_round),
            contentDescription = "Logotipo de El Amanecer",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(38.dp),
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
          verticalArrangement = Arrangement.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = appName,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (!subtitle.isNullOrBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
              ),
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    },
    actions = {
      actions()
    },
    colors = colors,
  )
}
