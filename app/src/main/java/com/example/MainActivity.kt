package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LogsViewerDialog
import com.example.ui.screens.BuildScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BuilderViewModel

enum class AppScreen(
  val titleRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  BUILD(R.string.nav_build, Icons.Filled.Build, Icons.Outlined.Build),
  HISTORY(R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
  SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

  private val viewModel: BuilderViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        var currentScreen by rememberSaveable { mutableStateOf(AppScreen.BUILD) }
        val authState by viewModel.authUiState.collectAsState()
        val logsDialogState by viewModel.logsDialogState.collectAsState()

        BackHandler(enabled = currentScreen != AppScreen.BUILD) {
          currentScreen = AppScreen.BUILD
        }

        LogsViewerDialog(
          state = logsDialogState,
          onDismiss = { viewModel.dismissLogsDialog() }
        )

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = DarkSurface,
          topBar = {
            CenterAlignedTopAppBar(
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = DarkSurface,
                titleContentColor = TextPrimary
              ),
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(AccentBlueBright),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.CloudUpload,
                      contentDescription = null,
                      tint = DarkSurface,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                  )
                }
              },
              actions = {
                // User badge or connection status
                if (authState.user != null) {
                  Row(
                    modifier = Modifier
                      .padding(end = 12.dp)
                      .clip(RoundedCornerShape(16.dp))
                      .background(DarkSurfaceVariant)
                      .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                      .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreenBright)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "@${authState.user?.login}",
                      fontSize = 11.sp,
                      color = TextPrimary,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = DarkSurface,
              tonalElevation = 0.dp
            ) {
              AppScreen.entries.forEach { screen ->
                val selected = currentScreen == screen
                NavigationBarItem(
                  selected = selected,
                  onClick = { currentScreen = screen },
                  icon = {
                    Icon(
                      imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                      contentDescription = stringResource(screen.titleRes)
                    )
                  },
                  label = {
                    Text(
                      text = stringResource(screen.titleRes),
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkSurface,
                    indicatorColor = AccentGreenBright,
                    selectedTextColor = AccentGreenBright,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextMuted
                  ),
                  modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            AnimatedContent(
              targetState = currentScreen,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "screen_transition"
            ) { screen ->
              when (screen) {
                AppScreen.BUILD -> BuildScreen(
                  viewModel = viewModel,
                  onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                  onNavigateToHistory = { currentScreen = AppScreen.HISTORY }
                )
                AppScreen.HISTORY -> HistoryScreen(
                  viewModel = viewModel,
                  onNavigateToBuild = { currentScreen = AppScreen.BUILD }
                )
                AppScreen.SETTINGS -> SettingsScreen(
                  viewModel = viewModel
                )
              }
            }
          }
        }
      }
    }
  }
}
