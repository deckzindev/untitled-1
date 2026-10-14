package com.example.cache

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "IconDiskCache"

data class CachedIcon(
    val bitmap: Bitmap,
    val imageBitmap: ImageBitmap
)

class IconDiskCache(context: Context) {

    // Persistent storage inside app's private filesDir to guarantee persistence across reboots
    private val cacheDir = File(context.filesDir, "icon_cache").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // Memoized key map to avoid repeated String replacements and hash formatting during 120fps scrolling
    private val keyMemo = ConcurrentHashMap<String, String>(256)

    // Adaptive in-memory LRU cache sized according to device RAM tier (Low-RAM devices use a tighter cap)
    private val isLowRamDevice: Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true
    } catch (_: Throwable) {
        false
    }

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val memoryCacheSizeKb = if (isLowRamDevice) {
        (maxMemoryKb / 14).coerceIn(4 * 1024, 8 * 1024)
    } else {
        (maxMemoryKb / 10).coerceIn(6 * 1024, 14 * 1024)
    }

    val recommendedWarmupCount: Int = if (isLowRamDevice) 48 else 84

    private val memoryCache = object : LruCache<String, CachedIcon>(memoryCacheSizeKb) {
        override fun sizeOf(key: String, value: CachedIcon): Int {
            return (value.bitmap.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    @Volatile
    private var cachedVariantInput: String = ""
    @Volatile
    private var cachedVariantSuffix: String = ""

    private fun computeVariantSuffix(iconShape: String, iconPack: String?): String {
        val packKey = iconPack ?: "default"
        val input = "${iconShape}|${packKey}"
        if (cachedVariantInput == input && cachedVariantSuffix.isNotEmpty()) {
            return cachedVariantSuffix
        }
        val cleanPack = packKey.replace('.', '_')
        val variantHash = "${iconShape}_${cleanPack}_v14".hashCode().toUInt().toString(16).padStart(8, '0')
        val suffix = "_v$variantHash"
        cachedVariantInput = input
        cachedVariantSuffix = suffix
        return suffix
    }

    private fun generateKey(
        packageName: String,
        activityName: String,
        iconShape: String,
        iconPack: String?
    ): String {
        val lookup = "$packageName|$activityName|$iconShape|${iconPack ?: ""}"
        keyMemo[lookup]?.let { return it }
        val cleanPkg = packageName.replace('.', '_')
        val compHash = "$packageName/$activityName".hashCode().toUInt().toString(16).padStart(8, '0')
        val variantSuffix = computeVariantSuffix(iconShape, iconPack)
        val computed = "${cleanPkg}_${compHash}${variantSuffix}"
        if (keyMemo.size > 1024) keyMemo.clear()
        keyMemo[lookup] = computed
        return computed
    }

    /**
     * Removes cached icons on disk and in RAM that belong to previous icon shapes or icon packs,
     * keeping only the active (iconShape, iconPack) variant so disk storage stays minimal (~2-3MB).
     */
    fun pruneObsoleteVariants(activeShape: String, activeIconPack: String?) {
        val activeSuffix = computeVariantSuffix(activeShape, activeIconPack)
        synchronized(memoryCache) {
            try {
                val keysToRemove = memoryCache.snapshot().keys.filter { !it.endsWith(activeSuffix) }
                for (k in keysToRemove) {
                    memoryCache.remove(k)
                }
            } catch (_: Throwable) {}
        }
        try {
            val activeFileSuffix = "${activeSuffix}.webp"
            val files = cacheDir.listFiles() ?: return
            for (file in files) {
                val name = file.name
                if (name.endsWith(".tmp") || !name.endsWith(activeFileSuffix)) {
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Ultra-fast memory-only lookup (0ms). Returns non-null ImageBitmap if icon is currently in RAM.
     */
    fun getMemoryOnly(
        packageName: String,
        activityName: String,
        iconShape: String,
        iconPack: String?
    ): ImageBitmap? {
        val cacheKey = generateKey(packageName, activityName, iconShape, iconPack)
        synchronized(memoryCache) {
            val memCached = memoryCache.get(cacheKey)
            if (memCached != null) {
                if (!memCached.bitmap.isRecycled && memCached.bitmap.width > 0 && memCached.bitmap.height > 0) {
                    return memCached.imageBitmap
                } else {
                    memoryCache.remove(cacheKey)
                }
            }
        }
        return null
    }

    /**
     * Checks RAM first, then Disk. If found on Disk, decodes and caches in RAM.
     */
    fun getImageBitmap(
        packageName: String,
        activityName: String,
        iconShape: String,
        iconPack: String?
    ): ImageBitmap? {
        val cacheKey = generateKey(packageName, activityName, iconShape, iconPack)
        
        // 1. Check in-memory L1 cache first
        synchronized(memoryCache) {
            val memCached = memoryCache.get(cacheKey)
            if (memCached != null) {
                if (!memCached.bitmap.isRecycled && memCached.bitmap.width > 0 && memCached.bitmap.height > 0) {
                    return memCached.imageBitmap
                } else {
                    memoryCache.remove(cacheKey)
                }
            }
        }

        // 2. Check persistent disk L2 cache (pure read-only I/O, no setLastModified write syscall)
        val fileName = "$cacheKey.webp"
        val file = File(cacheDir, fileName)
        if (file.exists() && file.length() > 0) {
            return try {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                    bitmap.prepareToDraw()
                    val cached = CachedIcon(bitmap, bitmap.asImageBitmap())
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, cached)
                    }
                    cached.imageBitmap
                } else {
                    file.delete()
                    null
                }
            } catch (e: Throwable) {
                try { file.delete() } catch (_: Exception) {}
                null
            }
        }
        return null
    }

    private var putCount = 0

    /**
     * Puts bitmap into L1 RAM cache immediately and persists to L2 Disk cache.
     */
    fun put(
        packageName: String,
        activityName: String,
        iconShape: String,
        iconPack: String?,
        bitmap: Bitmap
    ) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return
        bitmap.prepareToDraw()
        val cacheKey = generateKey(packageName, activityName, iconShape, iconPack)
        val cached = CachedIcon(bitmap, bitmap.asImageBitmap())
        
        synchronized(memoryCache) {
            try {
                memoryCache.put(cacheKey, cached)
            } catch (_: Throwable) {}
        }

        val fileName = "$cacheKey.webp"
        val file = File(cacheDir, fileName)
        val tmpFile = File(cacheDir, "$fileName.tmp")
        try {
            FileOutputStream(tmpFile).use { out ->
                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, 95, out)
            }
            if (tmpFile.exists() && tmpFile.length() > 0) {
                if (file.exists()) {
                    file.delete()
                }
                tmpFile.renameTo(file)
                
                val shouldTrim = synchronized(this) {
                    putCount++
                    putCount % 40 == 0
                }
                if (shouldTrim) {
                    trimDiskCacheIfNeeded()
                }
            }
        } catch (e: Throwable) {
            try { if (tmpFile.exists()) tmpFile.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Invalidates only the cached icons for a specific package name, in both RAM and Disk.
     */
    fun invalidatePackage(packageName: String) {
        val cleanPkg = packageName.replace('.', '_')
        val prefix = "${cleanPkg}_"
        
        synchronized(memoryCache) {
            val keysToRemove = memoryCache.snapshot().keys.filter { it.startsWith(prefix) }
            for (k in keysToRemove) {
                memoryCache.remove(k)
            }
        }

        try {
            val files = cacheDir.listFiles { _, name -> name.startsWith(prefix) }
            files?.forEach { file ->
                try { file.delete() } catch (_: Exception) {}
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error deleting disk cache for package $packageName: ${e.message}")
        }
    }

    /**
     * Prunes disk cache if exceeding 12MB or 240 files, removing oldest modified files first
     * in a single directory pass without repeated listFiles() syscalls.
     */
    private fun trimDiskCacheIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            val maxSizeBytes = 12 * 1024 * 1024L // 12MB max disk footprint
            val maxFiles = 240

            var remainingCount = files.size
            var totalSize = 0L
            for (f in files) {
                totalSize += f.length()
            }

            if (totalSize > maxSizeBytes || remainingCount > maxFiles) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    if (totalSize <= maxSizeBytes && remainingCount <= maxFiles) break
                    val fileSize = file.length()
                    if (file.delete()) {
                        totalSize -= fileSize
                        remainingCount--
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Responds to system memory pressure levels adaptively to keep RAM usage minimal while
     * preserving visible home screen / first-page icons.
     */
    fun trimMemory(level: Int) {
        synchronized(memoryCache) {
            try {
                when {
                    level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                    level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                        memoryCache.trimToSize(memoryCacheSizeKb / 3)
                    }
                    level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                    level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                        memoryCache.trimToSize(memoryCacheSizeKb / 2)
                    }
                    level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                        memoryCache.trimToSize((memoryCacheSizeKb * 3) / 4)
                    }
                }
            } catch (_: Throwable) {}
        }
    }
}
