package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreen
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
fun SettingsScreen(viewModel: BuilderViewModel) {
  val authState by viewModel.authUiState.collectAsState()

  var tokenInput by remember(authState.token) { mutableStateOf(authState.token) }
  var isTokenVisible by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Configuração",
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = TextPrimary
    )

    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = AccentBlueBright)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Token GitHub (PAT)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary
          )
        }

        OutlinedTextField(
          value = tokenInput,
          onValueChange = { tokenInput = it },
          placeholder = { Text("ghp_...", color = TextMuted) },
          singleLine = true,
          visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(
              onClick = { isTokenVisible = !isTokenVisible },
              modifier = Modifier.testTag("toggle_token_visibility")
            ) {
              Icon(
                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Alternar visibilidade",
                tint = TextSecondary
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlueBright,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("github_token_input")
        )

        Button(
          onClick = { viewModel.saveAndVerifyToken(tokenInput) },
          enabled = tokenInput.isNotBlank() && !authState.isVerifying,
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlueBright),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verify_token_button")
        ) {
          if (authState.isVerifying) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkSurface, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verificando...", color = DarkSurface, fontSize = 13.sp)
          } else {
            Text("Salvar Token", color = DarkSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }

        if (authState.user != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreenBright, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Conectado como @${authState.user?.login}",
              fontSize = 13.sp,
              color = AccentGreenBright,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    if (authState.errorMessage != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = authState.errorMessage ?: "", color = AccentRed, fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
