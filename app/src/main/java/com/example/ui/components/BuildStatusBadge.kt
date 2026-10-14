package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentRed

@Composable
fun BuildStatusBadge(
  status: String,
  conclusion: String? = null,
  modifier: Modifier = Modifier
) {
  val (label, bgCol, borderCol, textCol, icon) = when {
    status == "uploading" -> {
      StatusStyle("Enviando Código", AccentAmber.copy(alpha = 0.15f), AccentAmber, AccentAmber, null)
    }
    status == "queued" -> {
      StatusStyle("Na Fila do GitHub", AccentAmber.copy(alpha = 0.15f), AccentAmber, AccentAmber, Icons.Default.HourglassTop)
    }
    status == "in_progress" -> {
      StatusStyle("Compilando APK...", AccentBlueBright.copy(alpha = 0.15f), AccentBlueBright, AccentBlueBright, null)
    }
    status == "completed" && conclusion == "success" -> {
      StatusStyle("APK Compilado!", AccentGreenBright.copy(alpha = 0.15f), AccentGreenBright, AccentGreenBright, Icons.Default.CheckCircle)
    }
    status == "completed" && conclusion == "failure" -> {
      StatusStyle("Falha na Compilação", AccentRed.copy(alpha = 0.15f), AccentRed, AccentRed, Icons.Default.Error)
    }
    status == "failed" -> {
      StatusStyle("Erro no Envio", AccentRed.copy(alpha = 0.15f), AccentRed, AccentRed, Icons.Default.Error)
    }
    else -> {
      StatusStyle(status.replaceFirstChar { it.uppercase() }, Color.Gray.copy(alpha = 0.15f), Color.Gray, Color.White, null)
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(bgCol)
      .border(1.dp, borderCol.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (status == "in_progress" || status == "uploading") {
      CircularProgressIndicator(
        modifier = Modifier.size(12.dp),
        strokeWidth = 2.dp,
        color = textCol
      )
      Spacer(modifier = Modifier.width(6.dp))
    } else if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = textCol,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
    } else {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(textCol)
          .then(if (status in listOf("in_progress", "uploading", "queued")) Modifier.alpha(pulseAlpha) else Modifier)
      )
      Spacer(modifier = Modifier.width(6.dp))
    }

    Text(
      text = label,
      color = textCol,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

private data class StatusStyle(
  val label: String,
  val bgCol: Color,
  val borderCol: Color,
  val textCol: Color,
  val icon: androidx.compose.ui.graphics.vector.ImageVector?
)
