package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.ui.icons.SvgDockAddIcon
import com.example.ui.icons.SvgDockRemoveIcon
import com.example.ui.icons.SvgHideIcon
import com.example.ui.icons.SvgInfoIcon
import com.example.ui.icons.SvgUninstallIcon
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val OptionItemShape = RoundedCornerShape(9.dp)
private val IconBadgeShape = RoundedCornerShape(7.dp)
private val OptionPressSpring = spring<Float>(stiffness = Spring.StiffnessMedium)
private val OptionReleaseSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy)

private enum class ArrowSide {
  LEFT,   // Menu is to the RIGHT of the icon; arrow is on the LEFT edge pointing left
  RIGHT,  // Menu is to the LEFT of the icon; arrow is on the RIGHT edge pointing right
  TOP,    // Menu is BELOW the icon; arrow is on the TOP edge pointing up
  BOTTOM  // Menu is ABOVE the icon; arrow is on the BOTTOM edge pointing down
}

/**
 * Unified callout shape combining a rounded rectangle body and a pointer triangle
 * pointing directly toward the anchored app icon.
 */
private class CalloutBubbleShape(
  private val arrowSide: ArrowSide,
  private val arrowLengthPx: Float,
  private val arrowBasePx: Float,
  private val arrowAlignPx: Float,
  private val cornerRadiusPx: Float
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val w = size.width
    val h = size.height
    val r = cornerRadiusPx
    val halfBase = arrowBasePx * 0.5f

    val bodyLeft = if (arrowSide == ArrowSide.LEFT) arrowLengthPx else 0f
    val bodyTop = if (arrowSide == ArrowSide.TOP) arrowLengthPx else 0f
    val bodyRight = if (arrowSide == ArrowSide.RIGHT) (w - arrowLengthPx).coerceAtLeast(bodyLeft) else w
    val bodyBottom = if (arrowSide == ArrowSide.BOTTOM) (h - arrowLengthPx).coerceAtLeast(bodyTop) else h

    val bodyPath = Path().apply {
      addRoundRect(
        RoundRect(
          rect = Rect(bodyLeft, bodyTop, bodyRight, bodyBottom),
          cornerRadius = CornerRadius(r, r)
        )
      )
    }

    val arrowPath = Path().apply {
      when (arrowSide) {
        ArrowSide.LEFT -> {
          val minCy = bodyTop + r + halfBase
          val maxCy = (bodyBottom - r - halfBase).coerceAtLeast(minCy)
          val cy = arrowAlignPx.coerceIn(minCy, maxCy)
          moveTo(bodyLeft + 2f, cy - halfBase)
          lineTo(0f, cy)
          lineTo(bodyLeft + 2f, cy + halfBase)
          close()
        }
        ArrowSide.RIGHT -> {
          val minCy = bodyTop + r + halfBase
          val maxCy = (bodyBottom - r - halfBase).coerceAtLeast(minCy)
          val cy = arrowAlignPx.coerceIn(minCy, maxCy)
          moveTo(bodyRight - 2f, cy - halfBase)
          lineTo(w, cy)
          lineTo(bodyRight - 2f, cy + halfBase)
          close()
        }
        ArrowSide.TOP -> {
          val minCx = bodyLeft + r + halfBase
          val maxCx = (bodyRight - r - halfBase).coerceAtLeast(minCx)
          val cx = arrowAlignPx.coerceIn(minCx, maxCx)
          moveTo(cx - halfBase, bodyTop + 2f)
          lineTo(cx, 0f)
          lineTo(cx + halfBase, bodyTop + 2f)
          close()
        }
        ArrowSide.BOTTOM -> {
          val minCx = bodyLeft + r + halfBase
          val maxCx = (bodyRight - r - halfBase).coerceAtLeast(minCx)
          val cx = arrowAlignPx.coerceIn(minCx, maxCx)
          moveTo(cx - halfBase, bodyBottom - 2f)
          lineTo(cx, h)
          lineTo(cx + halfBase, bodyBottom - 2f)
          close()
        }
      }
    }

    val combined = Path()
    combined.op(bodyPath, arrowPath, PathOperation.Union)
    return Outline.Generic(combined)
  }
}

