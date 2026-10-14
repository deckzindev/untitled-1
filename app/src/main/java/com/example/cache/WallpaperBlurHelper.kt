package com.example.cache

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.wallpaper.VideoLiveWallpaperService
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
data class BlurredWallpaperPair(
    val lightModeBlur: ImageBitmap,
    val darkModeBlur: ImageBitmap
)

object WallpaperBlurHelper {

    private const val CACHE_FILE_NAME = "current_wallpaper_cache.jpg"
    private const val TARGET_MAX_DIMENSION = 200

    fun saveCustomWallpaperFromAsset(context: Context, assetPath: String) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Throwable) {}
    }

    fun saveCustomWallpaperFromUri(context: Context, uri: Uri) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            // Decode downsampled bitmap from URI before caching to disk so a 15MB 4K gallery photo
            // never occupies 15MB in the app's private storage
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sampleSize = 1
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > TARGET_MAX_DIMENSION * 2) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
            if (decoded != null) {
                val scaled = scaleDownBitmap(decoded, TARGET_MAX_DIMENSION)
                FileOutputStream(cacheFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                if (scaled !== decoded && !scaled.isRecycled) scaled.recycle()
                if (!decoded.isRecycled) decoded.recycle()
            }
        } catch (_: Throwable) {}
    }

    fun saveCustomWallpaperFromVideoFile(context: Context, videoFile: File) {
        if (!videoFile.exists() || videoFile.length() <= 0L) return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val rawFrame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (rawFrame != null) {
                val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                val scaled = scaleDownBitmap(rawFrame, TARGET_MAX_DIMENSION)
                FileOutputStream(cacheFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                if (scaled !== rawFrame && !scaled.isRecycled) scaled.recycle()
                if (!rawFrame.isRecycled) rawFrame.recycle()
            }
        } catch (_: Throwable) {
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    fun isVideoUri(context: Context, uri: Uri): Boolean {
        try {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            if (mime.startsWith("video/")) return true
            val path = uri.toString().lowercase()
            if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") || path.endsWith(".mov") || path.endsWith(".3gp")) {
                return true
            }
        } catch (_: Throwable) {}
        return false
    }

    fun loadBlurredWallpaperPair(context: Context): BlurredWallpaperPair? {
        val rawBitmap = loadCurrentWallpaperBitmap(context) ?: return null
        return try {
            val scaled = scaleDownBitmap(rawBitmap, TARGET_MAX_DIMENSION)
            if (scaled !== rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            val w = scaled.width
            val h = scaled.height
            val wh = w * h
            val pix = IntArray(wh)
            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            val vmin = IntArray(max(w, h))

            // Light mode: slightly softer blur radius (16) to preserve airy shapes and colors
            val lightBitmap = stackBlur(scaled, radius = 16, pix = pix, r = r, g = g, b = b, vmin = vmin)
            // Dark mode: slightly deeper blur radius (22) for rich, distraction-free contrast
            val darkBitmap = stackBlur(scaled, radius = 22, pix = pix, r = r, g = g, b = b, vmin = vmin)
            if (!scaled.isRecycled && scaled !== lightBitmap && scaled !== darkBitmap) {
                scaled.recycle()
            }
            lightBitmap.prepareToDraw()
            darkBitmap.prepareToDraw()
            BlurredWallpaperPair(
                lightModeBlur = lightBitmap.asImageBitmap(),
                darkModeBlur = darkBitmap.asImageBitmap()
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadCurrentWallpaperBitmap(context: Context): Bitmap? {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        val hasVideoActive = VideoLiveWallpaperService.hasActiveVideoFile(context)

        // 1. If a video wallpaper or custom cached frame is active in our launcher, prioritize our exact cached frame
        if (hasVideoActive && cacheFile.exists() && cacheFile.length() > 0L) {
            try {
                decodeSampledFile(cacheFile, TARGET_MAX_DIMENSION)?.let { return it }
            } catch (_: Throwable) {}
        }

        // 2. Try reading static system wallpaper from WallpaperManager
        try {
            val wm = WallpaperManager.getInstance(context)
            if (wm.wallpaperInfo == null) {
                val drawable: Drawable? = try {
                    wm.drawable ?: wm.fastDrawable ?: wm.peekDrawable()
                } catch (_: SecurityException) {
                    null
                } catch (_: Throwable) {
                    null
                }
                if (drawable != null) {
                    drawableToDownscaledBitmap(drawable)?.let { return it }
                }
            }
        } catch (_: Throwable) {}

        // 3. Fallback to cached file saved when user applied wallpaper via the launcher
        try {
            if (cacheFile.exists() && cacheFile.length() > 0L) {
                decodeSampledFile(cacheFile, TARGET_MAX_DIMENSION)?.let { return it }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun drawableToDownscaledBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            return scaleDownBitmap(drawable.bitmap, TARGET_MAX_DIMENSION)
        }
        val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1080
        val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: 2400
        val scale = min(1f, TARGET_MAX_DIMENSION.toFloat() / max(intrinsicW, intrinsicH).toFloat())
        val targetW = (intrinsicW * scale).toInt().coerceAtLeast(64)
        val targetH = (intrinsicH * scale).toInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetW, targetH)
        drawable.draw(canvas)
        return bitmap
    }

    private fun decodeSampledFile(file: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDim * 2) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun scaleDownBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source
        val maxSide = max(w, h)
        if (maxSide <= maxDim) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(32)
        val targetH = (h * ratio).toInt().coerceAtLeast(32)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    /**
     * Fast, zero-object-allocation StackBlur implementation using flat primitive arrays (API 21+).
     */
    private fun stackBlur(
        sentBitmap: Bitmap,
        radius: Int,
        pix: IntArray = IntArray(sentBitmap.width * sentBitmap.height),
        r: IntArray = IntArray(sentBitmap.width * sentBitmap.height),
        g: IntArray = IntArray(sentBitmap.width * sentBitmap.height),
        b: IntArray = IntArray(sentBitmap.width * sentBitmap.height),
        vmin: IntArray = IntArray(max(sentBitmap.width, sentBitmap.height))
    ): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val div = radius + radius + 1

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        yw = 0

        val stackR = IntArray(div)
        val stackG = IntArray(div)
        val stackB = IntArray(div)
        var stackpointer: Int
        var stackstart: Int
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                val sIdx = i + radius
                val sr = (p and 0xff0000) shr 16
                val sg = (p and 0x00ff00) shr 8
                val sb = p and 0x0000ff
                stackR[sIdx] = sr
                stackG[sIdx] = sg
                stackB[sIdx] = sb
                rbs = r1 - abs(i)
                rsum += sr * rbs
                gsum += sg * rbs
                bsum += sb * rbs
                if (i > 0) {
                    rinsum += sr
                    ginsum += sg
                    binsum += sb
                } else {
                    routsum += sr
                    goutsum += sg
                    boutsum += sb
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                var sIdx = stackstart % div

                routsum -= stackR[sIdx]
                goutsum -= stackG[sIdx]
                boutsum -= stackB[sIdx]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                val sr = (p and 0xff0000) shr 16
                val sg = (p and 0x00ff00) shr 8
                val sb = p and 0x0000ff
                stackR[sIdx] = sr
                stackG[sIdx] = sg
                stackB[sIdx] = sb

                rinsum += sr
                ginsum += sg
                binsum += sb

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sIdx = stackpointer

                val outR = stackR[sIdx]
                val outG = stackG[sIdx]
                val outB = stackB[sIdx]

                routsum += outR
                goutsum += outG
                boutsum += outB

                rinsum -= outR
                ginsum -= outG
                binsum -= outB

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x
                val sIdx = i + radius
                val sr = r[yi]
                val sg = g[yi]
                val sb = b[yi]
                stackR[sIdx] = sr
                stackG[sIdx] = sg
                stackB[sIdx] = sb
                rbs = r1 - abs(i)
                rsum += sr * rbs
                gsum += sg * rbs
                bsum += sb * rbs
                if (i > 0) {
                    rinsum += sr
                    ginsum += sg
                    binsum += sb
                } else {
                    routsum += sr
                    goutsum += sg
                    boutsum += sb
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                var sIdx = stackstart % div

                routsum -= stackR[sIdx]
                goutsum -= stackG[sIdx]
                boutsum -= stackB[sIdx]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                val sr = r[p]
                val sg = g[p]
                val sb = b[p]
                stackR[sIdx] = sr
                stackG[sIdx] = sg
                stackB[sIdx] = sb

                rinsum += sr
                ginsum += sg
                binsum += sb

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sIdx = stackpointer

                val outR = stackR[sIdx]
                val outG = stackG[sIdx]
                val outB = stackB[sIdx]

                routsum += outR
                goutsum += outG
                boutsum += outB

                rinsum -= outR
                ginsum -= outG
                binsum -= outB

                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
