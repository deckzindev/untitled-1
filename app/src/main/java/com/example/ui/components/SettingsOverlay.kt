package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.security.SecurityAuthHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlay(
  viewModel: LauncherViewModel,
  showAppNames: Boolean,
  onShowAppNamesChange: (Boolean) -> Unit,
  currentIconSizeProgress: Float,
  onIconSizeProgressChange: (Float) -> Unit,
  allApps: List<AppInfo> = emptyList(),
  hiddenAppKeys: Set<String> = emptySet(),
  onToggleHideAppKey: (String) -> Unit = {},
  lockedAppKeys: Set<String> = emptySet(),
  onSaveLockedApps: (Set<String>) -> Unit = {},
  onRequestSecurityAuth: (title: String, subtitle: String, onSuccess: () -> Unit, onError: ((String) -> Unit)?) -> Unit = { _, _, s, _ -> s() },
  currentDrawerStyle: String = "vertical",
  onDrawerStyleChange: (String) -> Unit = {},
  onEnterAppEditMode: () -> Unit = {},
  onClose: () -> Unit
) {
  val context = LocalContext.current
  var showHiddenAppsDialog by remember { mutableStateOf(false) }
  var showAppLockDialog by remember { mutableStateOf(false) }

  val isDark = isSystemInDarkTheme()
  val bgOverlayColor = if (isDark) Color(0xFF0E0E10) else Color(0xFFF6F6F8)
  val sectionBgColor = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
  val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
  val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
  val textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
  val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
  val accentColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgOverlayColor)
      .pointerInput(Unit) { detectTapGestures { } }
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Minimalist Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Configurações",
          color = textPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.4).sp
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF222226) else Color(0xFFEAECEF))
            .clickable { onClose() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Fechar",
            color = textPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(0.5.dp)
          .background(dividerColor)
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
      ) {
        // --- 1. Gaveta de Aplicativos ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "GAVETA DE APLICATIVOS",
            color = textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(sectionBgColor)
              .border(0.8.dp, cardBorderColor, RoundedCornerShape(16.dp))
          ) {
            // 1A. Estilo de navegação da gaveta
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Estilo de navegação",
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = (-0.1).sp
                )
                Text(
                  text = "Escolha como deslizar pelos aplicativos",
                  color = textSecondary,
                  fontSize = 13.sp
                )
              }

              DrawerStylePhonesRow(
                currentDrawerStyle = currentDrawerStyle,
                onDrawerStyleChange = onDrawerStyleChange,
                isVisible = !showAppLockDialog && !showHiddenAppsDialog,
                modifier = Modifier.fillMaxWidth()
              )
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(0.5.dp)
                .background(dividerColor)
            )

            // 1B. Modo de edição de aplicativos
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onEnterAppEditMode() }
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("enter_app_edit_mode_row"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Modo de edição de aplicativos",
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = (-0.1).sp
                )
                Text(
                  text = "Mover e reorganizar a posição dos aplicativos",
                  color = textSecondary,
                  fontSize = 13.sp
                )
              }
            }
          }
        }

        // --- 2. Aparência e Exibição ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "APARÊNCIA E EXIBIÇÃO",
            color = textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(sectionBgColor)
              .border(0.8.dp, cardBorderColor, RoundedCornerShape(16.dp))
          ) {
            // 2A. Tamanho dos ícones
            val iconSizeOptions = remember { listOf(0.50f, 0.80f, 1.00f, 1.20f) }
            val iconSizeLabels = remember { listOf("50%", "80%", "100%", "120%") }
            val selectedOptionIndex = remember(currentIconSizeProgress) {
              iconSizeOptions.indices.minByOrNull { idx ->
                kotlin.math.abs(iconSizeOptions[idx] - currentIconSizeProgress)
              } ?: 2
            }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Text(
                    text = "Tamanho dos ícones",
                    color = textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.1).sp
                  )
                  Text(
                    text = "Ajustar escala dos ícones na grade e na dock",
                    color = textSecondary,
                    fontSize = 13.sp
                  )
                }
                Text(
                  text = iconSizeLabels[selectedOptionIndex],
                  color = accentColor,
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Column(
                modifier = Modifier.fillMaxWidth()
              ) {
                Slider(
                  value = selectedOptionIndex.toFloat(),
                  onValueChange = { rawVal ->
                    val snappedIndex = rawVal.roundToInt().coerceIn(0, iconSizeOptions.lastIndex)
                    onIconSizeProgressChange(iconSizeOptions[snappedIndex])
                  },
                  valueRange = 0f..3f,
                  steps = 2,
                  modifier = Modifier.testTag("icon_size_slider"),
                  colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = if (isDark) Color(0xFF2A2A2E) else Color(0xFFE5E5EA),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                  )
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  iconSizeLabels.forEachIndexed { index, label ->
                    val isSelected = index == selectedOptionIndex
                    Text(
                      text = label,
                      color = if (isSelected) textPrimary else textSecondary,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      modifier = Modifier.clickable {
                        onIconSizeProgressChange(iconSizeOptions[index])
                      }
                    )
                  }
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(0.5.dp)
                .background(dividerColor)
            )

            // 2B. Rótulos dos aplicativos
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowAppNamesChange(!showAppNames) }
                .padding(horizontal = 18.dp, vertical = 15.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Rótulos dos aplicativos",
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = (-0.1).sp
                )
                Text(
                  text = "Exibir os nomes abaixo dos ícones na gaveta",
                  color = textSecondary,
                  fontSize = 13.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Switch(
                checked = showAppNames,
                onCheckedChange = onShowAppNamesChange,
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = accentColor,
                  uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                  uncheckedTrackColor = if (isDark) Color(0xFF2A2A2E) else Color(0xFFE5E5EA),
                  uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.88f)
              )
            }
          }
        }

        // --- 3. Privacidade e Segurança ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "PRIVACIDADE E SEGURANÇA",
            color = textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(sectionBgColor)
              .border(0.8.dp, cardBorderColor, RoundedCornerShape(16.dp))
          ) {
            // 3A. Bloqueio de Aplicativos
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (!SecurityAuthHelper.isDeviceSecure(context)) {
                    Toast.makeText(
                      context,
                      "Configure um bloqueio de tela no Android para proteger seus aplicativos.",
                      Toast.LENGTH_LONG
                    ).show()
                    SecurityAuthHelper.openSecuritySettings(context)
                  } else if (lockedAppKeys.isNotEmpty()) {
                    onRequestSecurityAuth(
                      "Bloqueio de Apps",
                      "Autentique-se para gerenciar os aplicativos protegidos",
                      { showAppLockDialog = true },
                      null
                    )
                  } else {
                    showAppLockDialog = true
                  }
                }
                .padding(horizontal = 18.dp, vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Bloqueio de aplicativos",
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = (-0.1).sp
                )
                Text(
                  text = "Proteger o acesso com biometria ou senha",
                  color = textSecondary,
                  fontSize = 13.sp
                )
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(0.5.dp)
                .background(dividerColor)
            )

            // 3B. Aplicativos Ocultos
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showHiddenAppsDialog = true }
                .padding(horizontal = 18.dp, vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Aplicativos ocultos",
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = (-0.1).sp
                )
                Text(
                  text = "Ocultar aplicativos da gaveta e da tela inicial",
                  color = textSecondary,
                  fontSize = 13.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    if (showAppLockDialog) {
      AppLockDialog(
        viewModel = viewModel,
        allApps = allApps,
        lockedAppKeys = lockedAppKeys,
        onRequestSecurityAuth = onRequestSecurityAuth,
        onSaveLockedApps = onSaveLockedApps,
        onDismiss = { showAppLockDialog = false }
      )
    }

    if (showHiddenAppsDialog) {
      HiddenAppsDialog(
        viewModel = viewModel,
        allApps = allApps,
        hiddenAppKeys = hiddenAppKeys,
        onToggleHideAppKey = onToggleHideAppKey,
        onDismiss = { showHiddenAppsDialog = false }
      )
    }
  }
}