private val FocusedDarkShadowStyle = TextStyle(
  shadow = Shadow(
    color = Color.Black.copy(alpha = 0.65f),
    offset = Offset(0f, 1.8f),
    blurRadius = 6.0f
  )
)

private val FocusedLightShadowStyle = TextStyle(
  shadow = Shadow(
    color = Color.White.copy(alpha = 0.80f),
    offset = Offset(0f, 1.0f),
    blurRadius = 4.5f
  )
)

@Composable
fun AppContextMenu(
  app: AppInfo,
  isInDock: Boolean,
  viewModel: LauncherViewModel,
  onDismiss: () -> Unit,
  onAppInfoClick: () -> Unit,
  onToggleDockClick: () -> Unit,
  onUninstallClick: () -> Unit,
  onHideClick: (() -> Unit)? = null,
  iconShape: String = "sammy",
  selectedIconPack: String? = null,
  anchor: AppMenuAnchor? = null
) {
  val isDark = isSystemInDarkTheme()
  val density = LocalDensity.current
  val menuBg = if (isDark) Color(0xF5161619) else Color(0xF8FFFFFF)
  val borderColor = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
  val textColor = if (isDark) Color(0xFFF5F5F7) else Color(0xFF141416)
  val iconBoxBg = if (isDark) Color(0x14FFFFFF) else Color(0x0B000000)
  val iconTint = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
  val dividerColor = if (isDark) Color(0x12FFFFFF) else Color(0x0D000000)
  val scrimColor = if (isDark) Color(0x3D000000) else Color(0x22000000)

  var iconBitmap by remember(app.packageName, app.activityName, iconShape, selectedIconPack, app.lastUpdateTime) {
    mutableStateOf(viewModel.getAppIconFast(app.packageName, app.activityName))
  }

  LaunchedEffect(app.packageName, app.activityName, iconShape, selectedIconPack, app.lastUpdateTime) {
    if (iconBitmap == null) {
      iconBitmap = viewModel.loadAppIconAsync(app.packageName, app.activityName)
    }
  }

  // Subtle focus pop animation for the sharp unblurred app icon and callout menu
  val focusScale = remember { Animatable(1f) }
  val menuPopProgress = remember { Animatable(0.86f) }
  LaunchedEffect(app.packageName, app.activityName) {
    launch {
      focusScale.snapTo(0.96f)
      focusScale.animateTo(
        targetValue = 1.05f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium
        )
      )
    }
    launch {
      menuPopProgress.snapTo(0.86f)
      menuPopProgress.animateTo(
        targetValue = 1f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium
        )
      )
    }
  }

  val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
  val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

  // Balanced minimalist callout dimensions
  val cardBodyWidthDp = 176.dp
  val arrowLengthDp = 7.dp
  val arrowBaseDp = 12.dp
  val cornerRadiusDp = 16.dp

  val arrowLengthPx = with(density) { arrowLengthDp.toPx() }
  val arrowBasePx = with(density) { arrowBaseDp.toPx() }
  val cornerRadiusPx = with(density) { cornerRadiusDp.toPx() }
  val gapToIconPx = with(density) { 6.dp.toPx() }
  val screenMarginPx = with(density) { 8.dp.toPx() }

  val itemCount = if (onHideClick != null) 4 else 3
  val estimatedMenuWidthPx = with(density) { (cardBodyWidthDp + arrowLengthDp).toPx() }
  val estimatedMenuHeightPx = with(density) { (if (itemCount == 4) 168.dp else 128.dp).toPx() }

  var measuredMenuSize by remember {
    mutableStateOf(IntSize(estimatedMenuWidthPx.roundToInt(), estimatedMenuHeightPx.roundToInt()))
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(scrimColor)
      .pointerInput(Unit) {
        detectTapGestures(onTap = { onDismiss() })
      }
  ) {
    val screenW = constraints.maxWidth.toFloat()
    val screenH = constraints.maxHeight.toFloat()

    val iconRect = anchor?.iconBoundsInRoot ?: Rect.Zero
    val itemRect = anchor?.itemBoundsInRoot ?: iconRect
    val hasValidAnchor = iconRect != Rect.Zero && iconRect.width > 0f && iconRect.height > 0f

    // 1. Sharp, unblurred replica of the pressed app icon + label at its exact screen coordinates
    if (hasValidAnchor) {
      val iconWidthDp = with(density) { iconRect.width.toDp() }
      val iconHeightDp = with(density) { iconRect.height.toDp() }

      // Render sharp icon at exact iconRect coordinates
      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              x = iconRect.left.roundToInt(),
              y = iconRect.top.roundToInt()
            )
          }
          .size(width = iconWidthDp, height = iconHeightDp)
          .graphicsLayer {
            scaleX = focusScale.value
            scaleY = focusScale.value
          }
          .pointerInput(Unit) {
            detectTapGestures(onTap = { onDismiss() })
          },
        contentAlignment = Alignment.Center
      ) {
        if (iconBitmap != null) {
          Image(
            bitmap = iconBitmap!!,
            contentDescription = "Ícone em foco de ${app.label}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
        }
      }

      // If the pressed item had a visible label (e.g., inside App Drawer), keep the label sharp too
      if (anchor?.showLabel == true) {
        val labelTopPx = iconRect.bottom + with(density) { 4.dp.toPx() }
        val labelWidthDp = with(density) { itemRect.width.toDp() }
        val actualTextColor = anchor.textColor ?: if (isDark) Color.White else Color(0xFF1D1D1F)
        val textStyle = if (isDark) FocusedDarkShadowStyle else FocusedLightShadowStyle

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                x = itemRect.left.roundToInt(),
                y = labelTopPx.roundToInt()
              )
            }
            .width(labelWidthDp),
          contentAlignment = Alignment.TopCenter
        ) {
          Text(
            text = app.label,
            color = actualTextColor,
            fontSize = 13.8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = textStyle,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.5.dp)
          )
        }
      }
    }

    // 2. Compute optimal side placement (immediately beside the icon) & screen-edge clamping
    val menuW = measuredMenuSize.width.toFloat().coerceAtLeast(estimatedMenuWidthPx)
    val menuH = measuredMenuSize.height.toFloat().coerceAtLeast(estimatedMenuHeightPx)

    val topSafePx = statusBarTopPx + with(density) { 10.dp.toPx() }
    val bottomSafePx = if (hasValidAnchor) {
      // Allow the menu to align down to the bottom of dock icons while staying inside screen bounds
      val navSafe = screenH - navBarBottomPx - with(density) { 8.dp.toPx() }
      val iconBottomSafe = (iconRect.bottom + cornerRadiusPx + arrowBasePx).coerceAtMost(screenH - screenMarginPx)
      maxOf(navSafe, iconBottomSafe)
    } else {
      screenH - navBarBottomPx - with(density) { 12.dp.toPx() }
    }

    val arrowSide: ArrowSide
    val menuX: Float
    val menuY: Float
    val arrowAlignPx: Float
    val transformOrigin: TransformOrigin

    if (hasValidAnchor) {
      val spaceRight = screenW - iconRect.right - gapToIconPx - screenMarginPx
      val spaceLeft = iconRect.left - gapToIconPx - screenMarginPx

      val preferRightSide = if (iconRect.center.x <= screenW * 0.5f) {
        spaceRight >= menuW * 0.85f || spaceRight >= spaceLeft
      } else {
        spaceLeft < menuW * 0.85f && spaceRight > spaceLeft
      }

      val canFitSide = spaceRight >= menuW * 0.82f || spaceLeft >= menuW * 0.82f

      if (canFitSide) {
        if (preferRightSide) {
          // Menu sits immediately to the RIGHT of the pressed icon; arrow on LEFT edge pointing left
          arrowSide = ArrowSide.LEFT
          val idealX = iconRect.right + gapToIconPx
          menuX = idealX.coerceIn(
            screenMarginPx,
            (screenW - menuW - screenMarginPx).coerceAtLeast(screenMarginPx)
          )
        } else {
          // Menu sits immediately to the LEFT of the pressed icon; arrow on RIGHT edge pointing right
          arrowSide = ArrowSide.RIGHT
          val idealX = iconRect.left - gapToIconPx - menuW
          menuX = idealX.coerceIn(
            screenMarginPx,
            (screenW - menuW - screenMarginPx).coerceAtLeast(screenMarginPx)
          )
        }

        val idealY = iconRect.center.y - menuH * 0.5f
        val maxTopY = (bottomSafePx - menuH).coerceAtLeast(topSafePx)
        menuY = idealY.coerceIn(topSafePx, maxTopY)

        val minArrowY = cornerRadiusPx + arrowBasePx * 0.5f + 2f
        val maxArrowY = (menuH - cornerRadiusPx - arrowBasePx * 0.5f - 2f).coerceAtLeast(minArrowY)
        arrowAlignPx = (iconRect.center.y - menuY).coerceIn(minArrowY, maxArrowY)

        val pivotX = if (arrowSide == ArrowSide.LEFT) 0f else 1f
        val pivotY = (arrowAlignPx / menuH).coerceIn(0.1f, 0.9f)
        transformOrigin = TransformOrigin(pivotX, pivotY)
      } else {
        // Fallback for ultra-narrow screens if neither horizontal side fits
        val spaceAbove = iconRect.top - gapToIconPx - topSafePx
        val spaceBelow = bottomSafePx - iconRect.bottom - gapToIconPx
        if (spaceAbove >= menuH || spaceAbove >= spaceBelow) {
          arrowSide = ArrowSide.BOTTOM
          menuY = (iconRect.top - gapToIconPx - menuH).coerceIn(topSafePx, (bottomSafePx - menuH).coerceAtLeast(topSafePx))
        } else {
          arrowSide = ArrowSide.TOP
          menuY = (iconRect.bottom + gapToIconPx).coerceIn(topSafePx, (bottomSafePx - menuH).coerceAtLeast(topSafePx))
        }
        val idealX = iconRect.center.x - menuW * 0.5f
        menuX = idealX.coerceIn(screenMarginPx, (screenW - menuW - screenMarginPx).coerceAtLeast(screenMarginPx))

        val minArrowX = cornerRadiusPx + arrowBasePx * 0.5f + 2f
        val maxArrowX = (menuW - cornerRadiusPx - arrowBasePx * 0.5f - 2f).coerceAtLeast(minArrowX)
        arrowAlignPx = (iconRect.center.x - menuX).coerceIn(minArrowX, maxArrowX)

        val pivotX = (arrowAlignPx / menuW).coerceIn(0.1f, 0.9f)
        val pivotY = if (arrowSide == ArrowSide.TOP) 0f else 1f
        transformOrigin = TransformOrigin(pivotX, pivotY)
      }
    } else {
      arrowSide = ArrowSide.LEFT
      menuX = ((screenW - menuW) * 0.5f).coerceAtLeast(screenMarginPx)
      menuY = ((screenH - menuH) * 0.5f).coerceAtLeast(topSafePx)
      arrowAlignPx = menuH * 0.5f
      transformOrigin = TransformOrigin.Center
    }

    val bubbleShape = remember(arrowSide, arrowLengthPx, arrowBasePx, arrowAlignPx, cornerRadiusPx) {
      CalloutBubbleShape(
        arrowSide = arrowSide,
        arrowLengthPx = arrowLengthPx,
        arrowBasePx = arrowBasePx,
        arrowAlignPx = arrowAlignPx,
        cornerRadiusPx = cornerRadiusPx
      )
    }

    val totalWidthDp = if (arrowSide == ArrowSide.LEFT || arrowSide == ArrowSide.RIGHT) {
      cardBodyWidthDp + arrowLengthDp
    } else {
      cardBodyWidthDp
    }

    // 3. Compact Callout Menu connected to the icon via the pointer arrow
    Box(
      modifier = Modifier
        .offset {
          IntOffset(
            x = menuX.roundToInt(),
            y = menuY.roundToInt()
          )
        }
        .width(totalWidthDp)
        .onSizeChanged { size ->
          if (size.width > 0 && size.height > 0) {
            measuredMenuSize = size
          }
        }
        .graphicsLayer {
          this.transformOrigin = transformOrigin
          scaleX = menuPopProgress.value
          scaleY = menuPopProgress.value
        }
        .shadow(
          elevation = 14.dp,
          shape = bubbleShape,
          ambientColor = Color.Black.copy(alpha = 0.22f),
          spotColor = Color.Black.copy(alpha = 0.26f)
        )
        .clip(bubbleShape)
        .background(menuBg, bubbleShape)
        .border(0.75.dp, borderColor, bubbleShape)
        .pointerInput(Unit) {
          detectTapGestures { /* consume tap inside menu */ }
        }
        .padding(
          start = if (arrowSide == ArrowSide.LEFT) arrowLengthDp else 0.dp,
          end = if (arrowSide == ArrowSide.RIGHT) arrowLengthDp else 0.dp,
          top = if (arrowSide == ArrowSide.TOP) arrowLengthDp else 0.dp,
          bottom = if (arrowSide == ArrowSide.BOTTOM) arrowLengthDp else 0.dp
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Item 1: App Info
        ContextMenuOptionItem(
          icon = SvgInfoIcon,
          title = "Informações do app",
          iconTint = iconTint,
          iconBoxBg = iconBoxBg,
          textColor = textColor,
          onClick = {
            onDismiss()
            onAppInfoClick()
          }
        )

        MenuHairlineDivider(dividerColor)

        // Item 2: Add to / Remove from Dock
        ContextMenuOptionItem(
          icon = if (isInDock) SvgDockRemoveIcon else SvgDockAddIcon,
          title = if (isInDock) "Remover da dock" else "Adicionar à dock",
          iconTint = iconTint,
          iconBoxBg = iconBoxBg,
          textColor = textColor,
          onClick = {
            onDismiss()
            onToggleDockClick()
          }
        )

        if (onHideClick != null) {
          MenuHairlineDivider(dividerColor)

          // Item 3: Ocultar App
          ContextMenuOptionItem(
            icon = SvgHideIcon,
            title = "Ocultar app",
            iconTint = iconTint,
            iconBoxBg = iconBoxBg,
            textColor = textColor,
            onClick = {
              onDismiss()
              onHideClick()
            }
          )
        }

        MenuHairlineDivider(dividerColor)

        // Item 4: Uninstall
        ContextMenuOptionItem(
          icon = SvgUninstallIcon,
          title = "Desinstalar",
          iconTint = Color(0xFFFF453A),
          iconBoxBg = Color(0x24FF453A),
          textColor = Color(0xFFFF453A),
          onClick = {
            onDismiss()
            onUninstallClick()
          }
        )
      }
    }
  }
}

@Composable
private fun MenuHairlineDivider(color: Color) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 6.dp, vertical = 1.dp)
      .height(0.6.dp)
      .background(color)
  )
}

@Composable
private fun ContextMenuOptionItem(
  icon: ImageVector,
  title: String,
  iconTint: Color,
  iconBoxBg: Color,
  textColor: Color,
  onClick: () -> Unit
) {
  val scaleAnim = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
      }
      .clip(OptionItemShape)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            scope.launch {
              scaleAnim.animateTo(0.95f, OptionPressSpring)
            }
            tryAwaitRelease()
            scaleAnim.animateTo(1f, OptionReleaseSpring)
          },
          onTap = { onClick() }
        )
      }
      .padding(vertical = 8.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(IconBadgeShape)
        .background(iconBoxBg),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconTint,
        modifier = Modifier.size(14.dp)
      )
    }
    Spacer(modifier = Modifier.width(9.dp))
    Text(
      text = title,
      color = textColor,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = (-0.1).sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}


