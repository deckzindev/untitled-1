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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.security.SecurityAuthHelper
import com.example.ui.HapticFeedbackHelper
import com.example.ui.icons.SvgLockIcon
import com.example.ui.icons.SvgSearchIcon

@Composable
fun AppLockDialog(
    viewModel: LauncherViewModel,
    allApps: List<AppInfo>,
    lockedAppKeys: Set<String>,
    onRequestSecurityAuth: (title: String, subtitle: String, onSuccess: () -> Unit, onError: ((String) -> Unit)?) -> Unit,
    onSaveLockedApps: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedKeys by remember(lockedAppKeys) { mutableStateOf(lockedAppKeys.toSet()) }
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
    val blurredWallpaper by viewModel.blurredWallpaper.collectAsStateWithLifecycle()

    val drawerScrimBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0C10).copy(alpha = 0.76f),
                    Color(0xFF0E1017).copy(alpha = 0.82f),
                    Color(0xFF090A0E).copy(alpha = 0.88f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8F9FD).copy(alpha = 0.78f),
                    Color(0xFFF3F5FA).copy(alpha = 0.84f),
                    Color(0xFFECF0F7).copy(alpha = 0.90f)
                )
            )
        }
    }

    val cardBgColor = if (isDark) Color(0xFF16171E).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f)
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    val textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val accentColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val activeIconShape by viewModel.iconShape.collectAsStateWithLifecycle()
    val activeIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val activeBlurredBitmap = if (isDark) blurredWallpaper?.darkModeBlur else blurredWallpaper?.lightModeBlur
        if (activeBlurredBitmap != null) {
            Image(
                bitmap = activeBlurredBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(drawerScrimBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Bloqueio de aplicativos",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = if (selectedKeys.isEmpty()) "Nenhum aplicativo protegido" else "${selectedKeys.size} protegido(s) por biometria",
                        color = if (selectedKeys.isEmpty()) textSecondary else accentColor,
                        fontSize = 12.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                        .border(0.8.dp, cardBorderColor, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        color = textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBgColor)
                    .border(0.8.dp, cardBorderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = SvgSearchIcon,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = textPrimary, fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar aplicativo...",
                                    color = textSecondary,
                                    fontSize = 15.sp
                                )
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

            Spacer(modifier = Modifier.height(10.dp))

            // App List Card
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBgColor)
                    .border(0.8.dp, cardBorderColor, RoundedCornerShape(20.dp)),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = filtered,
                    key = { "${it.packageName}/${it.activityName}" }
                ) { app ->
                    val appKey = "${app.packageName}/${app.activityName}"
                    val isLocked = selectedKeys.contains(appKey) || selectedKeys.contains(app.packageName)

                    var iconBitmap by remember(app.packageName, app.activityName, activeIconShape, activeIconPack, app.lastUpdateTime) {
                        mutableStateOf(viewModel.getAppIconFast(app.packageName, app.activityName))
                    }
                    if (iconBitmap == null) {
                        LaunchedEffect(app.packageName, app.activityName, activeIconShape, activeIconPack, app.lastUpdateTime) {
                            iconBitmap = viewModel.loadAppIconAsync(app.packageName, app.activityName)
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticFeedbackHelper.performTick(context, 10L)
                                    selectedKeys = if (isLocked) {
                                        selectedKeys - appKey - app.packageName
                                    } else {
                                        selectedKeys + appKey
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap!!,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = app.label,
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = isLocked,
                                onCheckedChange = { checked ->
                                    HapticFeedbackHelper.performTick(context, 10L)
                                    selectedKeys = if (checked) {
                                        selectedKeys + appKey
                                    } else {
                                        selectedKeys - appKey - app.packageName
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                                    uncheckedTrackColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFE5E7EB),
                                    uncheckedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 68.dp, end = 16.dp)
                                .height(0.5.dp)
                                .background(dividerColor)
                        )
                    }
                }
            }

            // Bottom Save Action Bar
            val hasChanges = selectedKeys != lockedAppKeys
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        HapticFeedbackHelper.performTick(context, 10L)
                        if (!hasChanges) {
                            onDismiss()
                            return@Button
                        }

                        if (!SecurityAuthHelper.isDeviceSecure(context)) {
                            Toast.makeText(
                                context,
                                "Configure um bloqueio de tela no Android para proteger seus aplicativos.",
                                Toast.LENGTH_LONG
                            ).show()
                            SecurityAuthHelper.openSecuritySettings(context)
                            return@Button
                        }

                        onRequestSecurityAuth(
                            "Confirmar Bloqueio",
                            "Autentique-se para salvar as configurações de bloqueio",
                            {
                                onSaveLockedApps(selectedKeys)
                                Toast.makeText(
                                    context,
                                    "Bloqueio de aplicativos atualizado com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            },
                            { errorMsg ->
                                Toast.makeText(
                                    context,
                                    "Autenticação necessária para alterar: $errorMsg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasChanges) accentColor else (if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                        contentColor = if (hasChanges) Color.White else textPrimary
                    )
                ) {
                    Icon(
                        imageVector = SvgLockIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasChanges) "Salvar alterações (${selectedKeys.size})" else "Salvar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
