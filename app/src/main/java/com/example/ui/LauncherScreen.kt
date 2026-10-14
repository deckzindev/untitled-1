package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.ui.components.AppContextMenu
import com.example.ui.components.AppDrawer
import com.example.ui.components.AppGridItem
import com.example.ui.components.AppMenuAnchor
import com.example.ui.components.HomeOptionsDialog
import com.example.ui.components.PersonalizationScreen
import com.example.ui.components.SettingsOverlay
import com.example.ui.performance.LauncherPerformanceTracker
import com.example.ui.performance.LocalLauncherPerformanceTracker
import com.example.ui.performance.TrackRecompositionScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private var cachedStatusBarService: Any? = null
private var cachedExpandNotificationsMethod: java.lang.reflect.Method? = null
private var cachedExpandSettingsMethod: java.lang.reflect.Method? = null

private fun expandNotificationShade(context: Context) {
  try {
    val service = cachedStatusBarService ?: context.applicationContext.getSystemService("statusbar")?.also {
      cachedStatusBarService = it
    } ?: return
    val expandNotifications = cachedExpandNotificationsMethod ?: Class.forName("android.app.StatusBarManager")
      .getMethod("expandNotificationsPanel")
      .also { cachedExpandNotificationsMethod = it }
    expandNotifications.invoke(service)
  } catch (_: Throwable) {
    try {
      val service = cachedStatusBarService ?: context.applicationContext.getSystemService("statusbar")?.also {
        cachedStatusBarService = it
      } ?: return
      val expandSettings = cachedExpandSettingsMethod ?: Class.forName("android.app.StatusBarManager")
        .getMethod("expandSettingsPanel")
        .also { cachedExpandSettingsMethod = it }
      expandSettings.invoke(service)
    } catch (_: Throwable) {}
  }
}