@Composable
private fun HiddenAppsDialog(
  viewModel: LauncherViewModel,
  allApps: List<AppInfo>,
  hiddenAppKeys: Set<String>,
  onToggleHideAppKey: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = remember(allApps, searchQuery) {
    val query = AppInfo.normalize(searchQuery.trim())
    if (query.isEmpty()) {
      allApps
    } else {
      allApps.filter { app ->
        app.normalizedLabel.contains(query) || app.packageName.contains(query)
      }
    }
  }

  val isDark = isSystemInDarkTheme()
  val dialogBg = if (isDark) Color(0xFF121214) else Color(0xFFF5F6F8)
  val itemBg = if (isDark) Color(0xFF1C1C1F) else Color(0xFFFFFFFF)
  val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
  val dividerColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
  val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)
  val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
  val accentColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

  BackHandler {
    onDismiss()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(dialogBg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .pointerInput(Unit) { detectTapGestures { } }
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Aplicativos ocultos",
          color = textPrimary,
          fontSize = 22.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.3).sp
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF222226) else Color(0xFFEAECEF))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Voltar",
            color = textPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Search Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 6.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(itemBg)
          .border(0.8.dp, cardBorderColor, RoundedCornerShape(14.dp))
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(color = textPrimary, fontSize = 15.5.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
              if (searchQuery.isEmpty()) {
                Text("Buscar aplicativo...", color = textSecondary, fontSize = 15.5.sp)
              }
              innerTextField()
            }
          )
          if (searchQuery.isNotEmpty()) {
            Text(
              text = "Limpar",
              color = textSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.clickable { searchQuery = "" }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      val activeIconShape by viewModel.iconShape.collectAsStateWithLifecycle()
      val activeIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()

      // App List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 20.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(itemBg)
          .border(0.8.dp, cardBorderColor, RoundedCornerShape(16.dp)),
        contentPadding = PaddingValues(vertical = 6.dp)
      ) {
        items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
          val appKey = "${app.packageName}/${app.activityName}"
          val isHidden = hiddenAppKeys.contains(appKey) || hiddenAppKeys.contains(app.packageName)

          var iconBitmap by remember(app.packageName, app.activityName, activeIconShape, activeIconPack, app.lastUpdateTime) {
            mutableStateOf(viewModel.getAppIconFast(app.packageName, app.activityName))
          }
          LaunchedEffect(app.packageName, app.activityName, activeIconShape, activeIconPack, app.lastUpdateTime) {
            if (iconBitmap == null) {
              iconBitmap = viewModel.loadAppIconAsync(app.packageName, app.activityName)
            }
          }

          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleHideAppKey(appKey) }
                .padding(vertical = 11.dp, horizontal = 18.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (iconBitmap != null) {
                Image(
                  bitmap = iconBitmap!!,
                  contentDescription = null,
                  modifier = Modifier.size(40.dp)
                )
              } else {
                Box(modifier = Modifier.size(40.dp))
              }
              Spacer(modifier = Modifier.width(14.dp))
              Text(
                text = app.label,
                color = textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Switch(
                checked = isHidden,
                onCheckedChange = { onToggleHideAppKey(appKey) },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = accentColor,
                  uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                  uncheckedTrackColor = if (isDark) Color(0xFF2C2C30) else Color(0xFFE5E5EA),
                  uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.88f)
              )
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp, end = 18.dp)
                .height(0.5.dp)
                .background(dividerColor)
            )
          }
        }
      }
    }
  }
}
