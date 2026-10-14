package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DrawerStylePhonesRow(
  currentDrawerStyle: String,
  onDrawerStyleChange: (String) -> Unit,
  isVisible: Boolean = true,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    DrawerPhonePreviewOption(
      isVertical = true,
      isSelected = currentDrawerStyle != "horizontal",
      isVisible = isVisible,
      onClick = { onDrawerStyleChange("vertical") },
      modifier = Modifier
        .weight(1f)
        .testTag("drawer_style_vertical")
    )

    DrawerPhonePreviewOption(
      isVertical = false,
      isSelected = currentDrawerStyle == "horizontal",
      isVisible = isVisible,
      onClick = { onDrawerStyleChange("horizontal") },
      modifier = Modifier
        .weight(1f)
        .testTag("drawer_style_horizontal")
    )
  }
}

@Composable
private fun DrawerPhonePreviewOption(
  isVertical: Boolean,
  isSelected: Boolean,
  isVisible: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = isSystemInDarkTheme()
  val accentColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
  val textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
  val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
  val unselectedCircleBorder = if (isDark) Color(0xFF48484C) else Color(0xFFC7C7CC)
  val scaleAnim = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()

  val optionContainerBg by animateColorAsState(
    targetValue = if (isSelected) {
      accentColor.copy(alpha = if (isDark) 0.08f else 0.06f)
    } else {
      if (isDark) Color(0xFF1D1D21) else Color(0xFFF7F8FA)
    },
    animationSpec = tween(200),
    label = "optionContainerBg"
  )
  val optionContainerBorder by animateColorAsState(
    targetValue = if (isSelected) {
      accentColor.copy(alpha = 0.55f)
    } else {
      if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    },
    animationSpec = tween(200),
    label = "optionContainerBorder"
  )
  val labelColor by animateColorAsState(
    targetValue = if (isSelected) textPrimary else textSecondary,
    animationSpec = tween(200),
    label = "drawerLabelColor"
  )

  Column(
    modifier = modifier
      .graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
      }
      .clip(RoundedCornerShape(14.dp))
      .background(optionContainerBg)
      .border(
        width = if (isSelected) 1.dp else 0.8.dp,
        color = optionContainerBorder,
        shape = RoundedCornerShape(14.dp)
      )
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            scope.launch {
              scaleAnim.animateTo(0.97f, spring(stiffness = Spring.StiffnessMedium))
            }
            tryAwaitRelease()
            scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
          },
          onTap = { onClick() }
        )
      }
      .padding(vertical = 14.dp, horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    AnimatedPhoneDemoScreen(
      isVertical = isVertical,
      isSelected = isSelected,
      isVisible = isVisible,
      isDark = isDark,
      accentColor = accentColor
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = if (isVertical) "Vertical" else "Horizontal",
      color = labelColor,
      fontSize = 13.5.sp,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
      letterSpacing = (-0.1).sp
    )

    Spacer(modifier = Modifier.height(7.dp))

    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(CircleShape)
        .border(
          width = if (isSelected) 1.6.dp else 1.3.dp,
          color = if (isSelected) accentColor else unselectedCircleBorder,
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      if (isSelected) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(accentColor)
        )
      }
    }
  }
}

