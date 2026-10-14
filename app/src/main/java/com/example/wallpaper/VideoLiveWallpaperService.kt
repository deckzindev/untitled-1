package com.example.wallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArraySet

private const val TAG = "VideoLiveWallpaper"

/**
 * Real System-Wide Android Native Live Wallpaper Service (`android.service.wallpaper.WallpaperService`).
 *
 * When set as the system Live Wallpaper, Android's WindowManager renders this surface directly in the
 * OS wallpaper layer — meaning the video plays across the entire Android system (lock screen if selected,
 * notification shade, recents/multitasking, translucent apps, and any launcher).
 */
class VideoLiveWallpaperService : WallpaperService() {

    companion object {
        const val VIDEO_FILE_NAME = "active_live_wallpaper.mp4"
        const val ACTION_VIDEO_WALLPAPER_UPDATED = "com.example.wallpaper.ACTION_VIDEO_WALLPAPER_UPDATED"

        private val activeEngines = CopyOnWriteArraySet<VideoWallpaperEngine>()

        fun getVideoFile(context: Context): File {
            return File(context.filesDir, VIDEO_FILE_NAME)
        }

        fun hasActiveVideoFile(context: Context): Boolean {
            val file = getVideoFile(context)
            return file.exists() && file.length() > 0L
        }

        fun clearVideoFile(context: Context) {
            try {
                val file = getVideoFile(context)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Throwable) {}
        }

        /**
         * Copies the selected video URI into the app's private storage atomically,
         * validates that it is a playable video, and immediately reloads all running
         * system LiveWallpaper engines without requiring re-activation if already active.
         */
        fun saveVideoFromUri(context: Context, uri: Uri): Boolean {
            val targetFile = getVideoFile(context)
            val tmpFile = File(context.filesDir, "$VIDEO_FILE_NAME.tmp")
            return try {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                    true
                } ?: false

                if (!copied || !tmpFile.exists() || tmpFile.length() <= 0L) {
                    tmpFile.delete()
                    return false
                }

                // Validate video stream quickly via MediaMetadataRetriever
                val isValidVideo = try {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(tmpFile.absolutePath)
                        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        hasVideo != null || width > 0
                    } finally {
                        try { retriever.release() } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {
                    true
                }

                if (!isValidVideo) {
                    tmpFile.delete()
                    return false
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                val renamed = tmpFile.renameTo(targetFile)
                if (renamed) {
                    // 1. Hot-reload any active system engines immediately in-memory
                    for (engine in activeEngines) {
                        engine.reloadVideoFromDisk()
                    }
                    // 2. Also broadcast in case engine runs in another context
                    try {
                        context.sendBroadcast(
                            Intent(ACTION_VIDEO_WALLPAPER_UPDATED).setPackage(context.packageName)
                        )
                    } catch (_: Throwable) {}
                }
                renamed
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to save video wallpaper from URI", e)
                try { if (tmpFile.exists()) tmpFile.delete() } catch (_: Throwable) {}
                false
            }
        }

        /**
         * Checks whether Android's WallpaperManager currently has this VideoLiveWallpaperService
         * registered as the active system-wide Live Wallpaper.
         */
        fun isSystemLiveWallpaperActive(context: Context): Boolean {
            return try {
                val info = WallpaperManager.getInstance(context).wallpaperInfo
                info?.packageName == context.packageName &&
                    info.serviceName == VideoLiveWallpaperService::class.java.name
            } catch (_: Throwable) {
                false
            }
        }

        /**
         * Activates `VideoLiveWallpaperService` as the real Android system-wide Live Wallpaper:
         * 1. If our `VideoLiveWallpaperService` is already the active system Live Wallpaper, it
         *    hot-reloads the new video immediately with zero prompts.
         * 2. Attempts direct binder call `WallpaperManager.setWallpaperComponent(component)`
         *    (works if the app holds system/privileged permission or on rooted/custom ROMs).
         * 3. Launches `WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` directly (without failing
         *    on Android 11+ package visibility) so the Android OS binds our `VideoLiveWallpaperService`.
         * 4. Falls back across OEM/AOSP LiveWallpaper intents (`LIVE_WALLPAPER_CHOOSER`, explicit
         *    `com.android.wallpaper.livepicker` component, or `ACTION_SET_WALLPAPER`).
         */
        fun activateSystemLiveWallpaper(context: Context): Boolean {
            // Already active in the OS! The engine was already hot-reloaded with the new video.
            if (isSystemLiveWallpaperActive(context)) {
                for (engine in activeEngines) {
                    engine.reloadVideoFromDisk()
                }
                return true
            }

            val component = ComponentName(context, VideoLiveWallpaperService::class.java)
            val wm = WallpaperManager.getInstance(context)

            // 1. Try direct WallpaperManager.setWallpaperComponent via reflection (instant silent activation when permitted)
            try {
                val setMethod = wm.javaClass.getMethod("setWallpaperComponent", ComponentName::class.java)
                val result = setMethod.invoke(wm, component) as? Boolean ?: true
                if (result && isSystemLiveWallpaperActive(context)) {
                    return true
                }
            } catch (_: Throwable) {}

            // 2. Standard Android OS Live Wallpaper activation intent (direct startActivity without resolveActivity check)
            try {
                val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(changeIntent)
                return true
            } catch (e: Throwable) {
                Log.w(TAG, "ACTION_CHANGE_LIVE_WALLPAPER direct launch failed, trying explicit AOSP/OEM components", e)
            }

            // 3. Explicit AOSP / Samsung / Xiaomi / Motorola LivePicker component targets
            val knownPickerComponents = listOf(
                ComponentName("com.android.wallpaper.livepicker", "com.android.wallpaper.livepicker.LiveWallpaperChange"),
                ComponentName("com.android.wallpaper.livepicker", "com.android.wallpaper.livepicker.LiveWallpaperActivity"),
                ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.settings.LiveWallpaperChangeActivity"),
                ComponentName("com.miui.miwallpaper", "com.miui.miwallpaper.activity.LiveWallpaperChangeActivity")
            )
            for (pickerComp in knownPickerComponents) {
                try {
                    val explicitIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        this.component = pickerComp
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                        if (context !is android.app.Activity) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(explicitIntent)
                    return true
                } catch (_: Throwable) {}
            }

            // 4. Fallback to general LIVE_WALLPAPER_CHOOSER
            try {
                val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(chooserIntent)
                return true
            } catch (_: Throwable) {}

            return false
        }
    }

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }

