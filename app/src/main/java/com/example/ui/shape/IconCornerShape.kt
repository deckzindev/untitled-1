package com.example.ui.shape

import android.graphics.Path as AndroidPath
import androidx.compose.ui.graphics.Path as ComposePath

/**
 * BaseBezierPath implements continuous, high-precision cubic Bezier curves for icon shapes.
 * Recreates Samsung One UI (Sammy) icon geometry with exact control distances and corner scaling.
 */
open class BaseBezierPath(
    val controlDistanceX: Float = 0.4431717172f,
    val controlDistanceY: Float = 0.1401010101f,
    val cornerScale: Float = 1.0f
) {
    fun createAndroidPath(width: Float, height: Float, left: Float = 0f, top: Float = 0f): AndroidPath {
        val path = AndroidPath()
        val right = left + width
        val bottom = top + height

        val cdxW = controlDistanceX * cornerScale * width
        val cdyW = controlDistanceY * cornerScale * width
        val cdxH = controlDistanceX * cornerScale * height
        val cdyH = controlDistanceY * cornerScale * height

        // Top-Left Corner
        path.moveTo(left, top + cdxH)
        path.cubicTo(
            left, top + cdyH,
            left + cdyW, top,
            left + cdxW, top
        )

        // Top Edge & Top-Right Corner
        path.lineTo(right - cdxW, top)
        path.cubicTo(
            right - cdyW, top,
            right, top + cdyH,
            right, top + cdxH
        )

        // Right Edge & Bottom-Right Corner
        path.lineTo(right, bottom - cdxH)
        path.cubicTo(
            right, bottom - cdyH,
            right - cdyW, bottom,
            right - cdxW, bottom
        )

        // Bottom Edge & Bottom-Left Corner
        path.lineTo(left + cdxW, bottom)
        path.cubicTo(
            left + cdyW, bottom,
            left, bottom - cdyH,
            left, bottom - cdxH
        )

        // Left Edge & Close
        path.lineTo(left, top + cdxH)
        path.close()

        return path
    }

    fun createComposePath(width: Float, height: Float, left: Float = 0f, top: Float = 0f): ComposePath {
        val path = ComposePath()
        val right = left + width
        val bottom = top + height

        val cdxW = controlDistanceX * cornerScale * width
        val cdyW = controlDistanceY * cornerScale * width
        val cdxH = controlDistanceX * cornerScale * height
        val cdyH = controlDistanceY * cornerScale * height

        // Top-Left Corner
        path.moveTo(left, top + cdxH)
        path.cubicTo(
            left, top + cdyH,
            left + cdyW, top,
            left + cdxW, top
        )

        // Top Edge & Top-Right Corner
        path.lineTo(right - cdxW, top)
        path.cubicTo(
            right - cdyW, top,
            right, top + cdyH,
            right, top + cdxH
        )

        // Right Edge & Bottom-Right Corner
        path.lineTo(right, bottom - cdxH)
        path.cubicTo(
            right, bottom - cdyH,
            right - cdyW, bottom,
            right - cdxW, bottom
        )

        // Bottom Edge & Bottom-Left Corner
        path.lineTo(left + cdxW, bottom)
        path.cubicTo(
            left + cdyW, bottom,
            left, bottom - cdyH,
            left, bottom - cdxH
        )

        // Left Edge & Close
        path.lineTo(left, top + cdxH)
        path.close()

        return path
    }
}

object IconCornerShape {
    const val controlDistanceX: Float = 0.4431717172f
    const val controlDistanceY: Float = 0.1401010101f
    const val cornerScale: Float = 1.0f

    val baseBezierPath = BaseBezierPath(
        controlDistanceX = controlDistanceX,
        controlDistanceY = controlDistanceY,
        cornerScale = cornerScale
    )

    fun createAndroidSammyPath(width: Float, height: Float, left: Float = 0f, top: Float = 0f): AndroidPath {
        return baseBezierPath.createAndroidPath(width, height, left, top)
    }

    fun createComposeSammyPath(width: Float, height: Float, left: Float = 0f, top: Float = 0f): ComposePath {
        return baseBezierPath.createComposePath(width, height, left, top)
    }
}