@Composable
fun LauncherScreen(
  viewModel: LauncherViewModel,
  onRequestSecurityAuth: (title: String, subtitle: String, onSuccess: () -> Unit, onError: ((String) -> Unit)?) -> Unit = { _, _, s, _ -> s() }
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val performanceTracker = remember { LauncherPerformanceTracker(isTrackingEnabled = false) }

  var isDrawerOpen by remember { mutableStateOf(false) }
  var showOptionsDialog by remember { mutableStateOf(false) }
  var showSettingsScreen by remember { mutableStateOf(false) }
  var showPersonalizationScreen by remember { mutableStateOf(false) }
  var isAppEditMode by remember { mutableStateOf(false) }
  var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
  var activeMenuApp by remember { mutableStateOf<AppInfo?>(null) }
  var selectedMenuAnchor by remember { mutableStateOf<AppMenuAnchor?>(null) }
  var activeMenuAnchor by remember { mutableStateOf<AppMenuAnchor?>(null) }
  var isMenuFromDrawer by remember { mutableStateOf(false) }

  val closeOverlaysTrigger by viewModel.closeOverlaysTrigger.collectAsStateWithLifecycle()

  val handleAppLaunch: (AppInfo) -> Unit = remember(viewModel, context, onRequestSecurityAuth) {
    { app ->
      if (viewModel.isAppLocked(app)) {
        onRequestSecurityAuth(
          "Abrir ${app.label}",
          "Autentique-se com sua biometria ou senha para abrir",
          {
            viewModel.launchApp(context, app)
            isDrawerOpen = false
            viewModel.setSearchQuery("")
          },
          null
        )
      } else {
        viewModel.launchApp(context, app)
        isDrawerOpen = false
        viewModel.setSearchQuery("")
      }
    }
  }

  LaunchedEffect(selectedAppForMenu, selectedMenuAnchor) {
    if (selectedAppForMenu != null) {
      activeMenuApp = selectedAppForMenu
      activeMenuAnchor = selectedMenuAnchor
    }
  }

  LaunchedEffect(closeOverlaysTrigger) {
    if (closeOverlaysTrigger > 0) {
      isDrawerOpen = false
      isAppEditMode = false
      showOptionsDialog = false
      showSettingsScreen = false
      showPersonalizationScreen = false
      selectedAppForMenu = null
      activeMenuApp = null
      selectedMenuAnchor = null
      activeMenuAnchor = null
    }
  }

  // Handle back button behavior gracefully based on open overlays
  if (selectedAppForMenu != null) {
    BackHandler {
      selectedAppForMenu = null
      selectedMenuAnchor = null
    }
  } else if (isDrawerOpen && isAppEditMode) {
    BackHandler {
      isAppEditMode = false
    }
  } else if (isDrawerOpen) {
    BackHandler {
      isDrawerOpen = false
      isAppEditMode = false
      viewModel.setSearchQuery("")
    }
  } else if (showSettingsScreen) {
    BackHandler {
      showSettingsScreen = false
    }
  } else if (showPersonalizationScreen) {
    BackHandler {
      showPersonalizationScreen = false
    }
  } else if (showOptionsDialog) {
    BackHandler {
      showOptionsDialog = false
    }
  }

  CompositionLocalProvider(LocalLauncherPerformanceTracker provides performanceTracker) {
    TrackRecompositionScope("LauncherScreenRoot")

    BoxWithConstraints(
      modifier = Modifier.fillMaxSize()
    ) {
      val rawHeight = constraints.maxHeight.toFloat()
      val screenHeightPx = if (rawHeight.isFinite() && rawHeight > 100f) rawHeight else 2000f
      val scope = rememberCoroutineScope()
      val drawerOffset = remember { Animatable(screenHeightPx) }
      var hasInitializedHeight by remember { mutableStateOf(false) }

      val drawerSpringSpec = remember {
        spring<Float>(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = 3200f
        )
      }

      LaunchedEffect(screenHeightPx) {
        if (screenHeightPx > 0f) {
          if (!isDrawerOpen) {
            drawerOffset.snapTo(screenHeightPx)
          } else {
            drawerOffset.snapTo(0f)
          }
          hasInitializedHeight = true
        }
      }

      LaunchedEffect(isDrawerOpen, screenHeightPx) {
        if (hasInitializedHeight) {
          if (isDrawerOpen) {
            if (drawerOffset.value > 0f) {
              drawerOffset.animateTo(0f, drawerSpringSpec)
            }
          } else {
            if (drawerOffset.value < screenHeightPx) {
              drawerOffset.animateTo(screenHeightPx, drawerSpringSpec)
            }
          }
        }
      }

      val isMenuOpen = selectedAppForMenu != null
      val menuBlurDp by animateDpAsState(
        targetValue = if (isMenuOpen) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 160),
        label = "contextMenuBlurDp"
      )
      val menuBlurProgress by animateFloatAsState(
        targetValue = if (isMenuOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "contextMenuBlurProgress"
      )

      // 0. Isolated Ambient Backdrop Layer
      val isOverlayCoveringHome = isDrawerOpen || showSettingsScreen || showPersonalizationScreen
      LauncherBackdropLayer(
        viewModel = viewModel,
        isDark = isDark,
        screenHeightPx = screenHeightPx,
        drawerOffset = drawerOffset,
        menuBlurProgress = menuBlurProgress,
        isOverlayCoveringHome = isOverlayCoveringHome
      )

      val workspaceBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && menuBlurDp > 0.5.dp) {
        Modifier.blur(radius = menuBlurDp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
      } else {
        Modifier
      }

      // Container wrapping Home workspace and AppDrawer so both blur while the pressed app stays sharp in AppContextMenu
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(workspaceBlurModifier)
      ) {
        // 1. Main home screen workspace with vertical drag and long press listeners
        Box(
          modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .pointerInput(screenHeightPx) {
              val velocityTracker = VelocityTracker()
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                val initialOffset = drawerOffset.value
                val initialY = down.position.y
                var isVerticalDrag = false
                var totalDragY = 0f

                // Stop any ongoing drawer animation instantly on touch down
                scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { drawerOffset.stop() }

                val pointerId = down.id
                var isLongPressHandled = false

                val longPressJob = scope.launch {
                  kotlinx.coroutines.delay(400L)
                  if (!isVerticalDrag && !isLongPressHandled) {
                    isLongPressHandled = true
                    HapticFeedbackHelper.performTick(context)
                    showOptionsDialog = true
                  }
                }

                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                  if (!change.pressed) break

                  velocityTracker.addPosition(change.uptimeMillis, change.position)
                  val dragAmount = change.positionChange().y
                  val totalDeltaY = change.position.y - initialY

                  if (!isVerticalDrag && abs(totalDeltaY) > 8f) {
                    isVerticalDrag = true
                    performanceTracker.setScrollOrGestureActive(true)
                    longPressJob.cancel()
                  }

                  if (isVerticalDrag) {
                    change.consume()
                    totalDragY += dragAmount
                    val newOffset = (initialOffset + totalDragY).coerceIn(0f, screenHeightPx)
                    scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                      drawerOffset.snapTo(newOffset)
                    }
                  }
                }

                longPressJob.cancel()
                performanceTracker.setScrollOrGestureActive(false)

                val velocityY = velocityTracker.calculateVelocity().y

                if (isVerticalDrag && !isLongPressHandled) {
                  scope.launch {
                    if (velocityY < -120f || totalDragY < -30f || drawerOffset.value < screenHeightPx * 0.80f) {
                      if (!isDrawerOpen) HapticFeedbackHelper.performTick(context)
                      isDrawerOpen = true
                      drawerOffset.animateTo(0f, drawerSpringSpec, initialVelocity = velocityY)
                    } else if (initialOffset >= screenHeightPx * 0.90f && (velocityY > 120f || totalDragY > 30f)) {
                      isDrawerOpen = false
                      HapticFeedbackHelper.performTick(context)
                      drawerOffset.animateTo(screenHeightPx, drawerSpringSpec)
                      expandNotificationShade(context)
                    } else {
                      if (isDrawerOpen) HapticFeedbackHelper.performTick(context)
                      isDrawerOpen = false
                      drawerOffset.animateTo(screenHeightPx, drawerSpringSpec, initialVelocity = velocityY)
                    }
                  }
                }
              }
            }
        ) {
          // Home workspace & dock with GPU-accelerated graphics layer and isolated state scope
          Box(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                val progress = if (screenHeightPx > 0f) {
                  (1f - (drawerOffset.value / screenHeightPx)).coerceIn(0f, 1f)
                } else 0f
                alpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
                val scale = (1f - progress * 0.06f).coerceIn(0.92f, 1f)
                scaleX = scale
                scaleY = scale
              }
          ) {
            HomeDockScope(
              viewModel = viewModel,
              isMenuFromDrawer = isMenuFromDrawer,
              selectedAppForMenu = selectedAppForMenu,
              onAppLaunch = handleAppLaunch,
              onSelectMenuWithAnchor = { app, anchor ->
                HapticFeedbackHelper.performTick(context)
                isMenuFromDrawer = false
                selectedMenuAnchor = anchor
                selectedAppForMenu = app
              },
              onSelectMenuWithoutAnchor = { app ->
                if (selectedAppForMenu == null) {
                  HapticFeedbackHelper.performTick(context)
                  isMenuFromDrawer = false
                  selectedAppForMenu = app
                }
              }
            )
          }
        }

        // 2. Application Drawer Layer isolated in its own recomposition scope
        AppDrawerScope(
          viewModel = viewModel,
          drawerOffset = drawerOffset,
          screenHeightPx = screenHeightPx,
          isDrawerOpen = isDrawerOpen,
          isAppEditMode = isAppEditMode,
          highlightedApp = if (isMenuFromDrawer && !isAppEditMode) selectedAppForMenu else null,
          onExitEditMode = { isAppEditMode = false },
          onAppLongClickWithAnchor = { anchor ->
            if (!isAppEditMode) {
              HapticFeedbackHelper.performTick(context)
              isMenuFromDrawer = true
              selectedMenuAnchor = anchor
              selectedAppForMenu = anchor.app
            }
          },
          onDrawerOpenChange = { open ->
            isDrawerOpen = open
            if (!open) {
              isAppEditMode = false
              viewModel.setSearchQuery("")
            }
          },
          onAppClick = { app ->
            if (!isAppEditMode) {
              handleAppLaunch(app)
            }
          },
          onAppLongClick = { app ->
            if (!isAppEditMode && selectedAppForMenu == null) {
              HapticFeedbackHelper.performTick(context)
              isMenuFromDrawer = true
              selectedAppForMenu = app
            }
          }
        )
      }

      // 3. Options Popup Dialog (Home screen long press)
      AnimatedVisibility(
        visible = showOptionsDialog,
        enter = fadeIn(animationSpec = tween(160)) +
                scaleIn(
                  animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                  ),
                  initialScale = 0.92f
                ),
        exit = fadeOut(animationSpec = tween(130)) +
               scaleOut(
                 animationSpec = tween(130),
                 targetScale = 0.94f
               )
      ) {
        HomeOptionsDialog(
          onDismiss = { showOptionsDialog = false },
          onLaunchWallpaperPicker = { showPersonalizationScreen = true },
          onOpenSettings = { showSettingsScreen = true }
        )
      }

      // 4. Settings Screen Overlay (only collects allApps/iconPacks when visible)
      AnimatedVisibility(
        visible = showSettingsScreen,
        enter = fadeIn(animationSpec = tween(190)) +
                slideInVertically(
                  animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                  ),
                  initialOffsetY = { it / 7 }
                ) + scaleIn(
                  animationSpec = tween(190),
                  initialScale = 0.97f
                ),
        exit = fadeOut(animationSpec = tween(150)) +
               slideOutVertically(
                 animationSpec = tween(150),
                 targetOffsetY = { it / 8 }
               ) + scaleOut(
                 animationSpec = tween(150),
                 targetScale = 0.97f
               )
      ) {
        SettingsOverlayScope(
          viewModel = viewModel,
          onRequestSecurityAuth = onRequestSecurityAuth,
          onEnterAppEditMode = {
            HapticFeedbackHelper.performTick(context)
            showSettingsScreen = false
            selectedAppForMenu = null
            selectedMenuAnchor = null
            viewModel.setSearchQuery("")
            isAppEditMode = true
            isDrawerOpen = true
          },
          onClose = { showSettingsScreen = false }
        )
      }

      // 5. Personalization Screen Overlay
      AnimatedVisibility(
        visible = showPersonalizationScreen,
        enter = fadeIn(animationSpec = tween(190)) +
                slideInVertically(
                  animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                  ),
                  initialOffsetY = { it / 7 }
                ) + scaleIn(
                  animationSpec = tween(190),
                  initialScale = 0.97f
                ),
        exit = fadeOut(animationSpec = tween(150)) +
               slideOutVertically(
                 animationSpec = tween(150),
                 targetOffsetY = { it / 8 }
               ) + scaleOut(
                 animationSpec = tween(150),
                 targetScale = 0.97f
               )
      ) {
        PersonalizationScreen(
          viewModel = viewModel,
          onClose = { showPersonalizationScreen = false }
        )
      }

      // 6. App Context Menu (Long press on any app icon)
      AnimatedVisibility(
        visible = selectedAppForMenu != null,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(130))
      ) {
        val app = activeMenuApp ?: selectedAppForMenu
        val anchor = selectedMenuAnchor ?: activeMenuAnchor
        if (app != null) {
          ContextMenuScope(
            app = app,
            anchor = anchor,
            viewModel = viewModel,
            onDismiss = {
              selectedAppForMenu = null
              selectedMenuAnchor = null
            }
          )
        }
      }
    }
  }
}

