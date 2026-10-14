package com.example.ui.components

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LauncherViewModel
import com.example.ui.icons.SvgGalleryIcon
import com.example.ui.icons.SvgIconPackCategoryIcon
import com.example.ui.icons.SvgSettingsIcon
import com.example.ui.icons.SvgShapeCategoryIcon
import com.example.ui.shape.IconCornerShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private val wallpaperPreviewMemoryCache = android.util.LruCache<String, ImageBitmap>(10)

@Composable
fun rememberBitmapAsset(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(assetPath) {
        mutableStateOf<ImageBitmap?>(wallpaperPreviewMemoryCache.get(assetPath))
    }

    if (bitmap == null) {
        LaunchedEffect(assetPath) {
            val cached = wallpaperPreviewMemoryCache.get(assetPath)
            if (cached != null) {
                bitmap = cached
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                try {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }

                    val reqWidth = 600
                    val reqHeight = 1200
                    var inSampleSize = 1
                    if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                            inSampleSize *= 2
                        }
                    }

                    options.inJustDecodeBounds = false
                    options.inSampleSize = inSampleSize

                    context.assets.open(assetPath).use { inputStream ->
                        val decoded = BitmapFactory.decodeStream(inputStream, null, options)
                        if (decoded != null) {
                            decoded.prepareToDraw()
                            val imgBitmap = decoded.asImageBitmap()
                            wallpaperPreviewMemoryCache.put(assetPath, imgBitmap)
                            bitmap = imgBitmap
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
    return bitmap
}

@Composable
fun PersonalizationScreen(
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentIconShape by viewModel.iconShape.collectAsStateWithLifecycle()
    val currentIconPack by viewModel.selectedIconPack.collectAsStateWithLifecycle()
    val iconPacksList by viewModel.iconPacks.collectAsStateWithLifecycle()
    val currentlyAppliedWallpaper by viewModel.appliedWallpaper.collectAsStateWithLifecycle()

    // Capture initial settings when opening PersonalizationScreen so "Cancelar" can cleanly revert any live changes
    val initialIconShape = remember { currentIconShape }
    val initialIconPack = remember { currentIconPack }
    val initialAppliedWallpaper = remember { currentlyAppliedWallpaper }

    var showShapePicker by remember { mutableStateOf(false) }
    var showPackPicker by remember { mutableStateOf(false) }
    val isAnyPickerOpen = showShapePicker || showPackPicker

    if (isAnyPickerOpen) {
        BackHandler {
            showShapePicker = false
            showPackPicker = false
        }
    }

    var defaultWallpapers by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasSyncedInitialPage by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { defaultWallpapers.size })

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val files = context.assets.list("wallpaper") ?: emptyArray()
                val loadedWallpapers = files
                    .filter {
                        it.endsWith(".jpg", ignoreCase = true) ||
                            it.endsWith(".png", ignoreCase = true) ||
                            it.endsWith(".jpeg", ignoreCase = true) ||
                            it.endsWith(".webp", ignoreCase = true)
                    }
                    .map { "wallpaper/$it" }
                    .sorted()
                defaultWallpapers = loadedWallpapers
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(defaultWallpapers) {
        if (!hasSyncedInitialPage && defaultWallpapers.isNotEmpty()) {
            hasSyncedInitialPage = true
            val initialIdx = defaultWallpapers.indexOf(initialAppliedWallpaper)
            if (initialIdx >= 0 && initialIdx != pagerState.currentPage) {
                pagerState.scrollToPage(initialIdx)
            }
        }
    }

    var appliedFromGalleryInSession by remember { mutableStateOf(false) }
    var isApplyingWallpaper by remember { mutableStateOf(false) }
    var pendingAppliedWallpaper by remember(currentlyAppliedWallpaper) {
        mutableStateOf(currentlyAppliedWallpaper)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val isVideo = withContext(Dispatchers.IO) {
                        com.example.cache.WallpaperBlurHelper.isVideoUri(context, uri)
                    }
                    if (isVideo) {
                        val savedVideoFile = withContext(Dispatchers.IO) {
                            val ok = com.example.wallpaper.VideoLiveWallpaperService.saveVideoFromUri(context, uri)
                            if (ok) com.example.wallpaper.VideoLiveWallpaperService.getVideoFile(context) else null
                        }
                        if (savedVideoFile != null) {
                            appliedFromGalleryInSession = true
                            viewModel.onVideoWallpaperApplied(savedVideoFile)
                            val wasAlreadyActive = com.example.wallpaper.VideoLiveWallpaperService.isSystemLiveWallpaperActive(context)
                            val launchedSystemSetter = com.example.wallpaper.VideoLiveWallpaperService.activateSystemLiveWallpaper(context)
                            if (wasAlreadyActive) {
                                Toast.makeText(context, "Live Wallpaper do sistema atualizado!", Toast.LENGTH_SHORT).show()
                            } else if (launchedSystemSetter) {
                                Toast.makeText(context, "Confirme para definir no sistema Android", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Vídeo definido como papel de parede!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Não foi possível carregar o vídeo selecionado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val wallpaperManager = WallpaperManager.getInstance(context)
                        val applied = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                wallpaperManager.setStream(stream)
                                true
                            } ?: false
                        }
                        if (applied) {
                            appliedFromGalleryInSession = true
                            viewModel.onUriWallpaperApplied(uri)
                            Toast.makeText(context, "Papel de parede aplicado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Não foi possível carregar o arquivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao aplicar papel de parede", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cancelScale = remember { Animatable(1f) }
    val doneScale = remember { Animatable(1f) }
    val galleryScale = remember { Animatable(1f) }
    val shapeScale = remember { Animatable(1f) }
    val packScale = remember { Animatable(1f) }

    val isDark = isSystemInDarkTheme()
    val blurredWallpaper by viewModel.blurredWallpaper.collectAsStateWithLifecycle()

    // Translucent gradient scrim identical to AppDrawer
    val drawerScrimBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0C0D12).copy(alpha = 0.56f),
                    Color(0xFF0E1016).copy(alpha = 0.62f),
                    Color(0xFF0A0B0F).copy(alpha = 0.68f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF7F8FC).copy(alpha = 0.60f),
                    Color(0xFFF3F5FA).copy(alpha = 0.66f),
                    Color(0xFFEEF1F7).copy(alpha = 0.72f)
                )
            )
        }
    }

    val iconBtnTint = if (isDark) Color.White else Color(0xFF1D1D1F)
    val defaultIconTint = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF1D1D1F).copy(alpha = 0.78f)
    val defaultLabelColor = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF1D1D1F).copy(alpha = 0.82f)
    val activeAccent = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

    // Smooth, lightweight transitions when opening/closing any bottom selector so the wallpaper slider scales gracefully without abrupt jumps
    val smoothLayoutTweenDp = remember { tween< androidx.compose.ui.unit.Dp >(durationMillis = 240, easing = FastOutSlowInEasing) }
    val smoothLayoutTweenFloat = remember { tween<Float>(durationMillis = 240, easing = FastOutSlowInEasing) }

    val topSpacerHeight by animateDpAsState(
        targetValue = if (isAnyPickerOpen) 86.dp else 104.dp,
        animationSpec = smoothLayoutTweenDp,
        label = "topSpacerHeight"
    )
    val pagerHorizontalPadding by animateDpAsState(
        targetValue = if (isAnyPickerOpen) 88.dp else 72.dp,
        animationSpec = smoothLayoutTweenDp,
        label = "pagerHorizontalPadding"
    )
    val pagerCardVerticalPad by animateDpAsState(
        targetValue = if (isAnyPickerOpen) 8.dp else 16.dp,
        animationSpec = smoothLayoutTweenDp,
        label = "pagerCardVerticalPad"
    )
    val pagerPickerScale by animateFloatAsState(
        targetValue = if (isAnyPickerOpen) 0.94f else 1f,
        animationSpec = smoothLayoutTweenFloat,
        label = "pagerPickerScale"
    )
    val belowPagerSpacerHeight by animateDpAsState(
        targetValue = if (isAnyPickerOpen) 16.dp else 28.dp,
        animationSpec = smoothLayoutTweenDp,
        label = "belowPagerSpacerHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .clickable(
                interactionSource = null,
                indication = null
            ) {
                if (showShapePicker || showPackPicker) {
                    showShapePicker = false
                    showPackPicker = false
                }
            }
    ) {
        // 1. Blurred current wallpaper layer identical to AppDrawer (already pre-blurred, zero runtime GPU blur overhead)
        val activeBlurredBitmap = if (isDark) blurredWallpaper?.darkModeBlur else blurredWallpaper?.lightModeBlur
        if (activeBlurredBitmap != null) {
            Image(
                bitmap = activeBlurredBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Translucent dark/light layer over the entire area identical to AppDrawer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(drawerScrimBrush)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacerHeight))

            if (defaultWallpapers.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    contentPadding = PaddingValues(horizontal = pagerHorizontalPadding),
                    pageSpacing = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = pagerCardVerticalPad)
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                val baseScale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.15f)
                                val alphaVal = 1f - (pageOffset * 0.5f).coerceIn(0f, 0.5f)
                                val combinedScale = baseScale * pagerPickerScale
                                scaleX = combinedScale
                                scaleY = combinedScale
                                alpha = alphaVal
                            }
                            .shadow(16.dp, RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF2C2C2C))
                    ) {
                        val pageWallpaperId = defaultWallpapers[page]
                        val bitmap = rememberBitmapAsset(pageWallpaperId)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Wallpaper $page",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        val isThisWallpaperInUse = !appliedFromGalleryInSession && pendingAppliedWallpaper == pageWallpaperId
                        val applyPillScale = remember { Animatable(1f) }
                        val applyPillBg = if (isDark) Color(0x33FFFFFF) else Color(0x5EFFFFFF)
                        val applyPillBorder = remember {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.18f)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                                .graphicsLayer {
                                    scaleX = applyPillScale.value
                                    scaleY = applyPillScale.value
                                }
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color.Black.copy(alpha = 0.18f),
                                    spotColor = Color.Black.copy(alpha = 0.25f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(applyPillBg)
                                .border(
                                    width = 1.dp,
                                    brush = applyPillBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .pointerInput(pageWallpaperId, isThisWallpaperInUse) {
                                    detectTapGestures(
                                        onPress = {
                                            if (!isThisWallpaperInUse) {
                                                scope.launch {
                                                    applyPillScale.animateTo(0.93f, spring(stiffness = Spring.StiffnessMedium))
                                                }
                                                tryAwaitRelease()
                                                applyPillScale.animateTo(
                                                    1f,
                                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                )
                                            }
                                        },
                                        onTap = {
                                            if (!isThisWallpaperInUse) {
                                                appliedFromGalleryInSession = false
                                                pendingAppliedWallpaper = pageWallpaperId
                                            }
                                        }
                                    )
                                }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                                .testTag("wallpaper_apply_button_$page"),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isThisWallpaperInUse,
                                transitionSpec = {
                                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.92f, animationSpec = tween(150)))
                                        .togetherWith(fadeOut(tween(110)) + scaleOut(targetScale = 0.92f, animationSpec = tween(110)))
                                },
                                label = "wallpaperStatusLabel_$page"
                            ) { inUse ->
                                Text(
                                    text = if (inUse) "Em uso" else "Aplicar",
                                    color = iconBtnTint,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum papel de parede encontrado.",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(belowPagerSpacerHeight))

            // Ultra-Clean Unified Segmented Capsule Selector Style:
            // Both Shape and Icon Pack selectors share a cohesive, refined floating glass dock
            // with a smooth sliding selection pill, tactile press physics, and zero visual clutter.
            val pickerEnterTransition = remember {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    expandVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Bottom
                    ) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
            }
            val pickerExitTransition = remember {
                fadeOut(tween(140, easing = FastOutSlowInEasing)) +
                    shrinkVertically(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Bottom
                    ) +
                    scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
            }

            val activePickerMode = when {
                showShapePicker -> 1
                showPackPicker -> 2
                else -> 0
            }

            val dockBg = if (isDark) Color(0xD916171D) else Color(0xE8FFFFFF)
            val dockBorder = remember(isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.32f else 0.85f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.28f)
                    )
                )
            }
            val activeThumbBg = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.11f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = 0.16f),
                        activeAccent.copy(alpha = 0.09f)
                    )
                )
            }
            val activeThumbBorder = if (isDark) {
                Color.White.copy(alpha = 0.28f)
            } else {
                activeAccent.copy(alpha = 0.38f)
            }

            AnimatedVisibility(
                visible = activePickerMode != 0,
                enter = pickerEnterTransition,
                exit = pickerExitTransition
            ) {
                AnimatedContent(
                    targetState = activePickerMode,
                    transitionSpec = {
                        (fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(160, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(tween(110, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = tween(110, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "pickerModeSwitch"
                ) { mode ->
                    if (mode == 1) {
                        // Shape Selector: Precision Segmented Capsule with Sliding Glass Pill & Micro Active Dot
                        val shapes = remember { listOf("sammy", "circle", "rounded_square") }
                        val selectedIndex = shapes.indexOf(currentIconShape).coerceAtLeast(0)
                        val slotWidth = 48.dp
                        val slotHeight = 40.dp
                        val slotSpacing = 6.dp
                        val indicatorOffsetX by animateDpAsState(
                            targetValue = (slotWidth + slotSpacing) * selectedIndex,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "shapeSlidingThumbOffset"
                        )

                        Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.22f),
                                    spotColor = Color.Black.copy(alpha = 0.30f)
                                )
                                .clip(CircleShape)
                                .background(dockBg)
                                .border(
                                    width = 0.8.dp,
                                    brush = dockBorder,
                                    shape = CircleShape
                                )
                                .clickable(interactionSource = null, indication = null) {}
                                .padding(horizontal = 5.dp, vertical = 5.dp)
                        ) {
                            // Sliding luminous glass indicator pill
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffsetX)
                                    .width(slotWidth)
                                    .height(slotHeight)
                                    .clip(CircleShape)
                                    .background(activeThumbBg)
                                    .border(
                                        width = 0.8.dp,
                                        color = activeThumbBorder,
                                        shape = CircleShape
                                    )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(slotSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                shapes.forEach { key ->
                                    val selected = currentIconShape == key
                                    val tapScale = remember { Animatable(1f) }
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (selected) 1.06f else 0.92f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "shapeIconScale_$key"
                                    )
                                    val fillAlpha by animateFloatAsState(
                                        targetValue = if (selected) 0.22f else 0f,
                                        animationSpec = tween(160),
                                        label = "shapeFillAlpha_$key"
                                    )
                                    val glyphColor by animateColorAsState(
                                        targetValue = if (selected) {
                                            if (isDark) Color.White else activeAccent
                                        } else {
                                            if (isDark) Color.White.copy(alpha = 0.48f) else Color(0xFF1D1D1F).copy(alpha = 0.45f)
                                        },
                                        animationSpec = tween(160),
                                        label = "shapeGlyphColor_$key"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(slotWidth)
                                            .height(slotHeight)
                                            .graphicsLayer {
                                                scaleX = tapScale.value
                                                scaleY = tapScale.value
                                            }
                                            .clip(CircleShape)
                                            .pointerInput(key) {
                                                detectTapGestures(
                                                    onPress = {
                                                        scope.launch {
                                                            tapScale.animateTo(0.88f, spring(stiffness = Spring.StiffnessMedium))
                                                        }
                                                        tryAwaitRelease()
                                                        tapScale.animateTo(
                                                            1f,
                                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                        )
                                                    },
                                                    onTap = {
                                                        viewModel.setIconShape(key)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                        ) {
                                            val w = size.width
                                            val strokeWidthPx = if (selected) 2.dp.toPx() else 1.6.dp.toPx()
                                            val strokeStyle = Stroke(width = strokeWidthPx)
                                            when (key) {
                                                "sammy" -> {
                                                    val path = IconCornerShape.createComposeSammyPath(w, w)
                                                    if (fillAlpha > 0.01f) {
                                                        drawPath(path = path, color = glyphColor.copy(alpha = fillAlpha))
                                                    }
                                                    drawPath(path = path, color = glyphColor, style = strokeStyle)
                                                }
                                                "circle" -> {
                                                    if (fillAlpha > 0.01f) {
                                                        drawCircle(color = glyphColor.copy(alpha = fillAlpha), radius = w / 2f)
                                                    }
                                                    drawCircle(color = glyphColor, radius = w / 2f, style = strokeStyle)
                                                }
                                                else -> {
                                                    val cr = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx())
                                                    if (fillAlpha > 0.01f) {
                                                        drawRoundRect(color = glyphColor.copy(alpha = fillAlpha), cornerRadius = cr)
                                                    }
                                                    drawRoundRect(color = glyphColor, cornerRadius = cr, style = strokeStyle)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (mode == 2) {
                        // Icon Pack Selector: Cohesive Minimalist Glass Dock with Sliding/Glowing Selection Pill
                        val slotWidth = 48.dp
                        val slotHeight = 40.dp
                        val slotSpacing = 6.dp
                        val isCompactPackList = iconPacksList.size <= 5
                        val selectedPackIndex = iconPacksList.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                        val packIndicatorOffsetX by animateDpAsState(
                            targetValue = (slotWidth + slotSpacing) * selectedPackIndex,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "packSlidingThumbOffset"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp)
                                .widthIn(max = 310.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.22f),
                                    spotColor = Color.Black.copy(alpha = 0.30f)
                                )
                                .clip(CircleShape)
                                .background(dockBg)
                                .border(
                                    width = 0.8.dp,
                                    brush = dockBorder,
                                    shape = CircleShape
                                )
                                .clickable(interactionSource = null, indication = null) {}
                                .padding(horizontal = 5.dp, vertical = 5.dp)
                        ) {
                            if (isCompactPackList) {
                                // Sliding indicator pill when packs fit in a single compact row
                                Box(
                                    modifier = Modifier
                                        .offset(x = packIndicatorOffsetX)
                                        .width(slotWidth)
                                        .height(slotHeight)
                                        .clip(CircleShape)
                                        .background(activeThumbBg)
                                        .border(
                                            width = 0.8.dp,
                                            color = activeThumbBorder,
                                            shape = CircleShape
                                        )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(slotSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    iconPacksList.forEach { pack ->
                                        val isSelected = pack.isSelected
                                        val tapScale = remember { Animatable(1f) }
                                        val iconScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.06f else 0.90f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "packIconScale_${pack.packageName}"
                                        )
                                        val iconAlpha by animateFloatAsState(
                                            targetValue = if (isSelected) 1f else 0.62f,
                                            animationSpec = tween(160),
                                            label = "packIconAlpha_${pack.packageName}"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .width(slotWidth)
                                                .height(slotHeight)
                                                .graphicsLayer {
                                                    scaleX = tapScale.value
                                                    scaleY = tapScale.value
                                                }
                                                .clip(CircleShape)
                                                .pointerInput(pack.packageName) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            scope.launch {
                                                                tapScale.animateTo(0.88f, spring(stiffness = Spring.StiffnessMedium))
                                                            }
                                                            tryAwaitRelease()
                                                            tapScale.animateTo(
                                                                1f,
                                                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                            )
                                                        },
                                                        onTap = {
                                                            viewModel.setIconPack(pack.packageName)
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (pack.packageName == null) {
                                                val defaultTint by animateColorAsState(
                                                    targetValue = if (isSelected) {
                                                        if (isDark) Color.White else activeAccent
                                                    } else {
                                                        if (isDark) Color.White.copy(alpha = 0.52f) else Color(0xFF1D1D1F).copy(alpha = 0.48f)
                                                    },
                                                    animationSpec = tween(160),
                                                    label = "defaultPackIconTint"
                                                )
                                                Icon(
                                                    imageVector = SvgSettingsIcon,
                                                    contentDescription = pack.label,
                                                    tint = defaultTint,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            scaleX = iconScale
                                                            scaleY = iconScale
                                                        }
                                                )
                                            } else {
                                                var packIcon by remember(pack.packageName) { mutableStateOf<Drawable?>(null) }
                                                LaunchedEffect(pack.packageName) {
                                                    withContext(Dispatchers.IO) {
                                                        try {
                                                            packIcon = context.packageManager.getApplicationIcon(pack.packageName)
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                                if (packIcon != null) {
                                                    Canvas(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .graphicsLayer {
                                                                scaleX = iconScale
                                                                scaleY = iconScale
                                                                alpha = iconAlpha
                                                            }
                                                            .clip(CircleShape)
                                                    ) {
                                                        drawIntoCanvas { composeCanvas ->
                                                            try {
                                                                packIcon!!.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                                                                packIcon!!.draw(composeCanvas.nativeCanvas)
                                                            } catch (_: Throwable) {}
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Scrollable capsule row when more than 5 icon packs are installed
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(slotSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(iconPacksList, key = { it.packageName ?: "default_pack" }) { pack ->
                                        val isSelected = pack.isSelected
                                        val tapScale = remember { Animatable(1f) }
                                        val iconScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.06f else 0.90f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "packScrollScale_${pack.packageName}"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .width(slotWidth)
                                                .height(slotHeight)
                                                .graphicsLayer {
                                                    scaleX = tapScale.value
                                                    scaleY = tapScale.value
                                                }
                                                .clip(CircleShape)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier
                                                            .background(activeThumbBg)
                                                            .border(0.8.dp, activeThumbBorder, CircleShape)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .pointerInput(pack.packageName) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            scope.launch {
                                                                tapScale.animateTo(0.88f, spring(stiffness = Spring.StiffnessMedium))
                                                            }
                                                            tryAwaitRelease()
                                                            tapScale.animateTo(
                                                                1f,
                                                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                            )
                                                        },
                                                        onTap = {
                                                            viewModel.setIconPack(pack.packageName)
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (pack.packageName == null) {
                                                val defaultTint by animateColorAsState(
                                                    targetValue = if (isSelected) {
                                                        if (isDark) Color.White else activeAccent
                                                    } else {
                                                        if (isDark) Color.White.copy(alpha = 0.52f) else Color(0xFF1D1D1F).copy(alpha = 0.48f)
                                                    },
                                                    animationSpec = tween(160),
                                                    label = "defaultPackScrollTint"
                                                )
                                                Icon(
                                                    imageVector = SvgSettingsIcon,
                                                    contentDescription = pack.label,
                                                    tint = defaultTint,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            scaleX = iconScale
                                                            scaleY = iconScale
                                                        }
                                                )
                                            } else {
                                                var packIcon by remember(pack.packageName) { mutableStateOf<Drawable?>(null) }
                                                LaunchedEffect(pack.packageName) {
                                                    withContext(Dispatchers.IO) {
                                                        try {
                                                            packIcon = context.packageManager.getApplicationIcon(pack.packageName)
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                                if (packIcon != null) {
                                                    Canvas(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .graphicsLayer {
                                                                scaleX = iconScale
                                                                scaleY = iconScale
                                                                alpha = if (isSelected) 1f else 0.62f
                                                            }
                                                            .clip(CircleShape)
                                                    ) {
                                                        drawIntoCanvas { composeCanvas ->
                                                            try {
                                                                packIcon!!.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                                                                packIcon!!.draw(composeCanvas.nativeCanvas)
                                                            } catch (_: Throwable) {}
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            val shapeIconTint by animateColorAsState(
                targetValue = if (showShapePicker) activeAccent else defaultIconTint,
                animationSpec = tween(180),
                label = "shapeIconTint"
            )
            val packIconTint by animateColorAsState(
                targetValue = if (showPackPicker) activeAccent else defaultIconTint,
                animationSpec = tween(180),
                label = "packIconTint"
            )
            val shapeLabelColor by animateColorAsState(
                targetValue = if (showShapePicker) activeAccent else defaultLabelColor,
                animationSpec = tween(180),
                label = "shapeLabelColor"
            )
            val packLabelColor by animateColorAsState(
                targetValue = if (showPackPicker) activeAccent else defaultLabelColor,
                animationSpec = tween(180),
                label = "packLabelColor"
            )

            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // 1. Galeria
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                scaleX = galleryScale.value
                                scaleY = galleryScale.value
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0x30FFFFFF) else Color(0x5EFFFFFF)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.18f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        galleryScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessMedium))
                                        tryAwaitRelease()
                                        galleryScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                                    },
                                    onTap = {
                                        showShapePicker = false
                                        showPackPicker = false
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SvgGalleryIcon,
                            contentDescription = "Selecionar imagem da galeria",
                            tint = defaultIconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Galeria",
                        color = defaultLabelColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // 2. Formato dos Ícones Button
                val shapeButtonBg by animateColorAsState(
                    targetValue = if (showShapePicker) {
                        if (isDark) Color(0x380A84FF) else Color(0x44007AFF)
                    } else {
                        if (isDark) Color(0x30FFFFFF) else Color(0x5EFFFFFF)
                    },
                    animationSpec = tween(180),
                    label = "shapeButtonBg"
                )
                val shapeButtonBorder = if (showShapePicker) {
                    Brush.linearGradient(
                        listOf(
                            activeAccent.copy(alpha = 0.85f),
                            activeAccent.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                scaleX = shapeScale.value
                                scaleY = shapeScale.value
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(shapeButtonBg)
                            .border(
                                width = 1.dp,
                                brush = shapeButtonBorder,
                                shape = CircleShape
                            )
                            .pointerInput(showShapePicker) {
                                detectTapGestures(
                                    onPress = {
                                        shapeScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessMedium))
                                        tryAwaitRelease()
                                        shapeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                                    },
                                    onTap = {
                                        showPackPicker = false
                                        showShapePicker = !showShapePicker
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SvgShapeCategoryIcon,
                            contentDescription = "Formato dos ícones",
                            tint = shapeIconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Formato",
                        color = shapeLabelColor,
                        fontSize = 12.5.sp,
                        fontWeight = if (showShapePicker) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // 3. Pacote de Ícones Button
                val packButtonBg by animateColorAsState(
                    targetValue = if (showPackPicker) {
                        if (isDark) Color(0x380A84FF) else Color(0x44007AFF)
                    } else {
                        if (isDark) Color(0x30FFFFFF) else Color(0x5EFFFFFF)
                    },
                    animationSpec = tween(180),
                    label = "packButtonBg"
                )
                val packButtonBorder = if (showPackPicker) {
                    Brush.linearGradient(
                        listOf(
                            activeAccent.copy(alpha = 0.85f),
                            activeAccent.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                scaleX = packScale.value
                                scaleY = packScale.value
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(packButtonBg)
                            .border(
                                width = 1.dp,
                                brush = packButtonBorder,
                                shape = CircleShape
                            )
                            .pointerInput(showPackPicker) {
                                detectTapGestures(
                                    onPress = {
                                        packScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessMedium))
                                        tryAwaitRelease()
                                        packScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                                    },
                                    onTap = {
                                        showShapePicker = false
                                        showPackPicker = !showPackPicker
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SvgIconPackCategoryIcon,
                            contentDescription = "Pacote de ícones",
                            tint = packIconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pacotes",
                        color = packLabelColor,
                        fontSize = 12.5.sp,
                        fontWeight = if (showPackPicker) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Top bar pill buttons: "Cancelar" (top-left) and "Concluir" (top-right)
        val topPillBg = if (isDark) Color(0x33FFFFFF) else Color(0x5EFFFFFF)
        val topPillBorder = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
        }

        // Pill-shaped "Cancelar" button at top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp)
                .graphicsLayer {
                    scaleX = cancelScale.value
                    scaleY = cancelScale.value
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(50))
                .background(topPillBg)
                .border(
                    width = 1.dp,
                    brush = topPillBorder,
                    shape = RoundedCornerShape(50)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            scope.launch {
                                cancelScale.animateTo(0.93f, spring(stiffness = Spring.StiffnessMedium))
                            }
                            tryAwaitRelease()
                            cancelScale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        },
                        onTap = {
                            showShapePicker = false
                            showPackPicker = false
                            pendingAppliedWallpaper = initialAppliedWallpaper
                            if (currentIconShape != initialIconShape) {
                                viewModel.setIconShape(initialIconShape)
                            }
                            if (currentIconPack != initialIconPack) {
                                viewModel.setIconPack(initialIconPack)
                            }
                            onClose()
                        }
                    )
                }
                .padding(horizontal = 18.dp, vertical = 9.dp)
                .testTag("personalization_cancel_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancelar",
                color = iconBtnTint,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Pill-shaped "Concluir" button at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp)
                .graphicsLayer {
                    scaleX = doneScale.value
                    scaleY = doneScale.value
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(50))
                .background(topPillBg)
                .border(
                    width = 1.dp,
                    brush = topPillBorder,
                    shape = RoundedCornerShape(50)
                )
                .pointerInput(defaultWallpapers, appliedFromGalleryInSession, isApplyingWallpaper, pendingAppliedWallpaper, currentlyAppliedWallpaper) {
                    detectTapGestures(
                        onPress = {
                            scope.launch {
                                doneScale.animateTo(0.93f, spring(stiffness = Spring.StiffnessMedium))
                            }
                            tryAwaitRelease()
                            doneScale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        },
                        onTap = {
                            if (isApplyingWallpaper) return@detectTapGestures
                            showShapePicker = false
                            showPackPicker = false
                            val targetWallpaperId = pendingAppliedWallpaper ?: ""

                            if (!appliedFromGalleryInSession &&
                                targetWallpaperId.isNotEmpty() &&
                                targetWallpaperId != currentlyAppliedWallpaper &&
                                defaultWallpapers.contains(targetWallpaperId)
                            ) {
                                isApplyingWallpaper = true
                                scope.launch {
                                    try {
                                        val wallpaperManager = WallpaperManager.getInstance(context)
                                        withContext(Dispatchers.IO) {
                                            context.assets.open(targetWallpaperId).use { inputStream ->
                                                wallpaperManager.setStream(inputStream)
                                            }
                                        }
                                        viewModel.setAppliedWallpaper(targetWallpaperId)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Erro ao aplicar papel de parede",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isApplyingWallpaper = false
                                        onClose()
                                    }
                                }
                            } else {
                                onClose()
                            }
                        }
                    )
                }
                .padding(horizontal = 18.dp, vertical = 9.dp)
                .testTag("personalization_done_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Concluir",
                color = iconBtnTint,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

