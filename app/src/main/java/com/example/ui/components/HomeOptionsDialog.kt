package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.icons.SvgPersonalizationIcon
import com.example.ui.icons.SvgSettingsIcon
import kotlinx.coroutines.launch

@Composable
fun HomeOptionsDialog(
  onDismiss: () -> Unit,
  onLaunchWallpaperPicker: () -> Unit,
  onOpenSettings: () -> Unit
) {
  val isDark = isSystemInDarkTheme()
  val menuBg = if (isDark) Color(0xF2161619) else Color(0xF7FFFFFF)
  val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
  val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
  val textColor = if (isDark) Color(0xFFF5F5F7) else Color(0xFF141416)
  val iconBoxBg = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.045f)
  val iconTint = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
  val scrimColor = if (isDark) Color(0x40000000) else Color(0x24000000)
  val cardShape = RoundedCornerShape(22.dp)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(scrimColor)
      .pointerInput(Unit) {
        detectTapGestures(onTap = { onDismiss() })
      },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .width(236.dp)
        .shadow(
          elevation = 16.dp,
          shape = cardShape,
          ambientColor = Color.Black.copy(alpha = 0.20f),
          spotColor = Color.Black.copy(alpha = 0.24f)
        )
        .clip(cardShape)
        .background(menuBg)
        .border(0.75.dp, borderColor, cardShape)
        .pointerInput(Unit) {
          detectTapGestures { /* consume tap inside dialog */ }
        }
    ) {
      Column(
        modifier = Modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        HomeOptionItem(
          icon = SvgPersonalizationIcon,
          title = "Personalização",
          iconTint = iconTint,
          iconBoxBg = iconBoxBg,
          textColor = textColor,
          onClick = {
            onDismiss()
            onLaunchWallpaperPicker()
          }
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .height(0.5.dp)
            .background(dividerColor)
        )

        HomeOptionItem(
          icon = SvgSettingsIcon,
          title = "Configurações",
          iconTint = iconTint,
          iconBoxBg = iconBoxBg,
          textColor = textColor,
          onClick = {
            onDismiss()
            onOpenSettings()
          }
        )
      }
    }
  }
}

@Composable
private fun HomeOptionItem(
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
      .heightIn(min = 48.dp)
      .graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
      }
      .clip(RoundedCornerShape(16.dp))
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            scope.launch {
              scaleAnim.animateTo(0.96f, spring(stiffness = Spring.StiffnessMedium))
            }
            tryAwaitRelease()
            scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
          },
          onTap = { onClick() }
        )
      }
      .padding(vertical = 8.dp, horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(iconBoxBg),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconTint,
        modifier = Modifier.size(17.dp)
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      color = textColor,
      fontSize = 14.5.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = (-0.15).sp
    )
  }
}