@Composable
private fun LauncherBackdropLayer(
  viewModel: LauncherViewModel,
  isDark: Boolean,
  screenHeightPx: Float,
  drawerOffset: Animatable<Float, *>,
  menuBlurProgress: Float,
  isOverlayCoveringHome: Boolean
) {
  TrackRecompositionScope("LauncherBackdropLayer")
  val context = LocalContext.current
  val blurredWallpaper by viewModel.blurredWallpaper.collectAsStateWithLifecycle()
  val isVideoWallpaperActive by viewModel.isVideoWallpaperActive.collectAsStateWithLifecycle()
  val videoWallpaperVersion by viewModel.videoWallpaperVersion.collectAsStateWithLifecycle()
  val isSystemLiveWallpaperActive by viewModel.isSystemLiveWallpaperServiceActive.collectAsStateWithLifecycle()

  val backdropTint = if (isDark) Color(0xFF0A0B0F) else Color(0xFFF4F6FA)
  val activeBlurredBitmap = if (isDark) blurredWallpaper?.darkModeBlur else blurredWallpaper?.lightModeBlur

  Box(modifier = Modifier.fillMaxSize()) {
    // Direct standalone hardware video wallpaper renderer when a video is active
    // and the device's system LiveWallpaperService isn't already rendering it behind our window
    if (isVideoWallpaperActive && !isSystemLiveWallpaperActive) {
      val videoFile = remember(videoWallpaperVersion) {
        com.example.wallpaper.VideoLiveWallpaperService.getVideoFile(context)
      }
      val isDrawerMostlyClosed by remember(screenHeightPx) {
        androidx.compose.runtime.derivedStateOf {
          drawerOffset.value > screenHeightPx * 0.05f
        }
      }
      val isHomeVisible = !isOverlayCoveringHome && isDrawerMostlyClosed
      com.example.wallpaper.DirectVideoWallpaperSurface(
        videoFile = videoFile,
        videoVersion = videoWallpaperVersion,
        shouldPlay = isHomeVisible,
        modifier = Modifier.fillMaxSize()
      )
    }

    if (activeBlurredBitmap != null && menuBlurProgress > 0.01f) {
      Image(
        bitmap = activeBlurredBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            val drawerProgress = if (screenHeightPx > 0f) {
              (1f - (drawerOffset.value / screenHeightPx)).coerceIn(0f, 1f)
            } else 0f
            alpha = (menuBlurProgress * (1f - drawerProgress)).coerceIn(0f, 1f)
          }
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          val drawerProgress = if (screenHeightPx > 0f) {
            (1f - (drawerOffset.value / screenHeightPx)).coerceIn(0f, 1f)
          } else 0f
          alpha = drawerProgress * 0.14f
        }
        .background(backdropTint)
    )
  }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HomeDockScope(
  viewModel: LauncherViewModel,
  isMenuFromDrawer: Boolean,
  selectedAppForMenu: AppInfo?,
  onAppLaunch: (AppInfo) -> Unit,
  onSelectMenuWithAnchor: (AppInfo, AppMenuAnchor) -> Unit,
  onSelectMenuWithoutAnchor: (AppInfo) -> Unit
) {
  TrackRecompositionScope("HomeDockScope")
  val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()
  val iconShape by viewModel.iconShape.collectAsStateWithLifecycle()
  val selectedIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()
  val iconSizeProgress by viewModel.iconSizeProgress.collectAsStateWithLifecycle()

  if (dockApps.isNotEmpty()) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      val drawerColumns = 4
      val itemSpacing = 4.dp
      val drawerItemWidth = (maxWidth - (itemSpacing * (drawerColumns - 1))) / drawerColumns
      val maxDockIconDpAt120 = (drawerItemWidth - 2.dp).coerceIn(54.dp, 92.dp)
      val minDockIconDpAt50 = (maxDockIconDpAt50Calc(maxDockIconDpAt120))
      val clampedProgress = iconSizeProgress.coerceIn(0.50f, 1.20f)
      val normalizedScale = (clampedProgress - 0.50f) / 0.70f
      val dockIconSize = (minDockIconDpAt50 + (maxDockIconDpAt120 - minDockIconDpAt50) * normalizedScale)
        .coerceIn(minDockIconDpAt50, maxDockIconDpAt120)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
      ) {
        dockApps.forEach { app ->
          key("${app.packageName}/${app.activityName}") {
            Box(
              modifier = Modifier
                .width(drawerItemWidth)
                .height(drawerItemWidth),
              contentAlignment = Alignment.Center
            ) {
              val isDockAppHighlighted = !isMenuFromDrawer &&
                selectedAppForMenu != null &&
                selectedAppForMenu.packageName == app.packageName &&
                selectedAppForMenu.activityName == app.activityName
              AppGridItem(
                app = app,
                showLabel = false,
                iconSize = dockIconSize,
                viewModel = viewModel,
                iconShape = iconShape,
                selectedIconPack = selectedIconPack,
                isMenuHighlighted = isDockAppHighlighted,
                onLongClickWithAnchor = { anchor ->
                  onSelectMenuWithAnchor(app, anchor)
                },
                onClick = {
                  onAppLaunch(app)
                },
                onLongClick = {
                  onSelectMenuWithoutAnchor(app)
                }
              )
            }
          }
        }
      }
    }
  }
}

