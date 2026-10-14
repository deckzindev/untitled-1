package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectBuildEntity
import com.example.ui.components.BuildStatusBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BuilderViewModel
import java.io.File

@Composable
fun HistoryScreen(
  viewModel: BuilderViewModel,
  onNavigateToBuild: () -> Unit
) {
  val context = LocalContext.current
  val builds by viewModel.allBuilds.collectAsState()
  val downloadingBuildIds by viewModel.downloadingBuildIds.collectAsState()
  val statusNotice by viewModel.statusNotice.collectAsState()

  // Display status notice if present
  LaunchedEffect(statusNotice) {
    statusNotice?.let { notice ->
      Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
      viewModel.clearStatusNotice()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Histórico de Compilações",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = TextPrimary
        )

        if (builds.isNotEmpty()) {
          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(DarkSurfaceVariant)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "${builds.size}",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = AccentGreenBright
            )
          }
        }
      }

      if (builds.isNotEmpty()) {
        IconButton(
          onClick = { viewModel.clearAllBuilds() },
          modifier = Modifier
            .size(36.dp)
            .testTag("clear_all_history_button")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Limpar todo o histórico",
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    if (builds.isEmpty()) {
      // Empty state
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(60.dp)
              .clip(CircleShape)
              .background(DarkSurfaceVariant)
              .border(1.dp, DarkBorder, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Build,
              contentDescription = null,
              tint = AccentBlueBright,
              modifier = Modifier.size(28.dp)
            )
          }

          Text(
            text = "Nenhuma compilação no histórico",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = TextPrimary
          )

          Text(
            text = "Envie um projeto ZIP para iniciar a compilação do seu APK.",
            fontSize = 13.sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = onNavigateToBuild,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier.testTag("goto_build_screen_button")
          ) {
            Text(
              text = "Nova Compilação",
              color = DarkSurface,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }
    } else {
      // Builds list
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(builds, key = { it.id }) { build ->
          val isDownloading = downloadingBuildIds.contains(build.id)
          val isDownloaded = remember(build.localApkPath, build.id) {
            val path = build.localApkPath
            !path.isNullOrBlank() && File(path).let { it.exists() && it.length() > 0 }
          }

          BuildCardItem(
            build = build,
            isDownloading = isDownloading,
            isDownloaded = isDownloaded,
            onInstallOrDownload = {
              viewModel.downloadAndInstallApk(build, context)
            },
            onDelete = {
              viewModel.deleteBuild(build.id)
            },
            onClickCard = {
              viewModel.openLogsForBuild(build)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun BuildCardItem(
  build: ProjectBuildEntity,
  isDownloading: Boolean,
  isDownloaded: Boolean,
  onInstallOrDownload: () -> Unit,
  onDelete: () -> Unit,
  onClickCard: () -> Unit
) {
  // Determine clean filename
  val rawFileName = build.apkFileName
    ?: build.artifactName
    ?: "${build.projectName.ifBlank { "app" }}-release.apk"
  val displayFileName = if (rawFileName.endsWith(".apk", ignoreCase = true)) {
    rawFileName
  } else {
    "$rawFileName.apk"
  }

  val isSuccess = build.status == "completed" && build.conclusion == "success"
  val isRunning = build.status in listOf("uploading", "queued", "in_progress")
  val isFailed = build.status in listOf("failed", "cancelled") || build.conclusion in listOf("failure", "cancelled")

  Card(
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClickCard)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Icon Container
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurfaceVariant)
          .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        when {
          isSuccess -> {
            Icon(
              imageVector = Icons.Default.Android,
              contentDescription = "APK Pronto",
              tint = AccentGreenBright,
              modifier = Modifier.size(24.dp)
            )
          }
          isRunning -> {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = AccentBlueBright
            )
          }
          isFailed -> {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = "Falha",
              tint = AccentRed,
              modifier = Modifier.size(24.dp)
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Default.HourglassTop,
              contentDescription = "Aguardando",
              tint = AccentAmber,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // 2. File Name & Compilation Status
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Nome do arquivo
        Text(
          text = displayFileName,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        // Status da compilação
        BuildStatusBadge(status = build.status, conclusion = build.conclusion)
      }

      Spacer(modifier = Modifier.width(8.dp))

      // 3. Action Buttons: Instalar / Baixar and Lixeira
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Botão para instalar / baixar o aplicativo
        when {
          isDownloading -> {
            Button(
              onClick = {},
              enabled = false,
              colors = ButtonDefaults.buttonColors(
                disabledContainerColor = DarkSurfaceVariant,
                disabledContentColor = TextPrimary
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .height(36.dp)
                .testTag("downloading_apk_${build.id}")
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AccentBlueBright
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Baixando...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
          }

          isSuccess -> {
            if (isDownloaded) {
              // Já baixado no dispositivo -> Iniciar instalação direta!
              Button(
                onClick = onInstallOrDownload,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(36.dp)
                  .testTag("install_apk_${build.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.InstallMobile,
                  contentDescription = "Instalar APK",
                  tint = DarkSurface,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Instalar",
                  color = DarkSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              // Concluído no GitHub -> Baixar e preparar instalação
              Button(
                onClick = onInstallOrDownload,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlueBright),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(36.dp)
                  .testTag("download_apk_${build.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Baixar e Instalar APK",
                  tint = DarkSurface,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Instalar",
                  color = DarkSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          isRunning -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.height(36.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 2.dp,
                  color = AccentBlueBright
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Aguarde",
                  fontSize = 11.sp,
                  color = TextSecondary,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          else -> {
            // Falha
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = AccentRed.copy(alpha = 0.12f),
              border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
              modifier = Modifier
                .height(36.dp)
                .clickable(onClick = onClickCard)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Ver Erro",
                  fontSize = 11.sp,
                  color = AccentRed,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // Botão de lixeira para excluir o registro do histórico
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(38.dp)
            .testTag("delete_build_${build.id}")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Excluir histórico",
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
