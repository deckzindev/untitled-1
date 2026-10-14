package com.example.ui.performance

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight, zero-allocation performance and recomposition inspector provided via CompositionLocal.
 * Tracks recomposition counts, layout pass durations, and heavy activity loading states across the
 * main screen loop so UI scopes can defer non-essential work and prevent redundant recompositions.
 */
@Stable
class LauncherPerformanceTracker(
    val isTrackingEnabled: Boolean = false
) {
    private val scopeCounters = ConcurrentHashMap<String, AtomicInteger>(16)
    private val scopeTimestamps = ConcurrentHashMap<String, AtomicLong>(16)
    private val totalRecompositions = AtomicLong(0L)
    private val heavyActivityLoading = AtomicBoolean(false)
    private val activeScrollOrGesture = AtomicBoolean(false)

    fun recordRecomposition(scopeName: String): Int {
        if (!isTrackingEnabled) return 0
        totalRecompositions.incrementAndGet()
        scopeTimestamps.getOrPut(scopeName) { AtomicLong(0L) }
            .lazySet(SystemClock.elapsedRealtime())
        return scopeCounters.getOrPut(scopeName) { AtomicInteger(0) }
            .incrementAndGet()
    }

    fun setHeavyActivityLoading(isLoading: Boolean) {
        heavyActivityLoading.lazySet(isLoading)
    }

    fun isHeavyActivityLoading(): Boolean = heavyActivityLoading.get()

    fun setScrollOrGestureActive(active: Boolean) {
        activeScrollOrGesture.lazySet(active)
    }

    fun isScrollOrGestureActive(): Boolean = activeScrollOrGesture.get()
}

val LocalLauncherPerformanceTracker = staticCompositionLocalOf {
    LauncherPerformanceTracker(isTrackingEnabled = false)
}

/**
 * Tracks recompositions of a specific UI scope without triggering any state reads or extra allocations.
 */
@Composable
fun TrackRecompositionScope(scopeName: String) {
    val tracker = LocalLauncherPerformanceTracker.current
    if (tracker.isTrackingEnabled) {
        SideEffect {
            tracker.recordRecomposition(scopeName)
        }
    }
}