private fun maxDockIconDpAt50Calc(maxDockIconDpAt120: Dp): Dp =
  (maxDockIconDpAt120 * 0.485f).coerceIn(36.dp, 45.dp)

@Composable
private fun AppDrawerScope(
  viewModel: LauncherViewModel,
  drawerOffset: Animatable<Float, *>,
  screenHeightPx: Float,
  isDrawerOpen: Boolean,
  isAppEditMode: Boolean,
  highlightedApp: AppInfo?,
  onExitEditMode: () -> Unit,
  onAppLongClickWithAnchor: (AppMenuAnchor) -> Unit,
  onDrawerOpenChange: (Boolean) -> Unit,
  onAppClick: (AppInfo) -> Unit,
  onAppLongClick: (AppInfo) -> Unit
) {
  TrackRecompositionScope("AppDrawerScope")
  val performanceTracker = LocalLauncherPerformanceTracker.current
  val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val showAppNames by viewModel.showAppNames.collectAsStateWithLifecycle()
  val iconShape by viewModel.iconShape.collectAsStateWithLifecycle()
  val selectedIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()
  val iconSizeProgress by viewModel.iconSizeProgress.collectAsStateWithLifecycle()
  val drawerStyle by viewModel.drawerStyle.collectAsStateWithLifecycle()

  LaunchedEffect(isLoading) {
    performanceTracker.setHeavyActivityLoading(isLoading)
  }

  AppDrawer(
    viewModel = viewModel,
    apps = apps,
    searchQuery = searchQuery,
    isLoading = isLoading,
    showAppNames = showAppNames,
    iconSizeProgress = iconSizeProgress,
    drawerOffset = drawerOffset,
    screenHeightPx = screenHeightPx,
    isDrawerOpen = isDrawerOpen,
    iconShape = iconShape,
    selectedIconPack = selectedIconPack,
    drawerStyle = drawerStyle,
    highlightedApp = highlightedApp,
    isAppEditMode = isAppEditMode,
    onExitEditMode = onExitEditMode,
    onAppLongClickWithAnchor = onAppLongClickWithAnchor,
    onDrawerOpenChange = onDrawerOpenChange,
    onSearchQueryChange = { viewModel.setSearchQuery(it) },
    onAppClick = onAppClick,
    onAppLongClick = onAppLongClick,
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer {
        translationY = drawerOffset.value
      }
      .testTag("app_drawer")
  )
}

