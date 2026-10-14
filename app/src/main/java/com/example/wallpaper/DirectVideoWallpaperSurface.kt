package com.example.wallpaper

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.PowerManager
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * Hardware-accelerated, zero-copy Direct Video Wallpaper renderer built directly into the Launcher.
 * Guarantees 100% standalone Live Wallpaper playback even when the user has uninstalled the system's
 * LiveWallpaperPicker or OEM Video Wallpaper helper apps.
 *
 * Automatically pauses playback whenever:
 * - The launcher Activity is paused/stopped (another app is in foreground)
 * - The App Drawer or full-screen overlay covers the home screen (`shouldPlay == false`)
 * - Android Battery Saver (`PowerManager.isPowerSaveMode`) is active
 */
@Composable
fun DirectVideoWallpaperSurface(
    videoFile: File,
    videoVersion: Int,
    shouldPlay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    val holder = remember { VideoTextureController(context.applicationContext) }

    val effectivePlay = isLifecycleResumed && shouldPlay

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.release()
        }
    }

    LaunchedEffect(videoFile.absolutePath, videoVersion) {
        holder.setVideoSource(videoFile)
    }

    LaunchedEffect(effectivePlay) {
        holder.setPlayRequested(effectivePlay)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = true
                surfaceTextureListener = holder.surfaceTextureListener
                holder.attachTextureView(this)
            }
        },
        update = { view ->
            holder.attachTextureView(view)
        }
    )
}

private class VideoTextureController(private val appContext: Context) {
    private var textureView: TextureView? = null
    private var surface: Surface? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private var isPrepared: Boolean = false
    private var playRequested: Boolean = true
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            viewWidth = width
            viewHeight = height
            surface?.release()
            surface = Surface(st)
            preparePlayerIfReady()
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
            viewWidth = width
            viewHeight = height
            updateCenterCropMatrix()
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            releasePlayerOnly()
            surface?.release()
            surface = null
            return true
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
    }

    fun attachTextureView(view: TextureView) {
        if (textureView !== view) {
            textureView = view
            if (view.isAvailable && surface == null) {
                viewWidth = view.width
                viewHeight = view.height
                view.surfaceTexture?.let { st ->
                    surface = Surface(st)
                    preparePlayerIfReady()
                }
            }
        }
    }

    fun setVideoSource(file: File) {
        currentFile = file
        preparePlayerIfReady()
    }

    fun setPlayRequested(play: Boolean) {
        playRequested = play
        syncPlayback()
    }

    private fun isPowerSaveMode(): Boolean {
        return try {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        } catch (_: Throwable) {
            false
        }
    }

    private fun preparePlayerIfReady() {
        val activeSurface = surface ?: return
        val file = currentFile ?: return
        if (!file.exists() || file.length() <= 0L) return

        releasePlayerOnly()
        try {
            val player = MediaPlayer()
            player.setSurface(activeSurface)
            player.setDataSource(file.absolutePath)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            player.setOnVideoSizeChangedListener { _, w, h ->
                videoWidth = w
                videoHeight = h
                updateCenterCropMatrix()
            }
            player.setOnPreparedListener { mp ->
                isPrepared = true
                mp.setVolume(0f, 0f)
                updateCenterCropMatrix()
                if (playRequested && !isPowerSaveMode()) {
                    try { mp.start() } catch (_: Throwable) {}
                } else {
                    try { mp.seekTo(1) } catch (_: Throwable) {}
                }
            }
            player.setOnErrorListener { _, _, _ ->
                releasePlayerOnly()
                true
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (_: Throwable) {
            releasePlayerOnly()
        }
    }

    private fun updateCenterCropMatrix() {
        val view = textureView ?: return
        val vw = viewWidth.takeIf { it > 0 } ?: view.width
        val vh = viewHeight.takeIf { it > 0 } ?: view.height
        val mw = videoWidth
        val mh = videoHeight
        if (vw <= 0 || vh <= 0 || mw <= 0 || mh <= 0) return

        val viewRatio = vw.toFloat() / vh.toFloat()
        val videoRatio = mw.toFloat() / mh.toFloat()

        val scaleX: Float
        val scaleY: Float
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, vw / 2f, vh / 2f)
        view.post {
            try {
                view.setTransform(matrix)
            } catch (_: Throwable) {}
        }
    }

    private fun syncPlayback() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            if (playRequested && !isPowerSaveMode()) {
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

    private fun releasePlayerOnly() {
        isPrepared = false
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Throwable) {}
            try {
                player.reset()
            } catch (_: Throwable) {}
            try {
                player.release()
            } catch (_: Throwable) {}
        }
    }

    fun release() {
        releasePlayerOnly()
        try {
            surface?.release()
        } catch (_: Throwable) {}
        surface = null
        textureView = null
    }
}
