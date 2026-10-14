package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.ui.performance.LocalLauncherPerformanceTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class AppMenuAnchor(
  val app: AppInfo,
  val iconBoundsInRoot: Rect = Rect.Zero,
  val itemBoundsInRoot: Rect = Rect.Zero,
  val showLabel: Boolean = false,
  val textColor: Color? = null
)

private class ItemCoordsHolder(
  var itemCoords: LayoutCoordinates? = null,
  var iconCoords: LayoutCoordinates? = null
)

private fun unscaleRectFromCenter(rect: Rect, scale: Float): Rect {
  if (scale <= 0f || scale == 1f || rect == Rect.Zero) return rect
  val halfW = (rect.width / scale) * 0.5f
  val halfH = (rect.height / scale) * 0.5f
  val cx = rect.center.x
  val cy = rect.center.y
  return Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
}

private val ItemClipShape = RoundedCornerShape(19.dp)
private val PressSpringSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val ReleaseSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private val DarkShadowStyle = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.48f),
        offset = Offset(0f, 1.2f),
        blurRadius = 4.0f
    )
)

private val LightShadowStyle = TextStyle(
    shadow = Shadow(
        color = Color.White.copy(alpha = 0.65f),
        offset = Offset(0f, 0.8f),
        blurRadius = 3.0f
    )
)

