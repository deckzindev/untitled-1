package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.BuildStepProgress
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

@Composable
fun BuildScreen(
  viewModel: BuilderViewModel,
  onNavigateToSettings: () -> Unit,
  onNavigateToHistory: () -> Unit
) {
  val authState by viewModel.authUiState.collectAsState()
  val buildState by viewModel.buildUiState.collectAsState()

  val zipPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.analyzeZipUri(uri)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (buildState.isBuilding) {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentBlueBright.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentBlueBright, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(10.dp))
              val step = buildState.currentProgress
              val stepText = when (step) {
                is BuildStepProgress.UploadingFiles -> "Enviando arquivos (${step.current}/${step.total})"
                is BuildStepProgress.CreatingTree -> "Criando árvore Git..."
                is BuildStepProgress.CreatingCommit -> "Criando commit..."
                is BuildStepProgress.UpdatingBranch -> "Atualizando branch..."
                is BuildStepProgress.WaitingForRunner -> "Aguardando GitHub Actions..."
                is BuildStepProgress.WorkflowStarted -> "Compilação iniciada (#${step.runId})"
                else -> "Compilando..."
              }
              Text(
                text = stepText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AccentBlueBright
              )
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentBlueBright)
          }
        }
      } else if (buildState.isAnalyzing) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentBlueBright)
        Text("Analisando arquivo...", fontSize = 13.sp, color = TextSecondary)
      } else if (buildState.projectAnalysis == null) {
        Button(
          onClick = {
            zipPickerLauncher.launch(
              arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream",
                "*/*"
              )
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("pick_zip_button"),
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlueBright),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.FolderZip, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Selecionar Arquivo",
            color = DarkSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      } else {
        // File selected: show file name and compile button
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FolderZip, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = buildState.projectName.ifBlank { "Projeto" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary
              )
            }

            IconButton(
              onClick = { viewModel.clearSelectedProject() },
              modifier = Modifier.size(28.dp).testTag("clear_project_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Remover", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
          }
        }

        Button(
          onClick = {
            if (authState.token.isBlank()) {
              onNavigateToSettings()
            } else {
              viewModel.startBuild { onNavigateToHistory() }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("start_build_button"),
          colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Compilar APK",
            color = DarkSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }

      if (buildState.errorMessage != null && !buildState.isBuilding) {
        Card(
          colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = buildState.errorMessage ?: "", color = AccentRed, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