@Composable
private fun SettingsOverlayScope(
  viewModel: LauncherViewModel,
  onRequestSecurityAuth: (title: String, subtitle: String, onSuccess: () -> Unit, onError: ((String) -> Unit)?) -> Unit,
  onEnterAppEditMode: () -> Unit,
  onClose: () -> Unit
) {
  TrackRecompositionScope("SettingsOverlayScope")
  val showAppNames by viewModel.showAppNames.collectAsStateWithLifecycle()
  val iconSizeProgress by viewModel.iconSizeProgress.collectAsStateWithLifecycle()
  val drawerStyle by viewModel.drawerStyle.collectAsStateWithLifecycle()
  val allApps by viewModel.allApps.collectAsStateWithLifecycle()
  val hiddenAppKeys by viewModel.hiddenAppKeys.collectAsStateWithLifecycle()
  val lockedAppKeys by viewModel.lockedAppKeys.collectAsStateWithLifecycle()

  SettingsOverlay(
    viewModel = viewModel,
    showAppNames = showAppNames,
    onShowAppNamesChange = { viewModel.setShowAppNames(it) },
    currentIconSizeProgress = iconSizeProgress,
    onIconSizeProgressChange = { viewModel.setIconSizeProgress(it) },
    allApps = allApps,
    hiddenAppKeys = hiddenAppKeys,
    onToggleHideAppKey = { viewModel.toggleHideAppKey(it) },
    lockedAppKeys = lockedAppKeys,
    onSaveLockedApps = { viewModel.setLockedAppKeys(it) },
    onRequestSecurityAuth = onRequestSecurityAuth,
    currentDrawerStyle = drawerStyle,
    onDrawerStyleChange = { viewModel.setDrawerStyle(it) },
    onEnterAppEditMode = onEnterAppEditMode,
    onClose = onClose
  )
}