    private inner class VideoWallpaperEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private var isEngineVisible: Boolean = false
        private var currentHolder: SurfaceHolder? = null
        private var videoUpdateReceiver: BroadcastReceiver? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            activeEngines.add(this)

            val filter = IntentFilter(ACTION_VIDEO_WALLPAPER_UPDATED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    reloadVideoFromDisk()
                }
            }
            videoUpdateReceiver = receiver
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(receiver, filter)
                }
            } catch (_: Throwable) {}
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            initOrUpdatePlayer(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            currentHolder = holder
            if (mediaPlayer == null) {
                initOrUpdatePlayer(holder)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isEngineVisible = visible
            updatePlaybackState()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isEngineVisible = false
            if (currentHolder === holder) {
                currentHolder = null
            }
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            activeEngines.remove(this)
            videoUpdateReceiver?.let {
                try { unregisterReceiver(it) } catch (_: Throwable) {}
            }
            videoUpdateReceiver = null
            currentHolder = null
            releasePlayer()
        }

        fun reloadVideoFromDisk() {
            val holder = currentHolder ?: surfaceHolder
            if (holder != null && holder.surface != null && holder.surface.isValid) {
                initOrUpdatePlayer(holder)
            }
        }

        private fun isPowerSaveActive(): Boolean {
            return try {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isPowerSaveMode == true
            } catch (_: Throwable) {
                false
            }
        }

        private fun initOrUpdatePlayer(holder: SurfaceHolder) {
            releasePlayer()
            val surface = holder.surface
            if (surface == null || !surface.isValid) return

            val videoFile = getVideoFile(applicationContext)
            if (!videoFile.exists() || videoFile.length() <= 0L) {
                drawFallbackFrame(holder)
                return
            }

            try {
                val player = MediaPlayer()
                player.setSurface(surface)
                player.setDataSource(videoFile.absolutePath)
                player.isLooping = true
                player.setVolume(0f, 0f)
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                player.setOnPreparedListener { mp ->
                    try {
                        mp.setVolume(0f, 0f)
                        if (isEngineVisible && !isPowerSaveActive()) {
                            mp.start()
                        } else {
                            // Seek to first frame so preview/lockscreen shows a crisp frame immediately
                            mp.seekTo(1)
                        }
                    } catch (_: Throwable) {}
                }
                player.setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    true
                }
                mediaPlayer = player
                player.prepareAsync()
            } catch (e: Throwable) {
                Log.e(TAG, "Error initializing MediaPlayer in LiveWallpaperEngine", e)
                releasePlayer()
            }
        }

        private fun updatePlaybackState() {
            val player = mediaPlayer
            if (player == null) {
                val holder = currentHolder ?: surfaceHolder
                if (isEngineVisible && holder?.surface?.isValid == true) {
                    initOrUpdatePlayer(holder)
                }
                return
            }
            try {
                if (isEngineVisible && !isPowerSaveActive()) {
                    if (!player.isPlaying) {
                        player.start()
                    }
                } else {
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            } catch (_: Throwable) {}
        }

        private fun drawFallbackFrame(holder: SurfaceHolder) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.drawColor(Color.parseColor("#0A0B0F"))
            } catch (_: Throwable) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
                }
            }
        }

        private fun releasePlayer() {
            val player = mediaPlayer
            mediaPlayer = null
            if (player != null) {
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (_: Throwable) {}
                try {
                    player.reset()
                } catch (_: Throwable) {}
                try {
                    player.release()
                } catch (_: Throwable) {}
            }
        }
    }
}
