package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LauncherViewModel
import com.example.model.AppInfo
import com.example.ui.HapticFeedbackHelper
import com.example.ui.icons.SvgCloseIcon
import com.example.ui.icons.SvgEmptyAppsIcon
import com.example.ui.icons.SvgSearchIcon
import com.example.ui.performance.LocalLauncherPerformanceTracker
import com.example.ui.performance.TrackRecompositionScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AppDrawer(
  viewModel: LauncherViewModel,
  apps: List<AppInfo>,
  searchQuery: String,
  isLoading: Boolean,
  showAppNames: Boolean,
  iconSizeProgress: Float,
  drawerOffset: Animatable<Float, *>,
  screenHeightPx: Float,
  isDrawerOpen: Boolean,
  onDrawerOpenChange: (Boolean) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onAppClick: (AppInfo) -> Unit,
  onAppLongClick: (AppInfo) -> Unit,
  modifier: Modifier = Modifier,
  iconShape: String = "sammy",
  selectedIconPack: String? = null,
  drawerStyle: String = "vertical",
  highlightedApp: AppInfo? = null,
  onAppLongClickWithAnchor: ((AppMenuAnchor) -> Unit)? = null,
  isAppEditMode: Boolean = false,
  onExitEditMode: () -> Unit = {}
) {
  val rootContext = LocalContext.current
  val performanceTracker = LocalLauncherPerformanceTracker.current
  TrackRecompositionScope("AppDrawerContent")
  var onCommitEditModeChanges by remember { mutableStateOf<(() -> Unit)?>(null) }
  val verticalScrollState = rememberScrollState()
  val isAtTop = remember(drawerStyle) {
    derivedStateOf {
      if (drawerStyle == "vertical") {
        verticalScrollState.value == 0
      } else {
        true
      }
    }
  }

  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val isImeVisible = WindowInsets.ime.getBottom(density) > 0

  var isSearchFocused by remember { mutableStateOf(false) }

  // Clear focus when drawer opens or closes to prevent cursor blinking when browsing
  LaunchedEffect(isDrawerOpen) {
    focusManager.clearFocus(force = true)
    keyboardController?.hide()
    isSearchFocused = false
  }

  // Clear focus on first mount
  LaunchedEffect(Unit) {
    focusManager.clearFocus(force = true)
    keyboardController?.hide()
    isSearchFocused = false
  }

  // If user dismisses software keyboard, immediately clear search focus so cursor stops blinking
  LaunchedEffect(isImeVisible) {
    if (!isImeVisible && isSearchFocused) {
      focusManager.clearFocus(force = true)
      isSearchFocused = false
    }
  }

  // Clear focus when user scrolls the app grid and notify performance tracker
  LaunchedEffect(verticalScrollState.isScrollInProgress) {
    val scrolling = verticalScrollState.isScrollInProgress
    performanceTracker.setScrollOrGestureActive(scrolling)
    if (scrolling && isSearchFocused) {
      focusManager.clearFocus(force = true)
      keyboardController?.hide()
      isSearchFocused = false
    }
  }

  // Handle system back when search bar is focused
  if (isSearchFocused) {
    BackHandler {
      focusManager.clearFocus(force = true)
      keyboardController?.hide()
      isSearchFocused = false
    }
  }

  val drawerSpringSpec = remember {
    spring<Float>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = 3200f
    )
  }

  val nestedScrollConnection = remember(screenHeightPx, isSearchFocused, isAppEditMode) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (isAppEditMode) return Offset.Zero
        if (source == NestedScrollSource.UserInput && available.y != 0f && isSearchFocused) {
          focusManager.clearFocus(force = true)
          keyboardController?.hide()
          isSearchFocused = false
        }
        // If drawer is partially pulled down (drawerOffset > 0) and user drags back UP
        if (source == NestedScrollSource.UserInput && available.y < 0f && drawerOffset.value > 0f) {
          val newOffset = (drawerOffset.value + available.y).coerceIn(0f, screenHeightPx)
          scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            drawerOffset.snapTo(newOffset)
          }
          return Offset(0f, available.y)
        }
        return Offset.Zero
      }

      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
      ): Offset {
        if (isAppEditMode) return Offset.Zero
        // If user drags DOWN when list is at top
        if (source == NestedScrollSource.UserInput && available.y > 0f && isAtTop.value) {
          val newOffset = (drawerOffset.value + available.y).coerceIn(0f, screenHeightPx)
          scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            drawerOffset.snapTo(newOffset)
          }
          return Offset(0f, available.y)
        }
        return Offset.Zero
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
        if (drawerOffset.value > 0f) {
          val velocityY = available.y
          val isFlingDown = velocityY > 120f
          val isFlingUp = velocityY < -120f
          val isPastThreshold = drawerOffset.value > screenHeightPx * 0.15f

          val shouldClose = isFlingDown || (!isFlingUp && isPastThreshold)

          if (shouldClose) {
            onDrawerOpenChange(false)
            try {
              drawerOffset.animateTo(
                targetValue = screenHeightPx,
                animationSpec = drawerSpringSpec,
                initialVelocity = velocityY
              )
            } catch (_: Throwable) {}
          } else {
            onDrawerOpenChange(true)
            try {
              drawerOffset.animateTo(
                targetValue = 0f,
                animationSpec = drawerSpringSpec,
                initialVelocity = velocityY
              )
            } catch (_: Throwable) {}
          }
          return available
        }
        return Velocity.Zero
      }
    }
  }

  val isDark = isSystemInDarkTheme()
  val blurredWallpaper by viewModel.blurredWallpaper.collectAsStateWithLifecycle()

  // Translucent gradient scrim that adapts automatically to Dark and Light mode
  val drawerScrimBrush = remember(isDark) {
    if (isDark) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFF0B0C10).copy(alpha = 0.62f),
          Color(0xFF0D0E14).copy(alpha = 0.68f),
          Color(0xFF090A0E).copy(alpha = 0.74f)
        )
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFFF8F9FC).copy(alpha = 0.66f),
          Color(0xFFF3F5FA).copy(alpha = 0.72f),
          Color(0xFFEDF0F7).copy(alpha = 0.78f)
        )
      )
    }
  }

  val searchBg = if (isDark) Color(0xFF1C1D24).copy(alpha = 0.68f) else Color(0xFFFFFFFF).copy(alpha = 0.78f)
  val searchBgFocused = if (isDark) Color(0xFF22242C).copy(alpha = 0.82f) else Color(0xFFFFFFFF).copy(alpha = 0.92f)
  val searchBorderUnfocused = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
  val searchBorderFocused = if (isDark) Color(0xFF0A84FF).copy(alpha = 0.55f) else Color(0xFF007AFF).copy(alpha = 0.45f)
  val textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF141416)
  val textSecondary = if (isDark) Color.White.copy(alpha = 0.64f) else Color(0xFF3C3C43).copy(alpha = 0.68f)
  val accentColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

  Box(
    modifier = modifier.clipToBounds()
  ) {
    // 1. Blurred current wallpaper layer locked to screen coordinates for seamless continuity (already pre-blurred, zero runtime GPU blur overhead)
    val activeBlurredBitmap = if (isDark) blurredWallpaper?.darkModeBlur else blurredWallpaper?.lightModeBlur
    if (activeBlurredBitmap != null) {
      Image(
        bitmap = activeBlurredBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            translationY = -drawerOffset.value
          }
      )
    }

    // 2. Translucent dark/light layer over the entire drawer area
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
        .nestedScroll(nestedScrollConnection)
        .padding(horizontal = 10.dp)
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = {
              focusManager.clearFocus(force = true)
              keyboardController?.hide()
              isSearchFocused = false
            }
          )
        },
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      // Fluid animated header switching seamlessly between Search Bar and Edit Mode Bar
      AnimatedContent(
        targetState = isAppEditMode,
        transitionSpec = {
          (fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 4 })
            .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 4 })
        },
        label = "drawerTopHeaderTransition"
      ) { editModeActive ->
        if (editModeActive) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(accentColor)
                .clickable {
                  HapticFeedbackHelper.performClick(rootContext)
                  onCommitEditModeChanges?.invoke()
                  onExitEditMode()
                }
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("concluir_edit_mode_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Concluir",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp
              )
            }
          }
        } else {
          val activeSearchBg by animateColorAsState(
            targetValue = if (isSearchFocused) searchBgFocused else searchBg,
            animationSpec = tween(200),
            label = "searchBarBgColor"
          )
          val activeSearchBorder by animateColorAsState(
            targetValue = if (isSearchFocused) searchBorderFocused else searchBorderUnfocused,
            animationSpec = tween(200),
            label = "searchBarBorderColor"
          )
          val searchIconTint by animateColorAsState(
            targetValue = if (isSearchFocused) accentColor else textSecondary,
            animationSpec = tween(200),
            label = "searchIconTint"
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .padding(horizontal = 6.dp)
              .clip(CircleShape)
              .background(activeSearchBg)
              .border(0.8.dp, activeSearchBorder, CircleShape)
              .padding(horizontal = 16.dp)
              .testTag("search_bar"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = SvgSearchIcon,
              contentDescription = "Ícone de pesquisa",
              tint = searchIconTint,
              modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(11.dp))
            Box(
              modifier = Modifier.weight(1f),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                  color = textPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Normal,
                  letterSpacing = (-0.1).sp
                ),
                cursorBrush = SolidColor(
                  if (isSearchFocused && isImeVisible) accentColor else Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                  imeAction = ImeAction.Search,
                  keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                  onSearch = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    isSearchFocused = false
                  }
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { focusState ->
                    isSearchFocused = focusState.isFocused
                  },
                decorationBox = { innerTextField ->
                  if (searchQuery.isEmpty()) {
                    Text(
                      text = "Pesquisar aplicativos",
                      color = textSecondary,
                      fontSize = 15.5.sp,
                      fontWeight = FontWeight.Normal,
                      letterSpacing = (-0.1).sp
                    )
                  }
                  innerTextField()
                }
              )
            }
            AnimatedVisibility(
              visible = searchQuery.isNotEmpty(),
              enter = fadeIn(tween(150)),
              exit = fadeOut(tween(120))
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(
                    if (isDark) Color.White.copy(alpha = 0.10f)
                    else Color.Black.copy(alpha = 0.07f)
                  )
                  .clickable {
                    onSearchQueryChange("")
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    isSearchFocused = false
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = SvgCloseIcon,
                  contentDescription = "Limpar pesquisa",
                  tint = textPrimary.copy(alpha = 0.75f),
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = textPrimary.copy(alpha = 0.75f),
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(30.dp)
          )
        }
      } else {
        if (apps.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 32.dp)
            ) {
              Icon(
                imageVector = SvgEmptyAppsIcon,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.42f),
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (searchQuery.isEmpty()) "Nenhum aplicativo disponível" else "Nenhum resultado encontrado",
                color = textPrimary.copy(alpha = 0.88f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center
              )
              if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Tente pesquisar com outro nome",
                  color = textSecondary,
                  fontSize = 13.5.sp,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .background(searchBg)
                    .border(0.8.dp, searchBorderUnfocused, CircleShape)
                    .clickable {
                      onSearchQueryChange("")
                      focusManager.clearFocus(force = true)
                      keyboardController?.hide()
                      isSearchFocused = false
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = "Limpar busca",
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        } else if (drawerStyle == "horizontal") {
          val context = LocalContext.current
          val savedHorizontalPages by viewModel.horizontalDrawerPages.collectAsStateWithLifecycle()

          BoxWithConstraints(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            val columns = 4
            val rowsPerPage = 6
            val appsPerPage = columns * rowsPerPage // 4x6 = 24 apps per page
            val gridHPad = 2.dp
            val gridColGap = 4.dp
            val gridCellWidth = ((maxWidth - gridHPad * 2 - gridColGap * (columns - 1)) / columns).coerceAtLeast(52.dp)
            val netGridHeight = (maxHeight - 32.dp).coerceAtLeast(280.dp)
            val verticalRowSpacing = (netGridHeight * 0.008f).coerceIn(3.dp, 6.dp)
            val actualRowHeight = ((netGridHeight - (verticalRowSpacing * (rowsPerPage - 1))) / rowsPerPage)
              .coerceAtLeast(62.dp)
            val labelReservedDp = if (showAppNames) 19.dp else 4.dp
            val maxIconDpAt120 = minOf(gridCellWidth - 2.dp, actualRowHeight - labelReservedDp).coerceIn(54.dp, 92.dp)
            val minIconDpAt50 = (maxIconDpAt120 * 0.485f).coerceIn(36.dp, 45.dp)
            val clampedProgress = iconSizeProgress.coerceIn(0.50f, 1.20f)
            val normalizedScale = (clampedProgress - 0.50f) / 0.70f
            val fixedIconSize = (minIconDpAt50 + (maxIconDpAt120 - minIconDpAt50) * normalizedScale)
              .coerceIn(minIconDpAt50, maxIconDpAt120)

            var optimisticPages by remember { mutableStateOf<List<List<AppInfo?>>?>(null) }
            LaunchedEffect(savedHorizontalPages, apps) {
              optimisticPages = null
            }

            val baseResolvedPages = remember(apps, savedHorizontalPages, searchQuery, optimisticPages) {
              if (searchQuery.isNotEmpty()) {
                apps.chunked(appsPerPage).map { chunk ->
                  List(appsPerPage) { idx -> chunk.getOrNull(idx) }
                }.ifEmpty { listOf(List(appsPerPage) { null }) }
              } else {
                optimisticPages ?: viewModel.resolveHorizontalDrawerPages(apps, savedHorizontalPages)
              }
            }

            var extraDragPagesCount by remember { mutableIntStateOf(0) }
            var selectedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            var draggingGroup by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            var dragSourcePage by remember { mutableIntStateOf(-1) }
            var dragSourceSlot by remember { mutableIntStateOf(-1) }
            var dragTargetSlot by remember { mutableIntStateOf(-1) }
            var dragFingerInContainer by remember { mutableStateOf(Offset.Zero) }
            var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
            var edgeScrollDirection by remember { mutableIntStateOf(0) }
            var containerWidthPx by remember { mutableStateOf(0f) }
            var containerRootPos by remember { mutableStateOf(Offset.Zero) }
            val latestOnAppClick = rememberUpdatedState(onAppClick)
            val latestOnAppLongClick = rememberUpdatedState(onAppLongClick)
            val latestOnAppLongClickWithAnchor = rememberUpdatedState(onAppLongClickWithAnchor)

            val selectedKeysMap = remember(selectedApps) {
              val map = HashMap<String, Int>(selectedApps.size * 2)
              selectedApps.forEachIndexed { idx, app ->
                map["${app.packageName}/${app.activityName}"] = idx
              }
              map
            }
            val draggingKeysSet = remember(draggingGroup) {
              draggingGroup.map { "${it.packageName}/${it.activityName}" }.toHashSet()
            }
            val isDragging = draggingGroup.isNotEmpty()

            val displayPages = remember(baseResolvedPages, extraDragPagesCount) {
              if (extraDragPagesCount <= 0) {
                baseResolvedPages
              } else {
                baseResolvedPages + List(extraDragPagesCount) { List(appsPerPage) { null } }
              }
            }

            val pagerState = rememberPagerState(pageCount = { displayPages.size.coerceAtLeast(1) })

            val activeDestPage = if (isDragging) pagerState.currentPage else -1
            val (liveReorganizedPages, effectiveTargetSlot) = remember(
              displayPages,
              draggingGroup,
              dragSourcePage,
              activeDestPage,
              dragTargetSlot
            ) {
              if (draggingGroup.isEmpty() || dragSourcePage < 0 || dragTargetSlot < 0 || activeDestPage < 0) {
                Pair(displayPages, -1)
              } else {
                viewModel.reorganizeHorizontalPagesMultiple(
                  currentPages = displayPages,
                  movingApps = draggingGroup,
                  sourcePage = dragSourcePage,
                  destPage = activeDestPage,
                  targetSlotOnDestPage = dragTargetSlot,
                  keepEmptyPagesForDrag = true
                )
              }
            }

            val hPadPx = with(density) { gridHPad.toPx() }
            val topPadPx = with(density) { 4.dp.toPx() }
            val colGapPx = with(density) { gridColGap.toPx() }
            val rowGapPx = with(density) { verticalRowSpacing.toPx() }
            val rowHeightPx = with(density) { actualRowHeight.toPx() }

            val latestDisplayPages = rememberUpdatedState(displayPages)
            val latestBasePages = rememberUpdatedState(baseResolvedPages)
            val latestPagerState = rememberUpdatedState(pagerState)
            val latestSearchQuery = rememberUpdatedState(searchQuery)
            val latestContainerWidthPx = rememberUpdatedState(containerWidthPx)
            val latestRowGapPx = rememberUpdatedState(rowGapPx)
            val latestRowHeightPx = rememberUpdatedState(rowHeightPx)
            val latestIsAppEditMode = rememberUpdatedState(isAppEditMode)
            val latestSelectedApps = rememberUpdatedState(selectedApps)

            LaunchedEffect(baseResolvedPages, isAppEditMode) {
              if (isAppEditMode) {
                onCommitEditModeChanges = {
                  selectedApps = emptyList()
                  val keyPages = latestBasePages.value.map { page ->
                    page.map { app -> app?.let { "${it.packageName}/${it.activityName}" } }
                  }
                  viewModel.saveHorizontalDrawerPages(keyPages)
                }
              }
            }

            LaunchedEffect(isAppEditMode) {
              if (!isAppEditMode) {
                selectedApps = emptyList()
                draggingGroup = emptyList()
                dragSourcePage = -1
                dragSourceSlot = -1
                dragTargetSlot = -1
                extraDragPagesCount = 0
                edgeScrollDirection = 0
              }
            }

            // Automatic page flip when holding the dragged group at the left or right screen edge
            LaunchedEffect(isDragging, edgeScrollDirection) {
              if (!isDragging || edgeScrollDirection == 0) return@LaunchedEffect
              delay(220L)
              while (draggingGroup.isNotEmpty() && edgeScrollDirection != 0) {
                if (edgeScrollDirection == 1) {
                  val curPage = pagerState.currentPage
                  val totalNow = latestBasePages.value.size + extraDragPagesCount
                  // Only create a new page if we are at the last page AND the last page is genuinely full (or cannot fit the dragged group without overflow)
                  if (curPage >= totalNow - 1 && totalNow < 12) {
                    val movingKeys = draggingGroup.map { "${it.packageName}/${it.activityName}" }.toSet()
                    val lastPageRemainingCount = latestBasePages.value.lastOrNull()?.count { app ->
                      app != null && !movingKeys.contains("${app.packageName}/${app.activityName}")
                    } ?: 0
                    if (lastPageRemainingCount >= appsPerPage || (lastPageRemainingCount > 0 && lastPageRemainingCount + draggingGroup.size > appsPerPage)) {
                      extraDragPagesCount += 1
                      delay(25L)
                    }
                  }
                  val updatedTotal = latestBasePages.value.size + extraDragPagesCount
                  if (pagerState.currentPage < updatedTotal - 1) {
                    HapticFeedbackHelper.performClick(context)
                    try {
                      pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = spring(
                          dampingRatio = Spring.DampingRatioNoBouncy,
                          stiffness = Spring.StiffnessMedium
                        )
                      )
                    } catch (_: Throwable) {}
                  }
                } else if (edgeScrollDirection == -1) {
                  if (pagerState.currentPage > 0) {
                    HapticFeedbackHelper.performClick(context)
                    try {
                      pagerState.animateScrollToPage(
                        page = pagerState.currentPage - 1,
                        animationSpec = spring(
                          dampingRatio = Spring.DampingRatioNoBouncy,
                          stiffness = Spring.StiffnessMedium
                        )
                      )
                    } catch (_: Throwable) {}
                  }
                }
                delay(420L)
              }
            }

            LaunchedEffect(searchQuery) {
              if (searchQuery.isNotEmpty() && pagerState.currentPage != 0) {
                pagerState.scrollToPage(0)
              }
            }

            LaunchedEffect(pagerState.isScrollInProgress) {
              performanceTracker.setScrollOrGestureActive(pagerState.isScrollInProgress)
              if (pagerState.isScrollInProgress && isSearchFocused) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                isSearchFocused = false
              }
            }

            Box(
              modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                  containerWidthPx = coords.size.width.toFloat()
                  containerRootPos = coords.positionInRoot()
                }
                .pointerInput(screenHeightPx) {
                  val touchSlopPx = 8.dp.toPx()
                  val moveSlopAfterLongPressPx = 4.dp.toPx()
                  val edgeZonePx = 42.dp.toPx()
                  val maxGrabOffsetPx = 28.dp.toPx()

                  fun computeSlot(pos: Offset): Int {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap
                    val totalGridH = rHeight * 6f + rGap * 5f

                    val relX = (pos.x - hPadPx).coerceIn(0f, totalGridW - 0.1f)
                    val relY = (pos.y - topPadPx).coerceIn(0f, totalGridH - 0.1f)
                    val col = (relX / colStep).toInt().coerceIn(0, 3)
                    val row = (relY / rowStep).toInt().coerceIn(0, 5)
                    return row * 4 + col
                  }

                  fun computeSlotCenter(slotIndex: Int): Offset {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap
                    val r = (slotIndex / 4).coerceIn(0, 5)
                    val c = (slotIndex % 4).coerceIn(0, 3)
                    return Offset(
                      x = hPadPx + c * colStep + colW * 0.5f,
                      y = topPadPx + r * rowStep + rHeight * 0.5f
                    )
                  }

                  fun computeSlotBoundsInRoot(slotIndex: Int): Pair<Rect, Rect> {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap
                    val r = (slotIndex / 4).coerceIn(0, 5)
                    val c = (slotIndex % 4).coerceIn(0, 3)
                    val itemLeft = containerRootPos.x + hPadPx + c * colStep
                    val itemTop = containerRootPos.y + topPadPx + r * rowStep
                    val itemRect = Rect(itemLeft, itemTop, itemLeft + colW, itemTop + rHeight)
                    val iconPx = with(density) { fixedIconSize.toPx() }
                    val iconLeft = itemLeft + (colW - iconPx) * 0.5f
                    val iconTop = itemTop + (if (showAppNames) (rHeight - iconPx - with(density) { 18.dp.toPx() }) * 0.5f else (rHeight - iconPx) * 0.5f).coerceAtLeast(0f)
                    val iconRect = Rect(iconLeft, iconTop, iconLeft + iconPx, iconTop + iconPx)
                    return Pair(iconRect, itemRect)
                  }

                  awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pointerId = down.id
                    val downPos = down.position
                    var currentPos = downPos
                    var isDragStarted = false
                    var isSelectionTriggeredOnHold = false
                    var isNormalMenuTriggered = false
                    var hasMovedDuringDrag = false
                    var isVerticalDrawerSwipe = false
                    var isHorizontalPagerSwipe = false
                    var totalVerticalDrag = 0f

                    val rHeight = latestRowHeightPx.value
                    val rGap = latestRowGapPx.value
                    val totalGridH = rHeight * 6f + rGap * 5f
                    val isInsideGrid = downPos.y >= 0f &&
                      downPos.y <= (topPadPx + totalGridH + rHeight * 0.25f) &&
                      downPos.x >= 0f &&
                      downPos.x <= latestContainerWidthPx.value

                    val startPage = latestPagerState.value.currentPage
                    val pressedSlot = computeSlot(downPos)
                    val pressedApp = if (isInsideGrid) {
                      latestDisplayPages.value.getOrNull(startPage)?.getOrNull(pressedSlot)
                    } else null

                    fun toggleSelectApp(app: AppInfo) {
                      val key = "${app.packageName}/${app.activityName}"
                      val cur = latestSelectedApps.value.toMutableList()
                      val existingIdx = cur.indexOfFirst { "${it.packageName}/${it.activityName}" == key }
                      if (existingIdx >= 0) {
                        cur.removeAt(existingIdx)
                      } else {
                        cur.add(app)
                      }
                      selectedApps = cur
                    }

                    fun ensureAppSelected(app: AppInfo) {
                      val key = "${app.packageName}/${app.activityName}"
                      val cur = latestSelectedApps.value
                      if (cur.none { "${it.packageName}/${it.activityName}" == key }) {
                        selectedApps = cur + app
                      }
                    }

                    fun startDraggingGroup() {
                      if (pressedApp == null || isDragStarted || !latestIsAppEditMode.value || latestSearchQuery.value.isNotEmpty()) return
                      isDragStarted = true
                      if (!isSelectionTriggeredOnHold) {
                        HapticFeedbackHelper.performClick(context)
                      }
                      focusManager.clearFocus(force = true)
                      keyboardController?.hide()
                      isSearchFocused = false

                      val pressedKey = "${pressedApp.packageName}/${pressedApp.activityName}"
                      val curSelected = latestSelectedApps.value
                      val groupToDrag = if (curSelected.any { "${it.packageName}/${it.activityName}" == pressedKey }) {
                        curSelected
                      } else if (curSelected.isNotEmpty()) {
                        val updated = curSelected + pressedApp
                        selectedApps = updated
                        updated
                      } else {
                        listOf(pressedApp)
                      }

                      val slotCenter = computeSlotCenter(pressedSlot)
                      val rawGrab = currentPos - slotCenter
                      dragGrabOffset = Offset(
                        rawGrab.x.coerceIn(-maxGrabOffsetPx, maxGrabOffsetPx),
                        rawGrab.y.coerceIn(-maxGrabOffsetPx, maxGrabOffsetPx)
                      )
                      dragSourcePage = startPage
                      dragSourceSlot = pressedSlot
                      dragTargetSlot = pressedSlot
                      dragFingerInContainer = currentPos
                      draggingGroup = groupToDrag
                    }

                    var longPressJob: Job? = null
                    if (pressedApp != null) {
                      if (latestIsAppEditMode.value && latestSearchQuery.value.isEmpty()) {
                        longPressJob = scope.launch {
                          delay(180L)
                          if (!isVerticalDrawerSwipe &&
                              !isHorizontalPagerSwipe &&
                              !isDragStarted &&
                              !latestPagerState.value.isScrollInProgress) {
                            isSelectionTriggeredOnHold = true
                            HapticFeedbackHelper.performClick(context)
                            ensureAppSelected(pressedApp)
                          }
                        }
                      } else if (!latestIsAppEditMode.value) {
                        longPressJob = scope.launch {
                          delay(400L)
                          if (!isVerticalDrawerSwipe &&
                              !isHorizontalPagerSwipe &&
                              !isNormalMenuTriggered &&
                              !latestPagerState.value.isScrollInProgress) {
                            isNormalMenuTriggered = true
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            isSearchFocused = false
                            val (iconRect, itemRect) = computeSlotBoundsInRoot(pressedSlot)
                            val anchor = AppMenuAnchor(
                              app = pressedApp,
                              iconBoundsInRoot = iconRect,
                              itemBoundsInRoot = itemRect,
                              showLabel = showAppNames,
                              textColor = textPrimary
                            )
                            latestOnAppLongClickWithAnchor.value?.invoke(anchor)
                            latestOnAppLongClick.value.invoke(pressedApp)
                          }
                        }
                      }
                    }

                    while (true) {
                      val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                      val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                      if (!change.pressed) {
                        if (isDragStarted || isSelectionTriggeredOnHold || isNormalMenuTriggered) {
                          change.consume()
                        }
                        break
                      }

                      currentPos = change.position
                      val totalDelta = currentPos - downPos

                      if (!isDragStarted) {
                        if (latestIsAppEditMode.value && pressedApp != null && latestSearchQuery.value.isEmpty()) {
                          // If user already held to select or drags on a selected app (or drags vertically on any app), start group drag immediately
                          val pressedKey = "${pressedApp.packageName}/${pressedApp.activityName}"
                          val isPressedSelected = latestSelectedApps.value.any { "${it.packageName}/${it.activityName}" == pressedKey }
                          if (isSelectionTriggeredOnHold && totalDelta.getDistance() > moveSlopAfterLongPressPx) {
                            startDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (isPressedSelected && totalDelta.getDistance() > touchSlopPx) {
                            longPressJob?.cancel()
                            startDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (!isHorizontalPagerSwipe && abs(totalDelta.y) > touchSlopPx && abs(totalDelta.y) >= abs(totalDelta.x)) {
                            longPressJob?.cancel()
                            startDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (!isHorizontalPagerSwipe && abs(totalDelta.x) > touchSlopPx * 1.3f && abs(totalDelta.x) > abs(totalDelta.y)) {
                            isHorizontalPagerSwipe = true
                            longPressJob?.cancel()
                          }
                        } else if (!latestIsAppEditMode.value) {
                          if (!isVerticalDrawerSwipe && !isHorizontalPagerSwipe) {
                            if (abs(totalDelta.x) > touchSlopPx && abs(totalDelta.x) > abs(totalDelta.y)) {
                              isHorizontalPagerSwipe = true
                              longPressJob?.cancel()
                            } else if (abs(totalDelta.y) > touchSlopPx && abs(totalDelta.y) > abs(totalDelta.x)) {
                              isVerticalDrawerSwipe = true
                              longPressJob?.cancel()
                              if (isSearchFocused) {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                isSearchFocused = false
                              }
                            }
                          }

                          if (isNormalMenuTriggered) {
                            change.consume()
                          } else if (isVerticalDrawerSwipe) {
                            val dy = change.positionChange().y
                            if (dy > 0f || drawerOffset.value > 0f) {
                              change.consume()
                              totalVerticalDrag += dy
                              val newOffset = (drawerOffset.value + dy).coerceIn(0f, screenHeightPx)
                              scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                                drawerOffset.snapTo(newOffset)
                              }
                            }
                          }
                        }
                      } else {
                        // Active drag mode inside Edit Mode: consume pointer events and update target slot & edge scroll
                        change.consume()
                        dragFingerInContainer = currentPos
                        if (!hasMovedDuringDrag && totalDelta.getDistance() > moveSlopAfterLongPressPx) {
                          hasMovedDuringDrag = true
                        }
                        val floatingCenter = currentPos - dragGrabOffset
                        val newTargetSlot = computeSlot(floatingCenter)
                        if (newTargetSlot != dragTargetSlot) {
                          dragTargetSlot = newTargetSlot
                        }

                        val w = latestContainerWidthPx.value
                        edgeScrollDirection = when {
                          currentPos.x < edgeZonePx -> -1
                          w > 0f && currentPos.x > (w - edgeZonePx) -> 1
                          else -> 0
                        }
                      }
                    }

                    longPressJob?.cancel()
                    edgeScrollDirection = 0

                    if (latestIsAppEditMode.value && pressedApp != null) {
                      if (isDragStarted && draggingGroup.isNotEmpty()) {
                        val destPage = latestPagerState.value.currentPage
                        val destSlot = dragTargetSlot.coerceIn(0, appsPerPage - 1)
                        val movedToDifferentSlot = destPage != dragSourcePage || destSlot != dragSourceSlot || draggingGroup.size > 1

                        if (hasMovedDuringDrag || movedToDifferentSlot) {
                          val (finalPages, _) = viewModel.reorganizeHorizontalPagesMultiple(
                            currentPages = latestDisplayPages.value,
                            movingApps = draggingGroup,
                            sourcePage = dragSourcePage,
                            destPage = destPage,
                            targetSlotOnDestPage = destSlot,
                            keepEmptyPagesForDrag = false
                          )
                          optimisticPages = finalPages
                          val keyPages = finalPages.map { page ->
                            page.map { app -> app?.let { "${it.packageName}/${it.activityName}" } }
                          }
                          viewModel.saveHorizontalDrawerPages(keyPages)
                          HapticFeedbackHelper.performClick(context)

                          val firstMoved = draggingGroup.first()
                          val landedPageIdx = finalPages.indexOfFirst { page ->
                            page.any {
                              it?.packageName == firstMoved.packageName &&
                                it.activityName == firstMoved.activityName
                            }
                          }
                          if (landedPageIdx >= 0 && landedPageIdx != latestPagerState.value.currentPage) {
                            scope.launch {
                              try {
                                latestPagerState.value.scrollToPage(landedPageIdx)
                              } catch (_: Throwable) {}
                            }
                          }
                        }

                        selectedApps = emptyList()
                        draggingGroup = emptyList()
                        dragSourcePage = -1
                        dragSourceSlot = -1
                        dragTargetSlot = -1
                        extraDragPagesCount = 0
                      } else if (!isSelectionTriggeredOnHold && !isHorizontalPagerSwipe && !isVerticalDrawerSwipe) {
                        val totalDist = (currentPos - downPos).getDistance()
                        if (totalDist <= touchSlopPx * 1.4f && latestSelectedApps.value.isNotEmpty()) {
                          HapticFeedbackHelper.performClick(context)
                          toggleSelectApp(pressedApp)
                        }
                      }
                    } else if (!latestIsAppEditMode.value) {
                      if (isVerticalDrawerSwipe && drawerOffset.value > 0f) {
                        val shouldClose = totalVerticalDrag > 90f || drawerOffset.value > screenHeightPx * 0.15f
                        scope.launch {
                          if (shouldClose) {
                            onDrawerOpenChange(false)
                            try {
                              drawerOffset.animateTo(screenHeightPx, drawerSpringSpec)
                            } catch (_: Throwable) {}
                          } else {
                            onDrawerOpenChange(true)
                            try {
                              drawerOffset.animateTo(0f, drawerSpringSpec)
                            } catch (_: Throwable) {}
                          }
                        }
                      } else if (!isNormalMenuTriggered && !isHorizontalPagerSwipe && !isVerticalDrawerSwipe && pressedApp != null) {
                        val totalDist = (currentPos - downPos).getDistance()
                        if (totalDist <= touchSlopPx * 1.5f && !latestPagerState.value.isScrollInProgress) {
                          focusManager.clearFocus(force = true)
                          keyboardController?.hide()
                          isSearchFocused = false
                          latestOnAppClick.value.invoke(pressedApp)
                        }
                      }
                    }
                  }
                }
            ) {
              val cellWidth = gridCellWidth
              val cellWidthPx = with(density) { cellWidth.toPx() }
              val colStepPx = cellWidthPx + colGapPx
              val rowStepPx = rowHeightPx + rowGapPx

              val pagerSnapSpring = remember {
                spring<Float>(
                  dampingRatio = Spring.DampingRatioNoBouncy,
                  stiffness = 780f
                )
              }
              val pagerFlingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.22f,
                snapAnimationSpec = pagerSnapSpring
              )

              Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                HorizontalPager(
                  state = pagerState,
                  beyondViewportPageCount = 1,
                  flingBehavior = pagerFlingBehavior,
                  key = { it },
                  userScrollEnabled = !isDragging,
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                ) { pageIndex ->
                  val pageSlots = liveReorganizedPages.getOrElse(pageIndex) { List(appsPerPage) { null } }
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(start = gridHPad, top = 4.dp, end = gridHPad, bottom = 4.dp)
                  ) {
                    // 1. Background 4x6 slot guides while in Edit Mode
                    if (isAppEditMode) {
                      for (slotIndex in 0 until appsPerPage) {
                        val colIndex = slotIndex % columns
                        val rowIndex = slotIndex / columns
                        val slotOffset = IntOffset(
                          x = (colIndex * colStepPx).roundToInt(),
                          y = (rowIndex * rowStepPx).roundToInt()
                        )
                        val isTargetSlotWhileDragging =
                          isDragging && pageIndex == pagerState.currentPage &&
                            slotIndex >= effectiveTargetSlot &&
                            slotIndex < (effectiveTargetSlot + draggingGroup.size).coerceAtMost(appsPerPage)

                        val slotShape = RoundedCornerShape(16.dp)
                        val slotBorderColor = if (isTargetSlotWhileDragging) {
                          accentColor.copy(alpha = 0.85f)
                        } else {
                          if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                        }
                        val slotBgColor = if (isTargetSlotWhileDragging) {
                          accentColor.copy(alpha = if (isDark) 0.18f else 0.14f)
                        } else {
                          if (isDark) Color.White.copy(alpha = 0.022f) else Color.Black.copy(alpha = 0.018f)
                        }

                        Box(
                          modifier = Modifier
                            .offset { slotOffset }
                            .width(cellWidth)
                            .height(actualRowHeight)
                            .clip(slotShape)
                            .background(slotBgColor, slotShape)
                            .border(
                              width = if (isTargetSlotWhileDragging) 1.5.dp else 0.8.dp,
                              color = slotBorderColor,
                              shape = slotShape
                            )
                        )
                      }
                    }

                    // 2. Contiguously organized apps with fast, responsive position updates
                    for (slotIndex in 0 until appsPerPage) {
                      val app = pageSlots.getOrNull(slotIndex) ?: continue
                      val appKey = "${app.packageName}/${app.activityName}"
                      val isDraggedAppItself = draggingKeysSet.contains(appKey)

                      if (!isDraggedAppItself) {
                        key(appKey) {
                          val colIndex = slotIndex % columns
                          val rowIndex = slotIndex / columns
                          val targetOffset = IntOffset(
                            x = (colIndex * colStepPx).roundToInt(),
                            y = (rowIndex * rowStepPx).roundToInt()
                          )
                          val isHighlighted = !isAppEditMode &&
                            highlightedApp != null &&
                            highlightedApp.packageName == app.packageName &&
                            highlightedApp.activityName == app.activityName
                          val selIdx = if (isAppEditMode) (selectedKeysMap[appKey] ?: -1) else -1

                          Box(
                            modifier = Modifier
                              .offset { targetOffset }
                              .width(cellWidth)
                              .height(actualRowHeight),
                            contentAlignment = Alignment.Center
                          ) {
                            AppGridItem(
                              app = app,
                              showLabel = showAppNames,
                              iconSize = fixedIconSize,
                              viewModel = viewModel,
                              textColor = textPrimary,
                              iconShape = iconShape,
                              selectedIconPack = selectedIconPack,
                              isMenuHighlighted = isHighlighted,
                              isSelected = selIdx >= 0,
                              selectionIndex = selIdx,
                              isInteractive = false,
                              enableLongPress = false,
                              onClick = {},
                              onLongClick = {}
                            )
                          }
                        }
                      }
                    }
                  }
                }

                // Page indicator dots drawn entirely in the GPU/Canvas draw phase (zero recomposition or layout pass during scroll)
                val pageCount = displayPages.size
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .drawBehind {
                      if (pageCount > 1 || isDragging) {
                        val continuousPos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        val dotHeightPx = 6.dp.toPx()
                        val gapPx = 6.dp.toPx()
                        val maxDotWidthPx = 20.dp.toPx()
                        val minDotWidthPx = 6.dp.toPx()
                        val cornerRadius = CornerRadius(dotHeightPx * 0.5f, dotHeightPx * 0.5f)

                        var totalRowWidthPx = 0f
                        for (i in 0 until pageCount) {
                          val dist = abs(continuousPos - i).coerceIn(0f, 1f)
                          val w = maxDotWidthPx - (maxDotWidthPx - minDotWidthPx) * dist
                          totalRowWidthPx += w
                          if (i < pageCount - 1) totalRowWidthPx += gapPx
                        }

                        var currentX = (size.width - totalRowWidthPx) * 0.5f
                        val centerY = (size.height - dotHeightPx) * 0.5f

                        for (i in 0 until pageCount) {
                          val dist = abs(continuousPos - i).coerceIn(0f, 1f)
                          val w = maxDotWidthPx - (maxDotWidthPx - minDotWidthPx) * dist
                          val dotAlpha = 0.92f - 0.62f * dist
                          val isCurrent = pagerState.currentPage == i
                          val color = if (isCurrent && isDragging) accentColor else textPrimary.copy(alpha = dotAlpha)

                          drawRoundRect(
                            color = color,
                            topLeft = Offset(currentX, centerY),
                            size = Size(w, dotHeightPx),
                            cornerRadius = cornerRadius
                          )
                          currentX += w + gapPx
                        }
                      }
                    }
                )
              }

              // Floating dragged app / group overlay
              if (draggingGroup.isNotEmpty()) {
                val primaryApp = draggingGroup.first()
                val groupSize = draggingGroup.size
                val floatingIconSize = fixedIconSize
                val floatingCenter = dragFingerInContainer - dragGrabOffset
                val boxWidthDp = gridCellWidth
                val boxHeightDp = actualRowHeight
                val halfW = with(density) { (boxWidthDp * 0.5f).toPx() }
                val halfH = with(density) { (boxHeightDp * 0.5f).toPx() }

                Box(
                  modifier = Modifier
                    .offset {
                      IntOffset(
                        x = (floatingCenter.x - halfW).roundToInt(),
                        y = (floatingCenter.y - halfH).roundToInt()
                      )
                    }
                    .width(boxWidthDp)
                    .height(boxHeightDp)
                    .graphicsLayer {
                      scaleX = 1.10f
                      scaleY = 1.10f
                      alpha = 0.96f
                    },
                  contentAlignment = Alignment.Center
                ) {
                  // Stacked preview icons behind primary icon when moving a group of apps
                  if (groupSize > 1) {
                    val previewApps = draggingGroup.drop(1).take(2).reversed()
                    previewApps.forEachIndexed { idx, stackedApp ->
                      val level = previewApps.size - idx
                      val stackedBitmap = remember(
                        stackedApp.packageName,
                        stackedApp.activityName,
                        iconShape,
                        selectedIconPack
                      ) {
                        viewModel.getAppIconFast(stackedApp.packageName, stackedApp.activityName)
                      }
                      if (stackedBitmap != null) {
                        Image(
                          bitmap = stackedBitmap,
                          contentDescription = null,
                          contentScale = ContentScale.Fit,
                          modifier = Modifier
                            .size(floatingIconSize)
                            .graphicsLayer {
                              translationX = (level * 12f)
                              translationY = -(level * 10f)
                              rotationZ = (level * 6f)
                              scaleX = 0.90f
                              scaleY = 0.90f
                              alpha = 0.78f
                            }
                        )
                      }
                    }
                  }

                  val primaryBitmap = remember(
                    primaryApp.packageName,
                    primaryApp.activityName,
                    iconShape,
                    selectedIconPack
                  ) {
                    viewModel.getAppIconFast(primaryApp.packageName, primaryApp.activityName)
                  }

                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = if (showAppNames) 1.dp else 0.dp, horizontal = 2.5.dp)
                  ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                      if (primaryBitmap != null) {
                        Image(
                          bitmap = primaryBitmap,
                          contentDescription = primaryApp.label,
                          contentScale = ContentScale.Fit,
                          modifier = Modifier.size(floatingIconSize)
                        )
                      } else {
                        Box(modifier = Modifier.size(floatingIconSize))
                      }

                      if (groupSize > 1) {
                        Box(
                          modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            text = "$groupSize",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                          )
                        }
                      }
                    }
                    if (showAppNames) {
                      Spacer(modifier = Modifier.height(3.dp))
                      Text(
                        text = if (groupSize > 1) "$groupSize selecionados" else primaryApp.label,
                        color = textPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                          shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.65f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                          )
                        ),
                        modifier = Modifier.fillMaxWidth()
                      )
                    }
                  }
                }
              }
            }
          }
        } else {
          val context = LocalContext.current
          val savedHorizontalPages by viewModel.horizontalDrawerPages.collectAsStateWithLifecycle()

          BoxWithConstraints(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            val columns = 4
            val rowsPerScreen = 6
            val gridHPad = 2.dp
            val gridColGap = 4.dp
            val gridCellWidth = ((maxWidth - gridHPad * 2 - gridColGap * (columns - 1)) / columns).coerceAtLeast(52.dp)
            val netGridHeight = (maxHeight - 16.dp).coerceAtLeast(280.dp)
            val verticalRowSpacing = (netGridHeight * 0.008f).coerceIn(3.dp, 6.dp)
            val actualRowHeight = ((netGridHeight - (verticalRowSpacing * (rowsPerScreen - 1))) / rowsPerScreen)
              .coerceAtLeast(62.dp)
            val labelReservedDp = if (showAppNames) 19.dp else 4.dp
            val maxIconDpAt120 = minOf(gridCellWidth - 2.dp, actualRowHeight - labelReservedDp).coerceIn(54.dp, 92.dp)
            val minIconDpAt50 = (maxIconDpAt120 * 0.485f).coerceIn(36.dp, 45.dp)
            val clampedProgress = iconSizeProgress.coerceIn(0.50f, 1.20f)
            val normalizedScale = (clampedProgress - 0.50f) / 0.70f
            val fixedIconSize = (minIconDpAt50 + (maxIconDpAt120 - minIconDpAt50) * normalizedScale)
              .coerceIn(minIconDpAt50, maxIconDpAt120)

            var optimisticOrderedApps by remember { mutableStateOf<List<AppInfo>?>(null) }
            LaunchedEffect(savedHorizontalPages, apps) {
              optimisticOrderedApps = null
            }

            val baseOrderedApps = remember(apps, savedHorizontalPages, searchQuery, optimisticOrderedApps) {
              if (searchQuery.isNotEmpty()) {
                apps
              } else {
                optimisticOrderedApps ?: viewModel.resolveOrderedApps(apps, savedHorizontalPages)
              }
            }

            var selectedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            var draggingGroup by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            var dragSourceIndex by remember { mutableIntStateOf(-1) }
            var dragTargetIndex by remember { mutableIntStateOf(-1) }
            var dragFingerInContainer by remember { mutableStateOf(Offset.Zero) }
            var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
            var verticalAutoScrollDir by remember { mutableIntStateOf(0) }
            var containerWidthPx by remember { mutableStateOf(0f) }
            var containerHeightPx by remember { mutableStateOf(0f) }
            var containerRootPos by remember { mutableStateOf(Offset.Zero) }
            val latestOnAppClick = rememberUpdatedState(onAppClick)
            val latestOnAppLongClick = rememberUpdatedState(onAppLongClick)
            val latestOnAppLongClickWithAnchor = rememberUpdatedState(onAppLongClickWithAnchor)

            val selectedKeysMap = remember(selectedApps) {
              val map = HashMap<String, Int>(selectedApps.size * 2)
              selectedApps.forEachIndexed { idx, app ->
                map["${app.packageName}/${app.activityName}"] = idx
              }
              map
            }
            val draggingKeysSet = remember(draggingGroup) {
              draggingGroup.map { "${it.packageName}/${it.activityName}" }.toHashSet()
            }
            val isDragging = draggingGroup.isNotEmpty()

            val (liveOrderedApps, effectiveTargetIndex) = remember(
              baseOrderedApps,
              draggingGroup,
              dragTargetIndex
            ) {
              if (draggingGroup.isEmpty() || dragTargetIndex < 0) {
                Pair(baseOrderedApps, -1)
              } else {
                viewModel.reorganizeOrderedAppsMultiple(
                  currentApps = baseOrderedApps,
                  movingApps = draggingGroup,
                  targetIndex = dragTargetIndex
                )
              }
            }

            val totalRows = ((liveOrderedApps.size + columns - 1) / columns).coerceAtLeast(1)
            val displaySlotRows = if (isAppEditMode) maxOf(totalRows, rowsPerScreen) else totalRows
            val totalContentHeightDp = 4.dp +
              (actualRowHeight * displaySlotRows) +
              (verticalRowSpacing * (displaySlotRows - 1).coerceAtLeast(0)) +
              24.dp

            val hPadPx = with(density) { gridHPad.toPx() }
            val topPadPx = with(density) { 4.dp.toPx() }
            val colGapPx = with(density) { gridColGap.toPx() }
            val rowGapPx = with(density) { verticalRowSpacing.toPx() }
            val rowHeightPx = with(density) { actualRowHeight.toPx() }

            val latestBaseOrderedApps = rememberUpdatedState(baseOrderedApps)
            val latestSearchQuery = rememberUpdatedState(searchQuery)
            val latestContainerWidthPx = rememberUpdatedState(containerWidthPx)
            val latestContainerHeightPx = rememberUpdatedState(containerHeightPx)
            val latestRowGapPx = rememberUpdatedState(rowGapPx)
            val latestRowHeightPx = rememberUpdatedState(rowHeightPx)
            val latestIsAppEditMode = rememberUpdatedState(isAppEditMode)
            val latestSelectedApps = rememberUpdatedState(selectedApps)

            LaunchedEffect(baseOrderedApps, isAppEditMode) {
              if (isAppEditMode) {
                onCommitEditModeChanges = {
                  selectedApps = emptyList()
                  viewModel.saveCustomOrderedApps(latestBaseOrderedApps.value)
                }
              }
            }

            LaunchedEffect(isAppEditMode) {
              if (!isAppEditMode) {
                selectedApps = emptyList()
                draggingGroup = emptyList()
                dragSourceIndex = -1
                dragTargetIndex = -1
                verticalAutoScrollDir = 0
              }
            }

            // Smooth automatic vertical scrolling when dragging near top/bottom (or left/right) edges
            LaunchedEffect(isDragging, verticalAutoScrollDir) {
              if (!isDragging || verticalAutoScrollDir == 0) return@LaunchedEffect
              delay(140L)
              val stepPx = with(density) { 14.dp.toPx() } * verticalAutoScrollDir
              while (draggingGroup.isNotEmpty() && verticalAutoScrollDir != 0) {
                val canScroll = if (verticalAutoScrollDir > 0) {
                  verticalScrollState.canScrollForward
                } else {
                  verticalScrollState.canScrollBackward
                }
                if (canScroll) {
                  verticalScrollState.scrollBy(stepPx)
                  val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                  val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                  val rGap = latestRowGapPx.value
                  val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                  val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                  val colStep = colW + colGapPx
                  val rowStep = rHeight + rGap
                  val floatingCenter = dragFingerInContainer - dragGrabOffset
                  val contentY = floatingCenter.y + verticalScrollState.value
                  val relX = (floatingCenter.x - hPadPx).coerceIn(0f, totalGridW - 0.1f)
                  val relY = (contentY - topPadPx).coerceAtLeast(0f)
                  val col = (relX / colStep).toInt().coerceIn(0, 3)
                  val row = (relY / rowStep).toInt().coerceAtLeast(0)
                  val maxIdx = (latestBaseOrderedApps.value.size - 1).coerceAtLeast(0)
                  val newSlot = (row * 4 + col).coerceIn(0, maxIdx)
                  if (newSlot != dragTargetIndex) {
                    dragTargetIndex = newSlot
                  }
                }
                delay(16L)
              }
            }

            Box(
              modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                  containerWidthPx = coords.size.width.toFloat()
                  containerHeightPx = coords.size.height.toFloat()
                  containerRootPos = coords.positionInRoot()
                }
                .pointerInput(screenHeightPx) {
                  val touchSlopPx = 8.dp.toPx()
                  val moveSlopAfterLongPressPx = 4.dp.toPx()
                  val verticalEdgeZonePx = 58.dp.toPx()
                  val sideEdgeZonePx = 28.dp.toPx()
                  val maxGrabOffsetPx = 28.dp.toPx()

                  fun computeSlotFromViewportPos(pos: Offset): Int {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap

                    val contentY = pos.y + verticalScrollState.value
                    val relX = (pos.x - hPadPx).coerceIn(0f, totalGridW - 0.1f)
                    val relY = (contentY - topPadPx).coerceAtLeast(0f)
                    val col = (relX / colStep).toInt().coerceIn(0, 3)
                    val row = (relY / rowStep).toInt().coerceAtLeast(0)
                    val maxIdx = (latestBaseOrderedApps.value.size - 1).coerceAtLeast(0)
                    return (row * 4 + col).coerceIn(0, maxIdx)
                  }

                  fun computeRawGridIndex(pos: Offset): Int {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap

                    val contentY = pos.y + verticalScrollState.value
                    if (contentY < 0f || pos.x < 0f || pos.x > w) return -1
                    val relX = (pos.x - hPadPx).coerceIn(0f, totalGridW - 0.1f)
                    val relY = (contentY - topPadPx).coerceAtLeast(0f)
                    val col = (relX / colStep).toInt().coerceIn(0, 3)
                    val row = (relY / rowStep).toInt().coerceAtLeast(0)
                    return row * 4 + col
                  }

                  fun computeSlotViewportCenter(slotIndex: Int): Offset {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap
                    val r = (slotIndex / 4).coerceAtLeast(0)
                    val c = (slotIndex % 4).coerceIn(0, 3)
                    return Offset(
                      x = hPadPx + c * colStep + colW * 0.5f,
                      y = topPadPx + r * rowStep + rHeight * 0.5f - verticalScrollState.value
                    )
                  }

                  fun computeSlotBoundsInRoot(slotIndex: Int): Pair<Rect, Rect> {
                    val w = latestContainerWidthPx.value.coerceAtLeast(1f)
                    val rHeight = latestRowHeightPx.value.coerceAtLeast(1f)
                    val rGap = latestRowGapPx.value
                    val totalGridW = (w - hPadPx * 2f).coerceAtLeast(1f)
                    val colW = ((totalGridW - colGapPx * 3f) / 4f).coerceAtLeast(1f)
                    val colStep = colW + colGapPx
                    val rowStep = rHeight + rGap
                    val r = (slotIndex / 4).coerceAtLeast(0)
                    val c = (slotIndex % 4).coerceIn(0, 3)
                    val itemLeft = containerRootPos.x + hPadPx + c * colStep
                    val itemTop = containerRootPos.y + topPadPx + r * rowStep - verticalScrollState.value
                    val itemRect = Rect(itemLeft, itemTop, itemLeft + colW, itemTop + rHeight)
                    val iconPx = with(density) { fixedIconSize.toPx() }
                    val iconLeft = itemLeft + (colW - iconPx) * 0.5f
                    val iconTop = itemTop + (if (showAppNames) (rHeight - iconPx - with(density) { 18.dp.toPx() }) * 0.5f else (rHeight - iconPx) * 0.5f).coerceAtLeast(0f)
                    val iconRect = Rect(iconLeft, iconTop, iconLeft + iconPx, iconTop + iconPx)
                    return Pair(iconRect, itemRect)
                  }

                  awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pointerId = down.id
                    val downPos = down.position
                    var currentPos = downPos
                    var isDragStarted = false
                    var isSelectionTriggeredOnHold = false
                    var isNormalMenuTriggered = false
                    var hasMovedDuringDrag = false
                    var isUserScrolling = false

                    val rawPressedIdx = computeRawGridIndex(downPos)
                    val pressedApp = if (rawPressedIdx >= 0) {
                      latestBaseOrderedApps.value.getOrNull(rawPressedIdx)
                    } else null

                    fun toggleSelectApp(app: AppInfo) {
                      val key = "${app.packageName}/${app.activityName}"
                      val cur = latestSelectedApps.value.toMutableList()
                      val existingIdx = cur.indexOfFirst { "${it.packageName}/${it.activityName}" == key }
                      if (existingIdx >= 0) {
                        cur.removeAt(existingIdx)
                      } else {
                        cur.add(app)
                      }
                      selectedApps = cur
                    }

                    fun ensureAppSelected(app: AppInfo) {
                      val key = "${app.packageName}/${app.activityName}"
                      val cur = latestSelectedApps.value
                      if (cur.none { "${it.packageName}/${it.activityName}" == key }) {
                        selectedApps = cur + app
                      }
                    }

                    fun startVerticalDraggingGroup() {
                      if (pressedApp == null || isDragStarted || !latestIsAppEditMode.value || latestSearchQuery.value.isNotEmpty()) return
                      isDragStarted = true
                      if (!isSelectionTriggeredOnHold) {
                        HapticFeedbackHelper.performClick(context)
                      }
                      focusManager.clearFocus(force = true)
                      keyboardController?.hide()
                      isSearchFocused = false

                      val pressedKey = "${pressedApp.packageName}/${pressedApp.activityName}"
                      val curSelected = latestSelectedApps.value
                      val groupToDrag = if (curSelected.any { "${it.packageName}/${it.activityName}" == pressedKey }) {
                        curSelected
                      } else if (curSelected.isNotEmpty()) {
                        val updated = curSelected + pressedApp
                        selectedApps = updated
                        updated
                      } else {
                        listOf(pressedApp)
                      }

                      val slotCenter = computeSlotViewportCenter(rawPressedIdx)
                      val rawGrab = currentPos - slotCenter
                      dragGrabOffset = Offset(
                        rawGrab.x.coerceIn(-maxGrabOffsetPx, maxGrabOffsetPx),
                        rawGrab.y.coerceIn(-maxGrabOffsetPx, maxGrabOffsetPx)
                      )
                      dragSourceIndex = rawPressedIdx
                      dragTargetIndex = rawPressedIdx
                      dragFingerInContainer = currentPos
                      draggingGroup = groupToDrag
                    }

                    var longPressJob: Job? = null
                    if (pressedApp != null) {
                      if (latestIsAppEditMode.value && latestSearchQuery.value.isEmpty()) {
                        longPressJob = scope.launch {
                          delay(180L)
                          if (!isUserScrolling && !isDragStarted && !verticalScrollState.isScrollInProgress) {
                            isSelectionTriggeredOnHold = true
                            HapticFeedbackHelper.performClick(context)
                            ensureAppSelected(pressedApp)
                          }
                        }
                      } else if (!latestIsAppEditMode.value) {
                        longPressJob = scope.launch {
                          delay(400L)
                          if (!isUserScrolling && !isNormalMenuTriggered && !verticalScrollState.isScrollInProgress) {
                            isNormalMenuTriggered = true
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            isSearchFocused = false
                            val (iconRect, itemRect) = computeSlotBoundsInRoot(rawPressedIdx)
                            val anchor = AppMenuAnchor(
                              app = pressedApp,
                              iconBoundsInRoot = iconRect,
                              itemBoundsInRoot = itemRect,
                              showLabel = showAppNames,
                              textColor = textPrimary
                            )
                            latestOnAppLongClickWithAnchor.value?.invoke(anchor)
                            latestOnAppLongClick.value.invoke(pressedApp)
                          }
                        }
                      }
                    }

                    while (true) {
                      val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                      val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                      if (!change.pressed) {
                        if (isDragStarted || isSelectionTriggeredOnHold || isNormalMenuTriggered) {
                          change.consume()
                        }
                        break
                      }

                      currentPos = change.position
                      val totalDelta = currentPos - downPos

                      if (!isDragStarted) {
                        if (latestIsAppEditMode.value && pressedApp != null && latestSearchQuery.value.isEmpty()) {
                          val pressedKey = "${pressedApp.packageName}/${pressedApp.activityName}"
                          val isPressedSelected = latestSelectedApps.value.any { "${it.packageName}/${it.activityName}" == pressedKey }
                          if (isSelectionTriggeredOnHold && totalDelta.getDistance() > moveSlopAfterLongPressPx) {
                            startVerticalDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (isPressedSelected && totalDelta.getDistance() > touchSlopPx) {
                            longPressJob?.cancel()
                            startVerticalDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (!isUserScrolling && abs(totalDelta.x) > touchSlopPx && abs(totalDelta.x) >= abs(totalDelta.y)) {
                            longPressJob?.cancel()
                            startVerticalDraggingGroup()
                            hasMovedDuringDrag = true
                            change.consume()
                            continue
                          } else if (!isUserScrolling && totalDelta.getDistance() > touchSlopPx * 1.3f) {
                            isUserScrolling = true
                            longPressJob?.cancel()
                          }
                        } else {
                          if (!isUserScrolling && totalDelta.getDistance() > touchSlopPx) {
                            isUserScrolling = true
                            longPressJob?.cancel()
                            if (isSearchFocused) {
                              focusManager.clearFocus(force = true)
                              keyboardController?.hide()
                              isSearchFocused = false
                            }
                          }
                          if (isNormalMenuTriggered) {
                            change.consume()
                          }
                        }
                      } else {
                        change.consume()
                        dragFingerInContainer = currentPos
                        if (!hasMovedDuringDrag && totalDelta.getDistance() > moveSlopAfterLongPressPx) {
                          hasMovedDuringDrag = true
                        }
                        val floatingCenter = currentPos - dragGrabOffset
                        val newTarget = computeSlotFromViewportPos(floatingCenter)
                        if (newTarget != dragTargetIndex) {
                          dragTargetIndex = newTarget
                        }

                        val h = latestContainerHeightPx.value
                        val w = latestContainerWidthPx.value
                        verticalAutoScrollDir = when {
                          currentPos.y < verticalEdgeZonePx -> -1
                          h > 0f && currentPos.y > (h - verticalEdgeZonePx) -> 1
                          currentPos.x < sideEdgeZonePx -> -1
                          w > 0f && currentPos.x > (w - sideEdgeZonePx) -> 1
                          else -> 0
                        }
                      }
                    }

                    longPressJob?.cancel()
                    verticalAutoScrollDir = 0

                    if (latestIsAppEditMode.value && pressedApp != null) {
                      if (isDragStarted && draggingGroup.isNotEmpty()) {
                        val finalTarget = dragTargetIndex.coerceIn(
                          0,
                          (latestBaseOrderedApps.value.size - 1).coerceAtLeast(0)
                        )
                        val movedToDifferentSlot = finalTarget != dragSourceIndex || draggingGroup.size > 1

                        if (hasMovedDuringDrag || movedToDifferentSlot) {
                          val (finalList, _) = viewModel.reorganizeOrderedAppsMultiple(
                            currentApps = latestBaseOrderedApps.value,
                            movingApps = draggingGroup,
                            targetIndex = finalTarget
                          )
                          optimisticOrderedApps = finalList
                          viewModel.saveCustomOrderedApps(finalList)
                          HapticFeedbackHelper.performClick(context)
                        }

                        selectedApps = emptyList()
                        draggingGroup = emptyList()
                        dragSourceIndex = -1
                        dragTargetIndex = -1
                      } else if (!isSelectionTriggeredOnHold && !isUserScrolling) {
                        val totalDist = (currentPos - downPos).getDistance()
                        if (totalDist <= touchSlopPx * 1.4f && latestSelectedApps.value.isNotEmpty()) {
                          HapticFeedbackHelper.performClick(context)
                          toggleSelectApp(pressedApp)
                        }
                      }
                    } else if (!latestIsAppEditMode.value && !isNormalMenuTriggered && !isUserScrolling && pressedApp != null) {
                      val totalDist = (currentPos - downPos).getDistance()
                      if (totalDist <= touchSlopPx * 1.5f && !verticalScrollState.isScrollInProgress) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        isSearchFocused = false
                        latestOnAppClick.value.invoke(pressedApp)
                      }
                    }
                  }
                }
            ) {
              val cellWidth = gridCellWidth
              val cellWidthPx = with(density) { cellWidth.toPx() }
              val colStepPx = cellWidthPx + colGapPx
              val rowStepPx = rowHeightPx + rowGapPx

              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(verticalScrollState, enabled = !isDragging)
                  .padding(start = gridHPad, top = 4.dp, end = gridHPad, bottom = 24.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(totalContentHeightDp)
                ) {
                  // 1. Background slot guides while in Edit Mode in vertical mode
                  if (isAppEditMode) {
                    val totalSlots = displaySlotRows * columns
                    for (slotIndex in 0 until totalSlots) {
                      val colIndex = slotIndex % columns
                      val rowIndex = slotIndex / columns
                      val slotOffset = IntOffset(
                        x = (colIndex * colStepPx).roundToInt(),
                        y = (rowIndex * rowStepPx).roundToInt()
                      )
                      val isTargetSlotWhileDragging = isDragging &&
                        slotIndex >= effectiveTargetIndex &&
                        slotIndex < (effectiveTargetIndex + draggingGroup.size).coerceAtMost(totalSlots)
                      val slotShape = RoundedCornerShape(16.dp)
                      val slotBorderColor = if (isTargetSlotWhileDragging) {
                        accentColor.copy(alpha = 0.85f)
                      } else {
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                      }
                      val slotBgColor = if (isTargetSlotWhileDragging) {
                        accentColor.copy(alpha = if (isDark) 0.18f else 0.14f)
                      } else {
                        if (isDark) Color.White.copy(alpha = 0.022f) else Color.Black.copy(alpha = 0.018f)
                      }

                      Box(
                        modifier = Modifier
                          .offset { slotOffset }
                          .width(cellWidth)
                          .height(actualRowHeight)
                          .clip(slotShape)
                          .background(slotBgColor, slotShape)
                          .border(
                            width = if (isTargetSlotWhileDragging) 1.5.dp else 0.8.dp,
                            color = slotBorderColor,
                            shape = slotShape
                          )
                      )
                    }
                  }

                  // 2. Contiguously organized apps in vertical mode with ultra-fast passive rendering
                  for (slotIndex in liveOrderedApps.indices) {
                    val app = liveOrderedApps[slotIndex]
                    val appKey = "${app.packageName}/${app.activityName}"
                    val isDraggedAppItself = draggingKeysSet.contains(appKey)

                    if (!isDraggedAppItself) {
                      key(appKey) {
                        val colIndex = slotIndex % columns
                        val rowIndex = slotIndex / columns
                        val targetOffset = IntOffset(
                          x = (colIndex * colStepPx).roundToInt(),
                          y = (rowIndex * rowStepPx).roundToInt()
                        )
                        val isHighlighted = !isAppEditMode &&
                          highlightedApp != null &&
                          highlightedApp.packageName == app.packageName &&
                          highlightedApp.activityName == app.activityName
                        val selIdx = if (isAppEditMode) (selectedKeysMap[appKey] ?: -1) else -1

                        Box(
                          modifier = Modifier
                            .offset { targetOffset }
                            .width(cellWidth)
                            .height(actualRowHeight),
                          contentAlignment = Alignment.Center
                        ) {
                          AppGridItem(
                            app = app,
                            showLabel = showAppNames,
                            iconSize = fixedIconSize,
                            viewModel = viewModel,
                            textColor = textPrimary,
                            iconShape = iconShape,
                            selectedIconPack = selectedIconPack,
                            isMenuHighlighted = isHighlighted,
                            isSelected = selIdx >= 0,
                            selectionIndex = selIdx,
                            isInteractive = false,
                            enableLongPress = false,
                            onClick = {},
                            onLongClick = {}
                          )
                        }
                      }
                    }
                  }
                }
              }

              // Minimalist auto-fading vertical scroll indicator drawn in drawBehind (zero recomposition during scroll)
              val isScrollbarVisible by remember {
                derivedStateOf {
                  verticalScrollState.isScrollInProgress && verticalScrollState.maxValue > 0
                }
              }
              val scrollbarAlpha by animateFloatAsState(
                targetValue = if (isScrollbarVisible) 0.45f else 0f,
                animationSpec = tween(durationMillis = if (isScrollbarVisible) 120 else 450),
                label = "verticalScrollbarAlpha"
              )
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(end = 1.dp)
                  .width(3.dp)
                  .fillMaxHeight()
                  .drawBehind {
                    val alpha = scrollbarAlpha
                    val maxScroll = verticalScrollState.maxValue
                    if (alpha > 0.01f && maxScroll > 0 && size.height > 24f) {
                      val trackPadPx = 12.dp.toPx()
                      val trackH = (size.height - trackPadPx * 2f).coerceAtLeast(40f)
                      val visibleFrac = (netGridHeight / totalContentHeightDp.coerceAtLeast(netGridHeight)).coerceIn(0.12f, 0.55f)
                      val thumbH = (trackH * visibleFrac).coerceAtLeast(28.dp.toPx())
                      val scrollFrac = (verticalScrollState.value.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
                      val thumbY = trackPadPx + (trackH - thumbH) * scrollFrac
                      val r = size.width * 0.5f
                      drawRoundRect(
                        color = textPrimary.copy(alpha = alpha),
                        topLeft = Offset(0f, thumbY),
                        size = Size(size.width, thumbH),
                        cornerRadius = CornerRadius(r, r)
                      )
                    }
                  }
              )

              // Floating dragged app / group overlay in vertical mode
              if (draggingGroup.isNotEmpty()) {
                val primaryApp = draggingGroup.first()
                val groupSize = draggingGroup.size
                val floatingIconSize = fixedIconSize
                val floatingCenter = dragFingerInContainer - dragGrabOffset
                val boxWidthDp = gridCellWidth
                val boxHeightDp = actualRowHeight
                val halfW = with(density) { (boxWidthDp * 0.5f).toPx() }
                val halfH = with(density) { (boxHeightDp * 0.5f).toPx() }

                Box(
                  modifier = Modifier
                    .offset {
                      IntOffset(
                        x = (floatingCenter.x - halfW).roundToInt(),
                        y = (floatingCenter.y - halfH).roundToInt()
                      )
                    }
                    .width(boxWidthDp)
                    .height(boxHeightDp)
                    .graphicsLayer {
                      scaleX = 1.10f
                      scaleY = 1.10f
                      alpha = 0.96f
                    },
                  contentAlignment = Alignment.Center
                ) {
                  if (groupSize > 1) {
                    val previewApps = draggingGroup.drop(1).take(2).reversed()
                    previewApps.forEachIndexed { idx, stackedApp ->
                      val level = previewApps.size - idx
                      val stackedBitmap = remember(
                        stackedApp.packageName,
                        stackedApp.activityName,
                        iconShape,
                        selectedIconPack
                      ) {
                        viewModel.getAppIconFast(stackedApp.packageName, stackedApp.activityName)
                      }
                      if (stackedBitmap != null) {
                        Image(
                          bitmap = stackedBitmap,
                          contentDescription = null,
                          contentScale = ContentScale.Fit,
                          modifier = Modifier
                            .size(floatingIconSize)
                            .graphicsLayer {
                              translationX = (level * 12f)
                              translationY = -(level * 10f)
                              rotationZ = (level * 6f)
                              scaleX = 0.90f
                              scaleY = 0.90f
                              alpha = 0.78f
                            }
                        )
                      }
                    }
                  }

                  val primaryBitmap = remember(
                    primaryApp.packageName,
                    primaryApp.activityName,
                    iconShape,
                    selectedIconPack
                  ) {
                    viewModel.getAppIconFast(primaryApp.packageName, primaryApp.activityName)
                  }

                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = if (showAppNames) 1.dp else 0.dp, horizontal = 2.5.dp)
                  ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                      if (primaryBitmap != null) {
                        Image(
                          bitmap = primaryBitmap,
                          contentDescription = primaryApp.label,
                          contentScale = ContentScale.Fit,
                          modifier = Modifier.size(floatingIconSize)
                        )
                      } else {
                        Box(modifier = Modifier.size(floatingIconSize))
                      }

                      if (groupSize > 1) {
                        Box(
                          modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            text = "$groupSize",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                          )
                        }
                      }
                    }
                    if (showAppNames) {
                      Spacer(modifier = Modifier.height(3.dp))
                      Text(
                        text = if (groupSize > 1) "$groupSize selecionados" else primaryApp.label,
                        color = textPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                          shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.65f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                          )
                        ),
                        modifier = Modifier.fillMaxWidth()
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
