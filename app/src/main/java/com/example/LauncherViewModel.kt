package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cache.AppMetadataCache
import com.example.cache.BlurredWallpaperPair
import com.example.cache.IconDiskCache
import com.example.cache.WallpaperBlurHelper
import com.example.model.AppInfo
import com.example.model.IconPackInfo
import com.example.ui.shape.IconCornerShape
import com.example.wallpaper.VideoLiveWallpaperService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val TAG = "LauncherViewModel"
private const val ICON_SIZE = 144
private val ALLOWED_ICON_SIZE_OPTIONS = floatArrayOf(0.50f, 0.80f, 1.00f, 1.20f)

private fun snapToAllowedIconSizeProgress(value: Float): Float {
    val clamped = value.coerceIn(0.50f, 1.20f)
    var closest = 1.00f
    var minDiff = Float.MAX_VALUE
    for (option in ALLOWED_ICON_SIZE_OPTIONS) {
        val diff = kotlin.math.abs(clamped - option)
        if (diff < minDiff) {
            minDiff = diff
            closest = option
        }
    }
    return closest
}

@Stable
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    private val launcherApps: LauncherApps? = try {
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    } catch (_: Throwable) {
        null
    }
    private val currentPackageName: String = application.packageName
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (!packageName.isNullOrEmpty() && packageName != currentPackageName) {
                onPackageRemoved(packageName)
            }
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (!packageName.isNullOrEmpty() && packageName != currentPackageName) {
                onPackageAdded(packageName)
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (!packageName.isNullOrEmpty() && packageName != currentPackageName) {
                onPackageChanged(packageName)
            }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            if (!packageNames.isNullOrEmpty()) {
                for (pkg in packageNames) {
                    if (pkg.isNotEmpty() && pkg != currentPackageName) {
                        if (replacing) onPackageChanged(pkg) else onPackageAdded(pkg)
                    }
                }
            }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            if (!replacing && !packageNames.isNullOrEmpty()) {
                for (pkg in packageNames) {
                    if (pkg.isNotEmpty() && pkg != currentPackageName) {
                        onPackageRemoved(pkg)
                    }
                }
            }
        }

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) {
            if (!packageNames.isNullOrEmpty()) {
                for (pkg in packageNames) {
                    if (pkg.isNotEmpty() && pkg != currentPackageName) {
                        onPackageChanged(pkg)
                    }
                }
            }
        }

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) {
            if (!packageNames.isNullOrEmpty()) {
                for (pkg in packageNames) {
                    if (pkg.isNotEmpty() && pkg != currentPackageName) {
                        onPackageChanged(pkg)
                    }
                }
            }
        }
    }

    // L1 (RAM) & L2 (Disk) Icon Cache
    private val iconDiskCache = IconDiskCache(application)

    // Persistent App Metadata Cache (fast startup, zero blocking on main thread)
    private val appMetadataCache = AppMetadataCache(application)

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception handled", throwable)
    }

    private val _closeOverlaysTrigger = MutableStateFlow(0)
    val closeOverlaysTrigger: StateFlow<Int> = _closeOverlaysTrigger

    private val _rawApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Persistence Settings
    private val _showAppNames = MutableStateFlow(true)
    val showAppNames: StateFlow<Boolean> = _showAppNames

    private val _iconShape = MutableStateFlow("circle")
    val iconShape: StateFlow<String> = _iconShape

    private val _selectedIconPack = MutableStateFlow<String?>(null)
    val selectedIconPack: StateFlow<String?> = _selectedIconPack

    private val _iconSizeProgress = MutableStateFlow(1.00f)
    val iconSizeProgress: StateFlow<Float> = _iconSizeProgress

    private val _drawerStyle = MutableStateFlow("vertical")
    val drawerStyle: StateFlow<String> = _drawerStyle

    // Custom 4x6 paged slot arrangement for horizontal drawer style (each page has 24 nullable appKey slots)
    private val _horizontalDrawerPages = MutableStateFlow<List<List<String?>>>(emptyList())
    val horizontalDrawerPages: StateFlow<List<List<String?>>> = _horizontalDrawerPages

    // Ordered Dock App Keys state (preserves exact insertion order)
    private val _dockAppKeys = MutableStateFlow<Set<String>>(emptySet())
    val dockAppKeys: StateFlow<Set<String>> = _dockAppKeys

    // Hidden App Keys state
    private val _hiddenAppKeys = MutableStateFlow<Set<String>>(emptySet())
    val hiddenAppKeys: StateFlow<Set<String>> = _hiddenAppKeys

    // Locked App Keys state (Bloqueio de Aplicativos)
    private val _lockedAppKeys = MutableStateFlow<Set<String>>(emptySet())
    val lockedAppKeys: StateFlow<Set<String>> = _lockedAppKeys

    private fun readOrderedStringSet(orderedKey: String, legacySetKey: String): LinkedHashSet<String> {
        val rawOrdered = prefs.getString(orderedKey, null)
        if (rawOrdered != null) {
            val result = LinkedHashSet<String>()
            for (token in rawOrdered.split("|")) {
                val trimmed = token.trim()
                if (trimmed.isNotEmpty()) {
                    result.add(trimmed)
                }
            }
            return result
        }
        val legacy = prefs.getStringSet(legacySetKey, null)
        val migrated = LinkedHashSet<String>()
        if (legacy != null) {
            for (item in legacy) {
                if (item.isNotEmpty()) migrated.add(item)
            }
            writeOrderedStringSet(orderedKey, legacySetKey, migrated)
        }
        return migrated
    }

    private fun writeOrderedStringSet(orderedKey: String, legacySetKey: String, values: Set<String>) {
        val serialized = values.filter { it.isNotEmpty() }.joinToString("|")
        prefs.edit()
            .putString(orderedKey, serialized)
            .putStringSet(legacySetKey, HashSet(values))
            .apply()
    }

    // Applied Wallpaper state
    private val _appliedWallpaper = MutableStateFlow<String?>(
        prefs.getString("applied_wallpaper", "wallpaper/wallpaper_1.jpg")
    )
    val appliedWallpaper: StateFlow<String?> = _appliedWallpaper

    private val _isVideoWallpaperActive = MutableStateFlow(
        prefs.getBoolean("is_video_wallpaper_active", false) &&
            VideoLiveWallpaperService.hasActiveVideoFile(application)
    )
    val isVideoWallpaperActive: StateFlow<Boolean> = _isVideoWallpaperActive

    private val _videoWallpaperVersion = MutableStateFlow(0)
    val videoWallpaperVersion: StateFlow<Int> = _videoWallpaperVersion

    private val _isSystemLiveWallpaperServiceActive = MutableStateFlow(
        VideoLiveWallpaperService.isSystemLiveWallpaperActive(application)
    )
    val isSystemLiveWallpaperServiceActive: StateFlow<Boolean> = _isSystemLiveWallpaperServiceActive

    private val _blurredWallpaper = MutableStateFlow<BlurredWallpaperPair?>(null)
    val blurredWallpaper: StateFlow<BlurredWallpaperPair?> = _blurredWallpaper
    private var wallpaperBlurJob: Job? = null

    fun setAppliedWallpaper(wallpaperId: String) {
        _appliedWallpaper.value = wallpaperId
        _isVideoWallpaperActive.value = false
        prefs.edit()
            .putString("applied_wallpaper", wallpaperId)
            .putBoolean("is_video_wallpaper_active", false)
            .apply()
        VideoLiveWallpaperService.clearVideoFile(getApplication())
        onAssetWallpaperApplied(wallpaperId)
    }

    fun onAssetWallpaperApplied(assetPath: String) {
        wallpaperBlurJob?.cancel()
        wallpaperBlurJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val app = getApplication<Application>()
            WallpaperBlurHelper.saveCustomWallpaperFromAsset(app, assetPath)
            _blurredWallpaper.value = WallpaperBlurHelper.loadBlurredWallpaperPair(app)
        }
    }

    fun onUriWallpaperApplied(uri: Uri) {
        _isVideoWallpaperActive.value = false
        prefs.edit().putBoolean("is_video_wallpaper_active", false).apply()
        wallpaperBlurJob?.cancel()
        wallpaperBlurJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val app = getApplication<Application>()
            VideoLiveWallpaperService.clearVideoFile(app)
            WallpaperBlurHelper.saveCustomWallpaperFromUri(app, uri)
            _blurredWallpaper.value = WallpaperBlurHelper.loadBlurredWallpaperPair(app)
        }
    }

    fun onVideoWallpaperApplied(videoFile: File) {
        _appliedWallpaper.value = "custom_video"
        _isVideoWallpaperActive.value = true
        _videoWallpaperVersion.value = _videoWallpaperVersion.value + 1
        prefs.edit()
            .putString("applied_wallpaper", "custom_video")
            .putBoolean("is_video_wallpaper_active", true)
            .apply()
        wallpaperBlurJob?.cancel()
        wallpaperBlurJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val app = getApplication<Application>()
            WallpaperBlurHelper.saveCustomWallpaperFromVideoFile(app, videoFile)
            _blurredWallpaper.value = WallpaperBlurHelper.loadBlurredWallpaperPair(app)
            _isSystemLiveWallpaperServiceActive.value =
                VideoLiveWallpaperService.isSystemLiveWallpaperActive(app)
        }
    }

    fun refreshWallpaper() {
        wallpaperBlurJob?.cancel()
        wallpaperBlurJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val app = getApplication<Application>()
            _isSystemLiveWallpaperServiceActive.value =
                VideoLiveWallpaperService.isSystemLiveWallpaperActive(app)
            _blurredWallpaper.value = WallpaperBlurHelper.loadBlurredWallpaperPair(app)
        }
    }

    val allApps: StateFlow<List<AppInfo>> = _rawApps

    val dockApps: StateFlow<List<AppInfo>> = combine(_rawApps, _dockAppKeys, _hiddenAppKeys) { apps, keys, hiddenKeys ->
        if (keys.isEmpty() || apps.isEmpty()) {
            emptyList()
        } else {
            val appMap = HashMap<String, AppInfo>(apps.size * 2)
            for (app in apps) {
                val fullKey = "${app.packageName}/${app.activityName}"
                if (!hiddenKeys.contains(fullKey) && !hiddenKeys.contains(app.packageName)) {
                    appMap[fullKey] = app
                    appMap.putIfAbsent(app.packageName, app)
                }
            }
            keys.take(4).mapNotNull { key -> appMap[key] }.distinctBy { "${it.packageName}/${it.activityName}" }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Cache for loaded icon pack appfilter mapping
    private var cachedIconPackPackageName: String? = null
    private var cachedAppFilterMap: Map<String, String>? = null

    // Thread-safe in-memory cache for processed AppInfo objects (key: packageName/activityName)
    private val appCache = ConcurrentHashMap<String, AppInfo>()

    // Deduplication map for in-flight icon rendering coroutines to avoid duplicate work
    private val inFlightIconJobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    private var loadAppsJob: Job? = null
    private var loadIconPacksJob: Job? = null
    private var preloadIconsJob: Job? = null

    // Ultra-fast in-memory search index with zero PackageManager overhead
    val filteredApps: StateFlow<List<AppInfo>> = combine(_rawApps, _searchQuery, _hiddenAppKeys) { apps, rawQuery, hiddenKeys ->
        val visibleApps = if (hiddenKeys.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                val key = "${app.packageName}/${app.activityName}"
                !hiddenKeys.contains(key) && !hiddenKeys.contains(app.packageName)
            }
        }

        val query = AppInfo.normalize(rawQuery.trim())
        if (query.isEmpty()) {
            visibleApps
        } else {
            // Instant in-memory token and prefix filtering using pre-normalized strings
            visibleApps.filter { app ->
                app.normalizedLabel.contains(query) || app.packageName.contains(query)
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // StateFlow for installed icon packs loaded asynchronously
    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val iconPacks: StateFlow<List<IconPackInfo>> = _iconPacks

    init {
        Log.d(TAG, "Initializing LauncherViewModel")
        _showAppNames.value = prefs.getBoolean("show_app_names", true)
        _iconShape.value = prefs.getString("icon_shape", "circle") ?: "circle"
        _selectedIconPack.value = prefs.getString("selected_icon_pack", null)
        val savedProgress = prefs.getFloat("icon_size_progress_v3", 1.00f)
        _iconSizeProgress.value = snapToAllowedIconSizeProgress(savedProgress)
        _drawerStyle.value = prefs.getString("drawer_style", "vertical") ?: "vertical"
        val primaryPagesRaw = prefs.getString("horizontal_drawer_pages_v1", null)
        val parsedPrimary = parseHorizontalDrawerPages(primaryPagesRaw)
        _horizontalDrawerPages.value = if (parsedPrimary.isNotEmpty() || primaryPagesRaw.isNullOrBlank()) {
            parsedPrimary
        } else {
            parseHorizontalDrawerPages(prefs.getString("horizontal_drawer_pages_v1_backup", null))
        }
        _dockAppKeys.value = readOrderedStringSet("dock_apps_ordered_v2", "dock_apps")
        _hiddenAppKeys.value = readOrderedStringSet("hidden_apps_ordered_v2", "hidden_apps")
        _lockedAppKeys.value = readOrderedStringSet("locked_apps_ordered_v2", "locked_apps")

        // 1. Instant Cache Initialization: Load persisted app metadata immediately
        // UI renders instantly on frame 1 without waiting for PackageManager.
        val cachedApps = appMetadataCache.loadFromDisk()
        if (!cachedApps.isNullOrEmpty()) {
            Log.d(TAG, "Restored ${cachedApps.size} apps instantly from persistent cache")
            _rawApps.value = cachedApps
            _isLoading.value = false
            for (app in cachedApps) {
                appCache["${app.packageName}/${app.activityName}"] = app
            }
            preloadDockIcons(cachedApps)
            preloadIconsInBackground(cachedApps)
        }

        // 2. Perform background reconciliation to detect newly installed, uninstalled or updated apps
        try {
            launcherApps?.registerCallback(launcherAppsCallback)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register LauncherApps.Callback", e)
        }
        loadApps()
        loadIconPacks()
        refreshWallpaper()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            launcherApps?.unregisterCallback(launcherAppsCallback)
        } catch (_: Throwable) {}
    }

    private fun preloadDockIcons(apps: List<AppInfo>) {
        val dockKeys = _dockAppKeys.value
        if (dockKeys.isEmpty() || apps.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val lookup = HashMap<String, AppInfo>(apps.size * 2)
            for (app in apps) {
                lookup["${app.packageName}/${app.activityName}"] = app
                lookup.putIfAbsent(app.packageName, app)
            }
            for (key in dockKeys.take(4)) {
                val app = lookup[key]
                if (app != null) {
                    loadAppIconAsync(app.packageName, app.activityName)
                }
            }
        }
    }

    private fun preloadIconsInBackground(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        preloadIconsJob?.cancel()
        preloadIconsJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            // Stage 1: Immediately warm up the first 28 apps (first full page + buffer)
            val priorityApps = apps.take(28)
            for (app in priorityApps) {
                if (getAppIconFast(app.packageName, app.activityName) == null) {
                    loadAppIconAsync(app.packageName, app.activityName)
                }
            }
            // Stage 2: Adaptively warm up additional apps based on device RAM tier with cooperative yield
            val maxWarmup = apps.size.coerceAtMost(iconDiskCache.recommendedWarmupCount)
            if (maxWarmup > 28) {
                for (i in 28 until maxWarmup) {
                    val app = apps[i]
                    if (getAppIconFast(app.packageName, app.activityName) == null) {
                        loadAppIconAsync(app.packageName, app.activityName)
                        yield()
                    }
                }
            }
            // Stage 3: Clean up any obsolete icon shape/pack variants on disk so storage stays minimal
            iconDiskCache.pruneObsoleteVariants(_iconShape.value, _selectedIconPack.value)
        }
    }

    @Synchronized
    private fun getCachedAppFilterMap(packageName: String): Map<String, String> {
        if (cachedIconPackPackageName == packageName) {
            cachedAppFilterMap?.let { return it }
        }
        val map = loadAppFilterMap(packageName)
        cachedIconPackPackageName = packageName
        cachedAppFilterMap = map
        return map
    }

    private fun loadAppFilterMap(packageName: String): Map<String, String> {
        val map = HashMap<String, String>()
        try {
            val res = packageManager.getResourcesForApplication(packageName)
            val assets = res.assets
            var parser: XmlPullParser? = null
            
            try {
                assets.open("appfilter.xml").use { isStream ->
                    val factory = XmlPullParserFactory.newInstance()
                    parser = factory.newPullParser()
                    parser!!.setInput(isStream, "UTF-8")
                    parseAppFilterParser(parser!!, map)
                }
            } catch (e: Exception) {
                val resId = res.getIdentifier("appfilter", "xml", packageName)
                if (resId != 0) {
                    parser = res.getXml(resId)
                    parseAppFilterParser(parser!!, map)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error loading appfilter.xml for $packageName: ${e.message}")
        }
        return map
    }

    private fun parseAppFilterParser(parser: XmlPullParser, map: HashMap<String, String>) {
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        if (component.startsWith("ComponentInfo{") && component.endsWith("}")) {
                            val clean = component.substring(14, component.length - 1)
                            map[clean] = drawable
                            val parts = clean.split("/")
                            if (parts.isNotEmpty()) {
                                map[parts[0]] = drawable
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Throwable) {}
    }

    private val pathCache = ConcurrentHashMap<String, Path>()

    private fun getMaskedIcon(original: Drawable, appPackageName: String, appActivityName: String): Bitmap {
        val size = ICON_SIZE
        val output = try {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError in createBitmap, using fallback size", oom)
            try {
                Bitmap.createBitmap(115, 115, Bitmap.Config.ARGB_8888)
            } catch (_: Throwable) {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
        }
        try {
            val canvas = Canvas(output)
            
            val pack = _selectedIconPack.value
            val isExternalPack = !pack.isNullOrEmpty()
            
            var resolvedDrawable: Drawable? = null
            if (isExternalPack) {
                try {
                    val map = getCachedAppFilterMap(pack!!)
                    val key = "$appPackageName/$appActivityName"
                    val drawableName = map[key] ?: map[appPackageName]
                    if (drawableName != null) {
                        val packRes = packageManager.getResourcesForApplication(pack)
                        val drawableId = packRes.getIdentifier(drawableName, "drawable", pack)
                        if (drawableId != 0) {
                            resolvedDrawable = packRes.getDrawable(drawableId, null)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error resolving icon pack drawable: ${e.message}")
                }
            }
            
            val finalDrawable = resolvedDrawable ?: original
            val safeDrawable = try {
                finalDrawable.constantState?.newDrawable()?.mutate() ?: finalDrawable.mutate()
            } catch (_: Throwable) {
                finalDrawable
            }

            val shape = _iconShape.value
            val path = pathCache.getOrPut(shape) {
                when (shape) {
                    "sammy" -> IconCornerShape.createAndroidSammyPath(size.toFloat(), size.toFloat())
                    "circle" -> Path().apply {
                        addOval(RectF(0f, 0f, size.toFloat(), size.toFloat()), Path.Direction.CW)
                    }
                    "squircle" -> Path().apply {
                        val r = size.toFloat() * 0.32f
                        addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, Path.Direction.CW)
                    }
                    "square" -> Path().apply {
                        val r = size.toFloat() * 0.08f
                        addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, Path.Direction.CW)
                    }
                    "rounded_square" -> Path().apply {
                        val r = size.toFloat() * 0.20f
                        addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, Path.Direction.CW)
                    }
                    else -> IconCornerShape.createAndroidSammyPath(size.toFloat(), size.toFloat())
                }
            }

            canvas.save()
            canvas.clipPath(path)

            val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && safeDrawable is AdaptiveIconDrawable
            if (isAdaptive) {
                try {
                    val adaptive = safeDrawable as AdaptiveIconDrawable
                    val background = adaptive.background
                    val foreground = adaptive.foreground
                    val offset = (size * 0.25f).toInt()
                    val left = -offset
                    val top = -offset
                    val right = size + offset
                    val bottom = size + offset

                    if (background != null) {
                        background.setBounds(left, top, right, bottom)
                        background.draw(canvas)
                    } else {
                        canvas.drawColor(android.graphics.Color.rgb(42, 44, 52))
                    }

                    if (foreground != null) {
                        foreground.setBounds(left, top, right, bottom)
                        foreground.draw(canvas)
                    }
                } catch (_: Throwable) {
                    safeDrawable.setBounds(0, 0, size, size)
                    safeDrawable.draw(canvas)
                }
            } else {
                val intrinsicWidth = safeDrawable.intrinsicWidth
                val intrinsicHeight = safeDrawable.intrinsicHeight

                val targetSize = size.toFloat()
                val (w, h) = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    val ratio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                    if (ratio > 1f) {
                        Pair(targetSize, targetSize / ratio)
                    } else {
                        Pair(targetSize * ratio, targetSize)
                    }
                } else {
                    Pair(targetSize, targetSize)
                }

                val left = (targetSize - w) / 2f
                val top = (targetSize - h) / 2f

                safeDrawable.setBounds(left.toInt(), top.toInt(), (left + w).toInt(), (top + h).toInt())
                safeDrawable.draw(canvas)
            }

            canvas.restore()
            output.prepareToDraw()
        } catch (e: Throwable) {
            Log.e(TAG, "Error in getMaskedIcon", e)
        }
        
        return output
    }

    private val iconSemaphore = Semaphore(4)

    /**
     * Instant L1 memory lookup (0ms). Returns ImageBitmap if present in RAM, avoiding any disk, GC, or IPC overhead.
     */
    fun getAppIconFast(packageName: String, activityName: String): ImageBitmap? {
        val shape = _iconShape.value
        val pack = _selectedIconPack.value
        return iconDiskCache.getMemoryOnly(packageName, activityName, shape, pack)
    }

    /**
     * Asynchronously loads the icon prioritizing L1 RAM -> L2 Persistent Disk -> PackageManager.
     * Thread-safe with in-flight deduplication to avoid redundant processing.
     */
    suspend fun loadAppIconAsync(packageName: String, activityName: String): ImageBitmap? {
        val shape = _iconShape.value
        val pack = _selectedIconPack.value
        
        // 1. Ultra-fast RAM check
        val memCached = iconDiskCache.getMemoryOnly(packageName, activityName, shape, pack)
        if (memCached != null) return memCached

        // 2. Persistent Disk check
        val diskCached = withContext(Dispatchers.IO) {
            iconDiskCache.getImageBitmap(packageName, activityName, shape, pack)
        }
        if (diskCached != null) return diskCached

        // 3. Deduplicate in-flight rendering for the exact same component using viewModelScope
        // so that if one caller's LaunchedEffect is cancelled during scroll, other callers waiting on the
        // same icon are not aborted with CancellationException.
        val jobKey = "${packageName}_${activityName}_${shape}_${pack ?: "default"}"
        var deferred = inFlightIconJobs[jobKey]
        if (deferred == null) {
            val newDeferred = viewModelScope.async(Dispatchers.IO) {
                try {
                    iconSemaphore.withPermit {
                        val doubleCheck = iconDiskCache.getImageBitmap(packageName, activityName, shape, pack)
                        if (doubleCheck != null) return@withPermit doubleCheck

                        val intent = Intent().apply { setClassName(packageName, activityName) }
                        val resolveInfo = try {
                            packageManager.resolveActivity(intent, 0)
                        } catch (_: Exception) { null }

                        val rawIcon = try {
                            resolveInfo?.loadIcon(packageManager) ?: packageManager.defaultActivityIcon
                        } catch (_: Exception) { null }

                        if (rawIcon != null) {
                            val processed = getMaskedIcon(rawIcon, packageName, activityName)
                            iconDiskCache.put(packageName, activityName, shape, pack, processed)
                            processed.asImageBitmap()
                        } else {
                            null
                        }
                    }
                } finally {
                    inFlightIconJobs.remove(jobKey)
                }
            }
            val existing = inFlightIconJobs.putIfAbsent(jobKey, newDeferred)
            if (existing != null) {
                newDeferred.cancel()
                deferred = existing
            } else {
                deferred = newDeferred
            }
        }
        return deferred.await()
    }

    /**
     * Loads and reconciles the app list in background.
     * Uses cached metadata and lastUpdateTime to skip redundant loadLabel() queries on unchanged apps.
     */
    fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            if (_rawApps.value.isEmpty()) {
                _isLoading.value = true
            }
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.queryIntentActivities(
                            mainIntent,
                            PackageManager.ResolveInfoFlags.of(0L)
                        )
                    } else {
                        packageManager.queryIntentActivities(mainIntent, 0)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error querying intent activities", e)
                    emptyList()
                }

                val filteredResolves = resolveInfos
                    .filter { it.activityInfo != null && it.activityInfo.packageName != currentPackageName }
                    .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }

                val currentKeys = ConcurrentHashMap.newKeySet<String>()
                val pkgUpdateTimeCache = HashMap<String, Long>(filteredResolves.size)
                var hasChanges = false
                
                val appsList = filteredResolves.mapNotNull { resolveInfo ->
                    val pkgName = resolveInfo.activityInfo.packageName ?: return@mapNotNull null
                    val actName = resolveInfo.activityInfo.name ?: return@mapNotNull null
                    val key = "$pkgName/$actName"
                    currentKeys.add(key)

                    val lastUpdateTime = pkgUpdateTimeCache.getOrPut(pkgName) {
                        val pkgInfo = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                packageManager.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0L))
                            } else {
                                packageManager.getPackageInfo(pkgName, 0)
                            }
                        } catch (_: Throwable) { null }
                        pkgInfo?.lastUpdateTime ?: 0L
                    }

                    // Check if already in cache and unchanged
                    val existing = appCache[key]
                    if (existing != null && existing.lastUpdateTime == lastUpdateTime && existing.label.isNotEmpty()) {
                        // Reuse cached AppInfo without reading APK resources!
                        existing
                    } else {
                        hasChanges = true
                        val label = try {
                            resolveInfo.loadLabel(packageManager).toString()
                        } catch (_: Throwable) {
                            pkgName
                        }
                        if (existing != null && existing.lastUpdateTime != lastUpdateTime) {
                            // App was updated, invalidate its old icon
                            iconDiskCache.invalidatePackage(pkgName)
                        }
                        AppInfo(label, pkgName, actName, lastUpdateTime)
                    }
                }.sortedBy { it.normalizedLabel }

                // Check if any app was removed
                val removedKeys = appCache.keys.filter { !currentKeys.contains(it) }
                if (removedKeys.isNotEmpty()) {
                    hasChanges = true
                    removedKeys.forEach { appCache.remove(it) }
                }

                // If apps changed from current state or this is initial load
                if (hasChanges || _rawApps.value.isEmpty() || _rawApps.value.size != appsList.size) {
                    appsList.forEach { appCache["${it.packageName}/${it.activityName}"] = it }
                    _rawApps.value = appsList
                    appMetadataCache.saveToDisk(appsList)
                    Log.d(TAG, "Reconciled app list: ${appsList.size} apps active")
                } else {
                    Log.d(TAG, "No changes detected in installed apps. Reusing cache.")
                }

                _isLoading.value = false

                // Pre-populate icons into L1 RAM cache in background for zero scrolling lag
                preloadDockIcons(appsList)
                preloadIconsInBackground(appsList)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Exception in loadApps", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Differential update when a package is uninstalled.
     * Removes ONLY the affected package without re-scanning or rebuilding the entire cache.
     */
    fun onPackageRemoved(packageName: String) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            Log.d(TAG, "onPackageRemoved (differential): $packageName")
            iconDiskCache.invalidatePackage(packageName)
            
            // Remove from memory appCache
            val keysToRemove = appCache.keys.filter { it.startsWith("$packageName/") }
            keysToRemove.forEach { appCache.remove(it) }

            // Remove from _rawApps
            val current = _rawApps.value
            val updated = current.filter { it.packageName != packageName }
            if (updated.size != current.size) {
                _rawApps.value = updated
                appMetadataCache.saveToDisk(updated)
            }

            // Remove from dock if present
            val dock = LinkedHashSet(_dockAppKeys.value)
            if (dock.removeAll { it.startsWith("$packageName/") || it == packageName }) {
                _dockAppKeys.value = dock
                writeOrderedStringSet("dock_apps_ordered_v2", "dock_apps", dock)
            }

            // Remove from hidden if present
            val hidden = LinkedHashSet(_hiddenAppKeys.value)
            if (hidden.removeAll { it.startsWith("$packageName/") || it == packageName }) {
                _hiddenAppKeys.value = hidden
                writeOrderedStringSet("hidden_apps_ordered_v2", "hidden_apps", hidden)
            }

            // Remove from locked if present
            val locked = LinkedHashSet(_lockedAppKeys.value)
            if (locked.removeAll { it.startsWith("$packageName/") || it == packageName }) {
                _lockedAppKeys.value = locked
                writeOrderedStringSet("locked_apps_ordered_v2", "locked_apps", locked)
            }

            // Clear slot in horizontal drawer pages if present
            val currentPages = _horizontalDrawerPages.value
            if (currentPages.isNotEmpty()) {
                val cleaned = currentPages.map { page ->
                    page.map { key ->
                        if (key != null && (key.startsWith("$packageName/") || key == packageName)) null else key
                    }
                }
                saveHorizontalDrawerPages(cleaned)
            }
        }
    }

    /**
     * Differential update when a package is installed.
     * Resolves only the newly installed package and appends it to cache.
     */
    fun onPackageAdded(packageName: String) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            Log.d(TAG, "onPackageAdded (differential): $packageName")
            iconDiskCache.invalidatePackage(packageName)
            loadSinglePackage(packageName)
            loadIconPacks()
        }
    }

    /**
     * Differential update when a package is changed or updated.
     * Invalidates only the updated package's icon and refreshes its activities.
     */
    fun onPackageChanged(packageName: String) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            Log.d(TAG, "onPackageChanged (differential): $packageName")
            iconDiskCache.invalidatePackage(packageName)
            loadSinglePackage(packageName)
        }
    }

    private suspend fun loadSinglePackage(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                packageManager.queryIntentActivities(intent, 0)
            }
            if (resolves.isEmpty()) {
                onPackageRemoved(packageName)
                return
            }

            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    packageManager.getPackageInfo(packageName, 0)
                }
            } catch (_: Throwable) { null }
            val lastUpdateTime = pkgInfo?.lastUpdateTime ?: System.currentTimeMillis()

            val newApps = resolves.mapNotNull { resolve ->
                val actName = resolve.activityInfo?.name ?: return@mapNotNull null
                val label = try {
                    resolve.loadLabel(packageManager).toString()
                } catch (_: Throwable) { packageName }
                AppInfo(label, packageName, actName, lastUpdateTime)
            }

            val current = _rawApps.value.toMutableList()
            current.removeAll { it.packageName == packageName }
            current.addAll(newApps)
            val sorted = current.sortedBy { it.normalizedLabel }

            for (app in newApps) {
                appCache["${app.packageName}/${app.activityName}"] = app
            }

            _rawApps.value = sorted
            appMetadataCache.saveToDisk(sorted)

            // Pre-fetch new icons in background
            for (app in newApps) {
                loadAppIconAsync(app.packageName, app.activityName)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading single package $packageName, falling back to full reconciliation", e)
            loadApps()
        }
    }

    fun onLauncherResumed() {
        val app = getApplication<Application>()
        _isSystemLiveWallpaperServiceActive.value =
            VideoLiveWallpaperService.isSystemLiveWallpaperActive(app)
        // Ensure first page of icons is always warm in L1 RAM if returning from extreme memory pressure
        val apps = _rawApps.value
        if (apps.isNotEmpty()) {
            val firstApp = apps.first()
            if (getAppIconFast(firstApp.packageName, firstApp.activityName) == null) {
                preloadDockIcons(apps)
                preloadIconsInBackground(apps)
            }
        }
    }

    fun onHomePressed() {
        _closeOverlaysTrigger.value = _closeOverlaysTrigger.value + 1
        if (_searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
        }
    }

    fun invalidatePackage(packageName: String) {
        onPackageChanged(packageName)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(context: Context, app: AppInfo) {
        try {
            Log.d(TAG, "Launching app: ${app.packageName}/${app.activityName}")
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.w(TAG, "Direct launch failed for ${app.packageName}, trying fallback launch intent", e)
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
                }
            } catch (e2: Throwable) {
                Log.e(TAG, "Fallback launch failed for ${app.packageName}", e2)
                Toast.makeText(context, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun trimMemory(level: Int = android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
        Log.d(TAG, "trimMemory executed: level=$level")
        iconDiskCache.trimMemory(level)
    }

    fun setShowAppNames(show: Boolean) {
        _showAppNames.value = show
        prefs.edit().putBoolean("show_app_names", show).apply()
    }

    fun setIconPack(packageName: String?) {
        if (_selectedIconPack.value == packageName) return
        _selectedIconPack.value = packageName
        cachedIconPackPackageName = null
        cachedAppFilterMap = null
        if (packageName == null) {
            prefs.edit().remove("selected_icon_pack").apply()
        } else {
            prefs.edit().putString("selected_icon_pack", packageName).apply()
        }
        loadIconPacks()
        preloadDockIcons(_rawApps.value)
        preloadIconsInBackground(_rawApps.value)
    }

    fun loadIconPacks() {
        loadIconPacksJob?.cancel()
        loadIconPacksJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val list = mutableListOf<IconPackInfo>()
            val current = _selectedIconPack.value
            
            list.add(IconPackInfo(packageName = null, label = "Padrão", isSelected = current.isNullOrEmpty()))
            
            try {
                val intents = listOf(
                    Intent("org.adw.launcher.THEMES"),
                    Intent("com.novalauncher.THEME"),
                    Intent("com.gau.go.launcherex.theme"),
                    Intent("com.fede.launcher.THEME_ICONPACK")
                )
                val foundPackages = mutableSetOf<String>()
                for (intent in intents) {
                    val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
                    } else {
                        packageManager.queryIntentActivities(intent, 0)
                    }
                    for (info in resolveList) {
                        val pkg = info.activityInfo.packageName
                        if (pkg != currentPackageName && foundPackages.add(pkg)) {
                            val label = info.loadLabel(packageManager).toString()
                            list.add(IconPackInfo(packageName = pkg, label = label, isSelected = current == pkg))
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading icon packs: ${e.message}")
            }
            _iconPacks.value = list
        }
    }

    fun setIconShape(shape: String) {
        if (_iconShape.value == shape) return
        _iconShape.value = shape
        prefs.edit().putString("icon_shape", shape).apply()
        preloadDockIcons(_rawApps.value)
        preloadIconsInBackground(_rawApps.value)
    }

    fun setIconSizeProgress(progress: Float) {
        val snapped = snapToAllowedIconSizeProgress(progress)
        _iconSizeProgress.value = snapped
        prefs.edit()
            .putFloat("icon_size_progress_v3", snapped)
            .apply()
    }

    fun setDrawerStyle(style: String) {
        val validStyle = if (style == "horizontal") "horizontal" else "vertical"
        _drawerStyle.value = validStyle
        prefs.edit().putString("drawer_style", validStyle).apply()
    }

    private fun parseHorizontalDrawerPages(raw: String?): List<List<String?>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").filter { it.isNotEmpty() }.map { pageLine ->
            val tokens = pageLine.split("\t")
            List(24) { idx ->
                val token = tokens.getOrNull(idx)?.trim()
                if (token.isNullOrEmpty() || token == "_EMPTY_") null else token
            }
        }
    }

    private fun serializeHorizontalDrawerPages(pages: List<List<String?>>): String {
        return pages.joinToString("\n") { page ->
            List(24) { idx -> page.getOrNull(idx) ?: "_EMPTY_" }.joinToString("\t")
        }
    }

    fun saveHorizontalDrawerPages(pages: List<List<String?>>) {
        val compacted = pages.mapNotNull { page ->
            val nonNullKeys = page.filterNotNull().filter { it.isNotEmpty() && it != "_EMPTY_" }.take(24)
            if (nonNullKeys.isEmpty()) {
                null
            } else {
                List(24) { idx -> nonNullKeys.getOrNull(idx) }
            }
        }
        _horizontalDrawerPages.value = compacted
        val serialized = serializeHorizontalDrawerPages(compacted)
        val previousValid = prefs.getString("horizontal_drawer_pages_v1", null)
        val editor = prefs.edit()
        if (!previousValid.isNullOrBlank()) {
            editor.putString("horizontal_drawer_pages_v1_backup", previousValid)
        }
        editor.putString("horizontal_drawer_pages_v1", serialized).apply()
    }

    fun resolveHorizontalDrawerPages(
        visibleApps: List<AppInfo>,
        savedPages: List<List<String?>>
    ): List<List<AppInfo?>> {
        if (visibleApps.isEmpty()) {
            return listOf(List(24) { null })
        }
        if (savedPages.isEmpty()) {
            return visibleApps.chunked(24).map { chunk ->
                List(24) { idx -> chunk.getOrNull(idx) }
            }
        }

        val appByKey = HashMap<String, AppInfo>(visibleApps.size * 2)
        for (app in visibleApps) {
            appByKey["${app.packageName}/${app.activityName}"] = app
            appByKey.putIfAbsent(app.packageName, app)
        }

        val placedKeys = HashSet<String>(visibleApps.size)
        val resolvedPages = mutableListOf<MutableList<AppInfo>>()

        for (pageKeys in savedPages) {
            val pageApps = mutableListOf<AppInfo>()
            for (rawKey in pageKeys) {
                if (rawKey == null) continue
                val app = appByKey[rawKey] ?: continue
                val fullKey = "${app.packageName}/${app.activityName}"
                if (placedKeys.add(fullKey) && pageApps.size < 24) {
                    pageApps.add(app)
                }
            }
            if (pageApps.isNotEmpty()) {
                resolvedPages.add(pageApps)
            }
        }

        // Place any newly discovered/unplaced apps into the last page with available space (or a new page)
        // without ever pulling or shifting apps between existing pages
        for (app in visibleApps) {
            val fullKey = "${app.packageName}/${app.activityName}"
            if (placedKeys.add(fullKey)) {
                val lastPage = resolvedPages.lastOrNull()
                if (lastPage != null && lastPage.size < 24) {
                    lastPage.add(app)
                } else {
                    resolvedPages.add(mutableListOf(app))
                }
            }
        }

        if (resolvedPages.isEmpty()) {
            return listOf(List(24) { null })
        }

        return resolvedPages.map { pageApps ->
            List(24) { idx -> pageApps.getOrNull(idx) }
        }
    }

    /**
     * Reorganizes horizontal 4x6 pages when moving a group of [movingApps] as a single unit
     * to [destPage] at [targetSlotOnDestPage].
     * - When moving to another page, automatically places the group into the first available spaces
     *   of that page (or at the target slot if reordering on the same page).
     * - Never creates a new page unless the destination page is genuinely full and overflow occurs.
     * - Compacts all pages so there are never empty spaces between apps.
     */
    fun reorganizeHorizontalPagesMultiple(
        currentPages: List<List<AppInfo?>>,
        movingApps: List<AppInfo>,
        sourcePage: Int,
        destPage: Int,
        targetSlotOnDestPage: Int,
        keepEmptyPagesForDrag: Boolean = false
    ): Pair<List<List<AppInfo?>>, Int> {
        if (movingApps.isEmpty()) return Pair(currentPages, -1)
        val movingKeys = movingApps.map { "${it.packageName}/${it.activityName}" }.toSet()

        val compactPages = currentPages.map { page ->
            page.filterNotNull().toMutableList()
        }.toMutableList()

        if (compactPages.isEmpty()) {
            compactPages.add(mutableListOf())
        }

        val safeDestPage = destPage.coerceIn(0, compactPages.lastIndex)

        // 1. Remove all moving apps from their origin positions so remaining apps shift contiguously
        for (page in compactPages) {
            page.removeAll { app ->
                movingKeys.contains("${app.packageName}/${app.activityName}")
            }
        }

        val destList = compactPages[safeDestPage]
        // 2. When moving to another page, place in the first available spaces (or at targetSlot if within existing apps)
        val effectiveInsertSlot = if (destPage != sourcePage && targetSlotOnDestPage >= destList.size) {
            destList.size.coerceAtMost(24)
        } else {
            targetSlotOnDestPage.coerceIn(0, destList.size.coerceAtMost(23))
        }

        destList.addAll(effectiveInsertSlot, movingApps)

        // 3. Cascade any overflow (> 24 apps) into subsequent pages, only creating a new page if strictly necessary
        var p = safeDestPage
        while (p < compactPages.size) {
            if (compactPages[p].size > 24) {
                val overflow = compactPages[p].subList(24, compactPages[p].size).toList()
                while (compactPages[p].size > 24) {
                    compactPages[p].removeAt(compactPages[p].lastIndex)
                }
                if (p + 1 >= compactPages.size) {
                    compactPages.add(overflow.toMutableList())
                } else {
                    compactPages[p + 1].addAll(0, overflow)
                }
            }
            p++
        }

        // 4. Compact empty pages when finalizing
        val finalCompact = if (keepEmptyPagesForDrag) {
            compactPages
        } else {
            compactPages.filter { it.isNotEmpty() }.ifEmpty { mutableListOf(mutableListOf()) }
        }

        val padded = finalCompact.map { pageApps ->
            List(24) { idx -> pageApps.getOrNull(idx) }
        }

        return Pair(padded, effectiveInsertSlot.coerceIn(0, 23))
    }

    fun resolveOrderedApps(
        visibleApps: List<AppInfo>,
        savedPages: List<List<String?>>
    ): List<AppInfo> {
        if (visibleApps.isEmpty()) return emptyList()
        if (savedPages.isEmpty()) return visibleApps
        return resolveHorizontalDrawerPages(visibleApps, savedPages).flatten().filterNotNull()
    }

    fun saveCustomOrderedApps(orderedApps: List<AppInfo>) {
        val pages = orderedApps.chunked(24).map { chunk ->
            List(24) { idx ->
                val app = chunk.getOrNull(idx)
                app?.let { "${it.packageName}/${it.activityName}" }
            }
        }
        saveHorizontalDrawerPages(pages)
    }

    fun reorganizeOrderedAppsMultiple(
        currentApps: List<AppInfo>,
        movingApps: List<AppInfo>,
        targetIndex: Int
    ): Pair<List<AppInfo>, Int> {
        if (movingApps.isEmpty()) return Pair(currentApps, -1)
        val movingKeys = movingApps.map { "${it.packageName}/${it.activityName}" }.toSet()
        val mutable = currentApps.toMutableList()
        mutable.removeAll { app ->
            movingKeys.contains("${app.packageName}/${app.activityName}")
        }
        val effectiveIdx = targetIndex.coerceIn(0, mutable.size)
        mutable.addAll(effectiveIdx, movingApps)
        return Pair(mutable, effectiveIdx)
    }

    fun toggleDockApp(app: AppInfo): Boolean {
        val key = "${app.packageName}/${app.activityName}"
        val current = LinkedHashSet(_dockAppKeys.value)
        if (current.contains(key) || current.contains(app.packageName)) {
            current.remove(key)
            current.remove(app.packageName)
            _dockAppKeys.value = current
            writeOrderedStringSet("dock_apps_ordered_v2", "dock_apps", current)
            return true
        } else {
            if (current.size < 4) {
                current.add(key)
                _dockAppKeys.value = current
                writeOrderedStringSet("dock_apps_ordered_v2", "dock_apps", current)
                return true
            } else {
                return false
            }
        }
    }

    fun isAppInDock(app: AppInfo): Boolean {
        val key = "${app.packageName}/${app.activityName}"
        val current = _dockAppKeys.value
        return current.contains(key) || current.contains(app.packageName)
    }

    fun toggleHideApp(app: AppInfo) {
        val key = "${app.packageName}/${app.activityName}"
        toggleHideAppKey(key)
    }

    fun toggleHideAppKey(key: String) {
        val current = LinkedHashSet(_hiddenAppKeys.value)
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }
        _hiddenAppKeys.value = current
        writeOrderedStringSet("hidden_apps_ordered_v2", "hidden_apps", current)
    }

    fun isAppLocked(app: AppInfo): Boolean {
        val key = "${app.packageName}/${app.activityName}"
        val current = _lockedAppKeys.value
        if (current.isEmpty()) return false
        return current.contains(key) || current.contains(app.packageName)
    }

    fun setLockedAppKeys(newKeys: Set<String>) {
        val copy = LinkedHashSet(newKeys)
        _lockedAppKeys.value = copy
        writeOrderedStringSet("locked_apps_ordered_v2", "locked_apps", copy)
    }
}