@Composable
private fun ContextMenuScope(
  app: AppInfo,
  anchor: AppMenuAnchor?,
  viewModel: LauncherViewModel,
  onDismiss: () -> Unit
) {
  TrackRecompositionScope("ContextMenuScope")
  val context = LocalContext.current
  val iconShape by viewModel.iconShape.collectAsStateWithLifecycle()
  val selectedIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()
  val dockAppKeys by viewModel.dockAppKeys.collectAsStateWithLifecycle()
  val appKey = "${app.packageName}/${app.activityName}"
  val isInDock = dockAppKeys.contains(appKey) || dockAppKeys.contains(app.packageName)

  AppContextMenu(
    app = app,
    isInDock = isInDock,
    viewModel = viewModel,
    iconShape = iconShape,
    selectedIconPack = selectedIconPack,
    anchor = anchor,
    onDismiss = onDismiss,
    onAppInfoClick = {
      try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.parse("package:${app.packageName}")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      } catch (_: Exception) {}
    },
    onToggleDockClick = {
      val added = viewModel.toggleDockApp(app)
      if (!added) {
        Toast.makeText(context, "Limite de 4 aplicativos na dock atingido", Toast.LENGTH_SHORT).show()
      }
    },
    onHideClick = {
      viewModel.toggleHideApp(app)
      Toast.makeText(context, "${app.label} ocultado", Toast.LENGTH_SHORT).show()
    },
    onUninstallClick = {
      val packageUri = Uri.fromParts("package", app.packageName, null)
      try {
        val intent = Intent(Intent.ACTION_DELETE, packageUri).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
      } catch (e: Throwable) {
        try {
          @Suppress("DEPRECATION")
          val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }
          context.startActivity(intent)
        } catch (ex: Throwable) {
          try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
          } catch (ex2: Throwable) {
            Toast.makeText(context, "Não foi possível desinstalar este aplicativo", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  )
}
