package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Premium SVG ImageVector definitions for the launcher interface.
 * All icons follow a unified minimalist vector aesthetic with smooth curves
 * and consistent 24x24dp viewport scaling.
 */

val SvgSearchIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgSearchIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(15.5f, 14f)
    horizontalLineToRelative(-0.79f)
    lineToRelative(-0.28f, -0.27f)
    curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
    curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
    reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
    reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
    curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
    lineToRelative(0.27f, 0.28f)
    verticalLineToRelative(0.79f)
    lineToRelative(5f, 4.99f)
    lineTo(20.49f, 19f)
    lineToRelative(-4.99f, -5f)
    close()
    moveTo(9.5f, 14f)
    curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
    reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
    reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
    reflectiveCurveTo(11.99f, 14f, 9.5f, 14f)
    close()
  }.build()
}

val SvgCloseIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgCloseIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(19f, 6.41f)
    lineTo(17.59f, 5f)
    lineTo(12f, 10.59f)
    lineTo(6.41f, 5f)
    lineTo(5f, 6.41f)
    lineTo(10.59f, 12f)
    lineTo(5f, 17.59f)
    lineTo(6.41f, 19f)
    lineTo(12f, 13.41f)
    lineTo(17.59f, 19f)
    lineTo(19f, 17.59f)
    lineTo(13.41f, 12f)
    close()
  }.build()
}

val SvgPersonalizationIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgPersonalizationIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(12f, 3f)
    curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
    curveToRelative(0f, 4.97f, 4.03f, 9f, 9f, 9f)
    curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
    curveToRelative(0f, -0.39f, -0.15f, -0.74f, -0.39f, -1.01f)
    curveToRelative(-0.23f, -0.26f, -0.38f, -0.61f, -0.38f, -0.99f)
    curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
    horizontalLineTo(16f)
    curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
    curveToRelative(0f, -4.97f, -4.03f, -9f, -9f, -9f)
    close()
    moveTo(6.5f, 11.5f)
    curveTo(5.67f, 11.5f, 5f, 10.83f, 5f, 10f)
    reflectiveCurveTo(5.67f, 8.5f, 6.5f, 8.5f)
    reflectiveCurveTo(8f, 9.17f, 8f, 10f)
    reflectiveCurveTo(7.33f, 11.5f, 6.5f, 11.5f)
    close()
    moveTo(9.5f, 7.5f)
    curveTo(8.67f, 7.5f, 8f, 6.83f, 8f, 6f)
    reflectiveCurveTo(8.67f, 4.5f, 9.5f, 4.5f)
    reflectiveCurveTo(11f, 5.17f, 11f, 6f)
    reflectiveCurveTo(10.33f, 7.5f, 9.5f, 7.5f)
    close()
    moveTo(14.5f, 7.5f)
    curveTo(13.67f, 7.5f, 13f, 6.83f, 13f, 6f)
    reflectiveCurveTo(13.67f, 4.5f, 14.5f, 4.5f)
    reflectiveCurveTo(16f, 5.17f, 16f, 6f)
    reflectiveCurveTo(15.33f, 7.5f, 14.5f, 7.5f)
    close()
    moveTo(17.5f, 11.5f)
    curveTo(16.67f, 11.5f, 16f, 10.83f, 16f, 10f)
    reflectiveCurveTo(16.67f, 8.5f, 17.5f, 8.5f)
    reflectiveCurveTo(19f, 9.17f, 19f, 10f)
    reflectiveCurveTo(18.33f, 11.5f, 17.5f, 11.5f)
    close()
  }.build()
}

val SvgSettingsIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgSettingsIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(19.14f, 12.94f)
    curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
    curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
    lineToRelative(2.03f, -1.58f)
    curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
    lineToRelative(-1.92f, -3.32f)
    curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
    lineToRelative(-2.39f, 0.96f)
    curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
    lineToRelative(-0.36f, -2.54f)
    curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
    horizontalLineToRelative(-3.84f)
    curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
    lineToRelative(-0.36f, 2.54f)
    curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
    lineToRelative(-2.39f, -0.96f)
    curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
    lineTo(2.74f, 8.87f)
    curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
    lineToRelative(2.03f, 1.58f)
    curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
    reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
    lineToRelative(-2.03f, 1.58f)
    curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
    lineToRelative(1.92f, 3.32f)
    curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
    lineToRelative(2.39f, -0.96f)
    curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
    lineToRelative(0.36f, 2.54f)
    curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
    horizontalLineToRelative(3.84f)
    curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
    lineToRelative(0.36f, -2.54f)
    curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
    lineToRelative(2.39f, 0.96f)
    curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
    lineToRelative(1.92f, -3.32f)
    curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
    lineToRelative(-2.01f, -1.58f)
    close()
    moveTo(12f, 15.6f)
    curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
    reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
    reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
    reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
    close()
  }.build()
}

val SvgInfoIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgInfoIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(12f, 2f)
    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
    curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
    curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
    curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
    close()
    moveTo(12f, 20f)
    curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
    curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
    curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
    curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
    close()
    moveTo(11f, 7f)
    lineTo(13f, 7f)
    lineTo(13f, 9f)
    lineTo(11f, 9f)
    close()
    moveTo(11f, 11f)
    lineTo(13f, 11f)
    lineTo(13f, 17f)
    lineTo(11f, 17f)
    close()
  }.build()
}

val SvgDockAddIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgDockAddIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(19f, 13f)
    lineTo(13f, 13f)
    lineTo(13f, 19f)
    lineTo(11f, 19f)
    lineTo(11f, 13f)
    lineTo(5f, 13f)
    lineTo(5f, 11f)
    lineTo(11f, 11f)
    lineTo(11f, 5f)
    lineTo(13f, 5f)
    lineTo(13f, 11f)
    lineTo(19f, 11f)
    close()
  }.build()
}

val SvgDockRemoveIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgDockRemoveIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(19f, 13f)
    lineTo(5f, 13f)
    lineTo(5f, 11f)
    lineTo(19f, 11f)
    close()
  }.build()
}

val SvgUninstallIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgUninstallIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color(0xFFFF453A))
  ) {
    moveTo(16f, 9f)
    lineTo(16f, 19f)
    lineTo(8f, 19f)
    lineTo(8f, 9f)
    lineTo(16f, 9f)
    close()
    moveTo(14.5f, 3f)
    lineTo(9.5f, 3f)
    lineTo(8.5f, 4f)
    lineTo(5f, 4f)
    lineTo(5f, 6f)
    lineTo(19f, 6f)
    lineTo(19f, 4f)
    lineTo(15.5f, 4f)
    close()
    moveTo(18f, 7f)
    lineTo(6f, 7f)
    lineTo(6f, 19f)
    curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
    lineTo(16f, 21f)
    curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
    lineTo(18f, 7f)
    close()
  }.build()
}

val SvgHideIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgHideIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(12f, 4.5f)
    curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
    curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
    curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
    curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
    close()
    moveTo(12f, 17f)
    curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
    curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
    curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
    curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
    close()
    moveTo(12f, 9f)
    curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
    curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
    curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
    curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
    close()
  }.build()
}

val SvgEmptyAppsIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgEmptyAppsIcon",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
  ).path(
    fill = SolidColor(Color(0x808E8E93))
  ) {
    moveTo(12f, 8f)
    curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
    verticalLineToRelative(24f)
    curveToRelative(0f, 2.21f, 1.79f, 4f, 4f, 4f)
    horizontalLineToRelative(24f)
    curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
    verticalLineTo(12f)
    curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
    horizontalLineTo(12f)
    close()
    moveTo(12f, 12f)
    horizontalLineToRelative(24f)
    verticalLineToRelative(24f)
    horizontalLineTo(12f)
    verticalLineTo(12f)
    close()
    moveTo(16f, 16f)
    verticalLineToRelative(6f)
    horizontalLineToRelative(6f)
    verticalLineToRelative(-6f)
    horizontalLineToRelative(-6f)
    close()
    moveTo(26f, 16f)
    verticalLineToRelative(6f)
    horizontalLineToRelative(6f)
    verticalLineToRelative(-6f)
    horizontalLineToRelative(-6f)
    close()
    moveTo(16f, 26f)
    verticalLineToRelative(6f)
    horizontalLineToRelative(6f)
    verticalLineToRelative(-6f)
    horizontalLineToRelative(-6f)
    close()
    moveTo(26f, 26f)
    verticalLineToRelative(6f)
    horizontalLineToRelative(6f)
    verticalLineToRelative(-6f)
    horizontalLineToRelative(-6f)
    close()
  }.build()
}

val SvgGalleryIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgGalleryIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(22f, 16f)
    lineTo(22f, 4f)
    curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
    lineTo(8f, 2f)
    curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f)
    lineTo(6f, 16f)
    curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f)
    lineTo(20f, 18f)
    curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
    close()
    moveTo(11f, 12f)
    lineToRelative(2.03f, 2.71f)
    lineTo(16f, 11.5f)
    lineToRelative(4f, 5f)
    lineTo(8f, 16.5f)
    lineToRelative(3f, -4f)
    close()
    moveTo(2f, 6f)
    lineTo(2f, 20f)
    curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
    lineTo(18f, 22f)
    lineTo(18f, 20f)
    lineTo(4f, 20f)
    lineTo(4f, 6f)
    lineTo(2f, 6f)
    close()
  }.build()
}

val SvgLockIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgLockIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(18f, 8f)
    horizontalLineToRelative(-1f)
    verticalLineTo(6f)
    curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
    reflectiveCurveTo(7f, 3.24f, 7f, 6f)
    verticalLineToRelative(2f)
    horizontalLineTo(6f)
    curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
    verticalLineToRelative(10f)
    curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
    horizontalLineToRelative(12f)
    curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
    verticalLineTo(10f)
    curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
    close()
    moveTo(12f, 17f)
    curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
    reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
    reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
    reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
    close()
    moveTo(9f, 8f)
    verticalLineTo(6f)
    curveToRelative(0f, -1.66f, 1.34f, -3f, 3f, -3f)
    reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
    verticalLineToRelative(2f)
    horizontalLineTo(9f)
    close()
  }.build()
}

val SvgShapeCategoryIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgShapeCategoryIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(17f, 3f)
    horizontalLineTo(7f)
    curveTo(4.79f, 3f, 3f, 4.79f, 3f, 7f)
    verticalLineToRelative(10f)
    curveToRelative(0f, 2.21f, 1.79f, 4f, 4f, 4f)
    horizontalLineToRelative(10f)
    curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
    verticalLineTo(7f)
    curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
    close()
    moveTo(18.5f, 17f)
    curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
    horizontalLineTo(7f)
    curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
    verticalLineTo(7f)
    curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
    horizontalLineToRelative(10f)
    curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
    verticalLineToRelative(10f)
    close()
  }.build()
}

val SvgIconPackCategoryIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "SvgIconPackCategoryIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).path(
    fill = SolidColor(Color.White)
  ) {
    moveTo(4f, 4f)
    horizontalLineToRelative(5.5f)
    curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
    verticalLineToRelative(5.5f)
    curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
    horizontalLineTo(4f)
    curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
    verticalLineTo(5.5f)
    curveTo(2.5f, 4.67f, 3.17f, 4f, 4f, 4f)
    close()

    moveTo(14.5f, 4f)
    horizontalLineTo(20f)
    curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
    verticalLineToRelative(5.5f)
    curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
    horizontalLineToRelative(-5.5f)
    curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
    verticalLineTo(5.5f)
    curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
    close()

    moveTo(4f, 14.5f)
    horizontalLineToRelative(5.5f)
    curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
    verticalLineTo(20f)
    curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
    horizontalLineTo(4f)
    curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
    verticalLineToRelative(-4f)
    curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
    close()

    moveTo(14.5f, 14.5f)
    horizontalLineTo(20f)
    curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
    verticalLineTo(20f)
    curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
    horizontalLineToRelative(-5.5f)
    curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
    verticalLineToRelative(-4f)
    curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
    close()
  }.build()
}