@Composable
private fun AnimatedPhoneDemoScreen(
  isVertical: Boolean,
  isSelected: Boolean,
  isVisible: Boolean,
  isDark: Boolean,
  accentColor: Color
) {
  val shouldAnimate = isVisible && isSelected
  val verticalProgress: Float
  val horizontalPageProgress: Float

  if (shouldAnimate) {
    val infiniteTransition = rememberInfiniteTransition(
      label = if (isVertical) "verticalDemoVideo" else "horizontalDemoVideo"
    )
    if (isVertical) {
      val vProg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = keyframes {
            durationMillis = 4600
            0f at 0 using FastOutSlowInEasing
            0f at 450 using FastOutSlowInEasing
            1f at 2100 using FastOutSlowInEasing
            1f at 2650 using FastOutSlowInEasing
            0f at 4300 using FastOutSlowInEasing
            0f at 4600
          },
          repeatMode = RepeatMode.Restart
        ),
        label = "verticalScrollOffset"
      )
      verticalProgress = vProg
      horizontalPageProgress = 0f
    } else {
      val hProg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
          animation = keyframes {
            durationMillis = 6400
            0f at 0 using FastOutSlowInEasing
            0f at 450 using FastOutSlowInEasing
            1f at 1350 using FastOutSlowInEasing
            1f at 1850 using FastOutSlowInEasing
            2f at 2750 using FastOutSlowInEasing
            2f at 3250 using FastOutSlowInEasing
            3f at 4150 using FastOutSlowInEasing
            3f at 4750 using FastOutSlowInEasing
            0f at 6100 using FastOutSlowInEasing
            0f at 6400
          },
          repeatMode = RepeatMode.Restart
        ),
        label = "horizontalPageOffset"
      )
      verticalProgress = 0f
      horizontalPageProgress = hProg
    }
  } else {
    verticalProgress = 0f
    horizontalPageProgress = 0f
  }

  val phoneScreenBg = if (isDark) Color(0xFF131316) else Color(0xFFF3F4F7)
  val phoneFrameColor = if (isSelected) {
    accentColor.copy(alpha = 0.75f)
  } else {
    if (isDark) Color(0xFF2C2C32) else Color(0xFFD4D6DE)
  }

  val searchPillColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
  val indicatorColor = if (isSelected) {
    accentColor
  } else {
    if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF7A7D85)
  }

  val iconTileColor = if (isDark) {
    Color.White.copy(alpha = if (isSelected) 0.22f else 0.13f)
  } else {
    Color(0xFF1C1C1E).copy(alpha = if (isSelected) 0.18f else 0.11f)
  }

  Box(
    modifier = Modifier
      .width(104.dp)
      .height(180.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(phoneScreenBg)
      .border(
        width = if (isSelected) 1.2.dp else 0.8.dp,
        color = phoneFrameColor,
        shape = RoundedCornerShape(18.dp)
      )
      .padding(horizontal = 8.dp, vertical = 8.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Top speaker / camera notch pill
      val notchW = w * 0.28f
      val notchH = 2.8.dp.toPx()
      drawRoundRect(
        color = phoneFrameColor.copy(alpha = 0.65f),
        topLeft = Offset((w - notchW) / 2f, 1.5.dp.toPx()),
        size = Size(notchW, notchH),
        cornerRadius = CornerRadius(notchH / 2f, notchH / 2f)
      )

      // Mini search bar pill
      val searchTop = 7.5.dp.toPx()
      val searchH = 9.5.dp.toPx()
      val searchMargin = 2.5.dp.toPx()
      drawRoundRect(
        color = searchPillColor,
        topLeft = Offset(searchMargin, searchTop),
        size = Size(w - searchMargin * 2f, searchH),
        cornerRadius = CornerRadius(searchH / 2f, searchH / 2f)
      )

      // 4x6 Grid viewport geometry (4 columns x 6 visible rows = 24 apps per screen)
      val cols = 4
      val visibleRows = 6
      val gridViewportTop = searchTop + searchH + 6.dp.toPx()
      val gridViewportBottom = h - 11.dp.toPx()
      val viewportH = gridViewportBottom - gridViewportTop

      val usableW = w - 10.dp.toPx()
      val minSpacingX = 4.5.dp.toPx()
      val minSpacingY = 4.5.dp.toPx()
      val maxCellByW = (usableW - minSpacingX * (cols - 1)) / cols
      val maxCellByH = (viewportH - minSpacingY * (visibleRows - 1)) / visibleRows
      val cellSize = minOf(maxCellByW, maxCellByH)

      val totalGridW = cellSize * cols + minSpacingX * (cols - 1)
      val gridLeft = if (isVertical) {
        ((w - 5.dp.toPx()) - totalGridW) / 2f
      } else {
        (w - totalGridW) / 2f
      }
      val spacingX = minSpacingX
      val spacingY = (viewportH - cellSize * visibleRows) / (visibleRows - 1)
      val rowStride = cellSize + spacingY
      val cornerR = CornerRadius(cellSize * 0.30f, cellSize * 0.30f)

      if (isVertical) {
        // Video-like continuous vertical scrolling of 14 rows x 4 columns (56 apps in a 4x6 viewport)
        val totalRows = 14
        val maxScrollPx = rowStride * (totalRows - visibleRows)
        val currentScrollY = verticalProgress * maxScrollPx

        clipRect(
          left = 0f,
          top = gridViewportTop,
          right = w,
          bottom = gridViewportBottom
        ) {
          for (row in 0 until totalRows) {
            val y = gridViewportTop + row * rowStride - currentScrollY
            if (y + cellSize < gridViewportTop - 2f || y > gridViewportBottom + 2f) continue

            for (col in 0 until cols) {
              val x = gridLeft + col * (cellSize + spacingX)
              drawRoundRect(
                color = iconTileColor,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                cornerRadius = cornerR
              )
            }
          }
        }

        // Animated right-side vertical scrollbar
        val trackX = w - 2.dp.toPx()
        val trackTop = gridViewportTop + 1.dp.toPx()
        val trackBottom = gridViewportBottom - 1.dp.toPx()
        val trackH = trackBottom - trackTop
        val thumbH = trackH * (visibleRows.toFloat() / totalRows.toFloat())
        val thumbTop = trackTop + (trackH - thumbH) * verticalProgress

        drawLine(
          color = indicatorColor.copy(alpha = 0.20f),
          start = Offset(trackX, trackTop),
          end = Offset(trackX, trackBottom),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round
        )

        drawLine(
          color = indicatorColor,
          start = Offset(trackX, thumbTop),
          end = Offset(trackX, thumbTop + thumbH),
          strokeWidth = 2.6.dp.toPx(),
          cap = StrokeCap.Round
        )
      } else {
        // Video-like horizontal paged scrolling across 4 pages of 4x6 apps (96 apps total)
        val pageCount = 4
        val rowsPerPage = 6
        val pageStride = w + 6.dp.toPx()

        clipRect(
          left = 0f,
          top = gridViewportTop,
          right = w,
          bottom = gridViewportBottom
        ) {
          for (page in 0 until pageCount) {
            val pageOffsetX = (page - horizontalPageProgress) * pageStride
            if (pageOffsetX + w < -10f || pageOffsetX > w + 10f) continue

            for (row in 0 until rowsPerPage) {
              val y = gridViewportTop + row * rowStride
              for (col in 0 until cols) {
                val x = pageOffsetX + gridLeft + col * (cellSize + spacingX)
                drawRoundRect(
                  color = iconTileColor,
                  topLeft = Offset(x, y),
                  size = Size(cellSize, cellSize),
                  cornerRadius = cornerR
                )
              }
            }
          }
        }

        // Animated bottom 4-page indicator dots synchronized with horizontalPageProgress
        val dotsY = h - 4.dp.toPx()
        val baseDotW = 4.2.dp.toPx()
        val activePillW = 10.dp.toPx()
        val dotH = 4.2.dp.toPx()
        val gap = 4.dp.toPx()

        val widths = FloatArray(pageCount) { index ->
          val dist = abs(horizontalPageProgress - index).coerceIn(0f, 1f)
          activePillW - (activePillW - baseDotW) * dist
        }
        val totalW = widths.sum() + gap * (pageCount - 1)
        var curX = (w - totalW) / 2f

        for (index in 0 until pageCount) {
          val dist = abs(horizontalPageProgress - index).coerceIn(0f, 1f)
          val alpha = 0.95f - 0.60f * dist
          val itemW = widths[index]
          drawRoundRect(
            color = indicatorColor.copy(alpha = alpha),
            topLeft = Offset(curX, dotsY - dotH / 2f),
            size = Size(itemW, dotH),
            cornerRadius = CornerRadius(dotH / 2f, dotH / 2f)
          )
          curX += itemW + gap
        }
      }
    }
  }
}