@Composable
fun AppGridItem(
  app: AppInfo,
  showLabel: Boolean,
  viewModel: LauncherViewModel,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  iconSize: Dp = 58.dp,
  textColor: Color? = null,
  iconShape: String = "sammy",
  selectedIconPack: String? = null,
  isMenuHighlighted: Boolean = false,
  isSelected: Boolean = false,
  selectionIndex: Int = -1,
  enableLongPress: Boolean = true,
  isInteractive: Boolean = true,
  onLongClickWithAnchor: ((AppMenuAnchor) -> Unit)? = null
) {
  val currentIconSize = iconSize
  val verticalPadding = 0.dp
  val isDark = isSystemInDarkTheme()
  val selectionAccent = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
  val performanceTracker = LocalLauncherPerformanceTracker.current

  // Instant 0ms memory lookup: renders immediately if icon is in RAM
  var iconBitmap by remember(app.packageName, app.activityName, iconShape, selectedIconPack, app.lastUpdateTime) { 
      mutableStateOf(viewModel.getAppIconFast(app.packageName, app.activityName))
  }

  // Only launch a coroutine if the icon is not already in L1 RAM cache
  if (iconBitmap == null) {
      LaunchedEffect(app.packageName, app.activityName, iconShape, selectedIconPack, app.lastUpdateTime) {
          if (performanceTracker.isScrollOrGestureActive() || performanceTracker.isHeavyActivityLoading()) {
              kotlinx.coroutines.yield()
          }
          iconBitmap = viewModel.loadAppIconAsync(app.packageName, app.activityName)
      }
  }

  // Fast passive path for AppDrawer items: zero pointerInput coroutines, zero onGloballyPositioned, zero Animatable
  if (!isInteractive) {
    val passiveModifier = if (isMenuHighlighted) {
      Modifier
        .fillMaxWidth()
        .graphicsLayer { alpha = 0f }
        .padding(vertical = verticalPadding, horizontal = 1.dp)
        .testTag("app_item_${app.packageName}")
    } else if (isSelected) {
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(selectionAccent.copy(alpha = if (isDark) 0.16f else 0.12f), RoundedCornerShape(16.dp))
        .border(1.2.dp, selectionAccent.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
        .padding(vertical = 3.dp, horizontal = 1.dp)
        .testTag("app_item_${app.packageName}")
    } else {
      Modifier
        .fillMaxWidth()
        .padding(vertical = verticalPadding, horizontal = 1.dp)
        .testTag("app_item_${app.packageName}")
    }

    Column(
      modifier = passiveModifier,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(contentAlignment = Alignment.TopEnd) {
        if (iconBitmap != null) {
          Image(
            bitmap = iconBitmap!!,
            contentDescription = "Ícone de ${app.label}",
            modifier = Modifier.size(currentIconSize),
            contentScale = ContentScale.Fit
          )
        } else {
          Box(modifier = Modifier.size(currentIconSize))
        }

        if (isSelected) {
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(CircleShape)
              .background(selectionAccent, CircleShape)
              .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (selectionIndex >= 0) "${selectionIndex + 1}" else "✓",
              color = Color.White,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 11.sp
            )
          }
        }
      }

      if (showLabel) {
        Spacer(modifier = Modifier.height(3.dp))

        val actualTextColor = textColor ?: if (isDark) Color(0xFFF5F5F7) else Color(0xFF141416)
        val textStyle = if (isDark) DarkShadowStyle else LightShadowStyle

        Text(
          text = app.label,
          color = actualTextColor,
          fontSize = 12.sp,
          lineHeight = 14.5.sp,
          letterSpacing = (-0.15).sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          style = textStyle,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp)
        )
      }
    }
    return
  }

  val scaleAnim = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  // Non-reactive reference holder for interactive Home Dock items
  val coordsHolder = remember { ItemCoordsHolder() }

  val computeAnchorNow: () -> AppMenuAnchor = remember(app, showLabel, textColor) {
    {
      val rawItem = try {
        coordsHolder.itemCoords?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero
      } catch (_: Throwable) {
        Rect.Zero
      }
      val rawIcon = try {
        coordsHolder.iconCoords?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero
      } catch (_: Throwable) {
        Rect.Zero
      }
      val unscaledIcon = unscaleRectFromCenter(rawIcon, scaleAnim.value)
      AppMenuAnchor(
        app = app,
        iconBoundsInRoot = if (unscaledIcon != Rect.Zero) unscaledIcon else rawItem,
        itemBoundsInRoot = if (rawItem != Rect.Zero) rawItem else unscaledIcon,
        showLabel = showLabel,
        textColor = textColor
      )
    }
  }

  val latestOnClick = rememberUpdatedState(onClick)
  val latestOnLongClick = rememberUpdatedState(onLongClick)
  val latestOnLongClickWithAnchor = rememberUpdatedState(onLongClickWithAnchor)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        coordsHolder.itemCoords = coords
      }
      .graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        alpha = if (isMenuHighlighted) 0f else 1f
      }
      .clip(ItemClipShape)
      .then(
        if (isSelected) {
          Modifier
            .background(selectionAccent.copy(alpha = if (isDark) 0.16f else 0.12f), ItemClipShape)
            .border(1.2.dp, selectionAccent.copy(alpha = 0.85f), ItemClipShape)
        } else {
          Modifier
        }
      )
      .pointerInput(app.packageName, app.activityName, showLabel, textColor, enableLongPress) {
        val stationarySlopPx = 6.dp.toPx()
        val tapSlopPx = 14.dp.toPx()
        val stationaryHoldTimeoutMs = 400L

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          down.consume()
          val pointerId = down.id
          val downPos = down.position
          var maxDist = 0f
          var isMenuTriggered = false
          var isCancelledByMove = false

          scope.launch {
            scaleAnim.animateTo(0.92f, PressSpringSpec)
          }

          var holdJob: Job? = null
          if (enableLongPress) {
            holdJob = scope.launch {
              delay(stationaryHoldTimeoutMs)
              if (!isCancelledByMove && !isMenuTriggered && maxDist < stationarySlopPx) {
                isMenuTriggered = true
                scaleAnim.animateTo(1f, ReleaseSpringSpec)
                latestOnLongClickWithAnchor.value?.invoke(computeAnchorNow())
                latestOnLongClick.value.invoke()
              }
            }
          }

          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
              change.consume()
              break
            }
            val dist = (change.position - downPos).getDistance()
            if (dist > maxDist) {
              maxDist = dist
            }
            if (!isMenuTriggered && maxDist >= stationarySlopPx) {
              holdJob?.cancel()
              if (maxDist >= tapSlopPx && !isCancelledByMove) {
                isCancelledByMove = true
                scope.launch {
                  scaleAnim.animateTo(1f, ReleaseSpringSpec)
                }
                break
              }
            }
            if (isMenuTriggered) {
              change.consume()
            }
          }

          holdJob?.cancel()
          scope.launch {
            scaleAnim.animateTo(1f, ReleaseSpringSpec)
          }

          if (!isMenuTriggered && !isCancelledByMove && maxDist < tapSlopPx) {
            latestOnClick.value.invoke()
          }
        }
      }
      .padding(vertical = verticalPadding, horizontal = 1.dp)
      .testTag("app_item_${app.packageName}"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      if (iconBitmap != null) {
        Image(
          bitmap = iconBitmap!!,
          contentDescription = "Ícone de ${app.label}",
          modifier = Modifier
            .size(currentIconSize)
            .onGloballyPositioned { coords ->
              coordsHolder.iconCoords = coords
            },
          contentScale = ContentScale.Fit
        )
      } else {
        Box(
          modifier = Modifier
            .size(currentIconSize)
            .onGloballyPositioned { coords ->
              coordsHolder.iconCoords = coords
            }
        )
      }

      if (isSelected) {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(selectionAccent, CircleShape)
            .border(1.5.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (selectionIndex >= 0) "${selectionIndex + 1}" else "✓",
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 11.sp
          )
        }
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(3.dp))

      val actualTextColor = textColor ?: if (isDark) Color(0xFFF5F5F7) else Color(0xFF141416)
      val textStyle = if (isDark) DarkShadowStyle else LightShadowStyle

      Text(
        text = app.label,
        color = actualTextColor,
        fontSize = 12.sp,
        lineHeight = 14.5.sp,
        letterSpacing = (-0.15).sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = textStyle,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 3.dp)
      )
    }
  }
}

