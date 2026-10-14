# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================================================================
# R8 / ProGuard Performance & Package Management Optimizations
# ==============================================================================

# 1. R8 access modification for inlining
-allowaccessmodification
-repackageclasses 'com.example.internal'

# 2. Keep and optimize reflection targets used for system services & package management
# Used in LauncherScreen.expandNotificationShade via Class.forName("android.app.StatusBarManager")
-keep class android.app.StatusBarManager {
    public void expandNotificationsPanel();
    public void expandSettingsPanel();
}
-dontwarn android.app.StatusBarManager

# Preserve PackageManager, ResolveInfo, ActivityInfo, PackageInfo, and RoleManager structures
# queried heavily during app reconciliation, differential package updates, and icon pack resolution
-keep class android.content.pm.PackageManager { *; }
-keep class android.content.pm.PackageManager$ResolveInfoFlags { *; }
-keep class android.content.pm.PackageManager$PackageInfoFlags { *; }
-keep class android.content.pm.ResolveInfo { *; }
-keep class android.content.pm.ActivityInfo { *; }
-keep class android.content.pm.ApplicationInfo { *; }
-keep class android.content.pm.PackageInfo { *; }
-keep class android.app.role.RoleManager { *; }

# 3. Optimize XML Pull Parser reflection used when parsing external icon pack appfilter.xml
-keep class org.xmlpull.v1.XmlPullParser { *; }
-keep class org.xmlpull.v1.XmlPullParserFactory { *; }
-dontwarn org.xmlpull.v1.**

# 4. Keep immutable data models, WallpaperService, and cache structures intact for fast serialization & Compose skipping
-keep class com.example.wallpaper.VideoLiveWallpaperService { *; }
-keep class com.example.model.AppInfo { *; }
-keep class com.example.model.IconPackInfo { *; }
-keep class com.example.cache.BlurredWallpaperPair { *; }
-keep class com.example.cache.CachedIcon { *; }
-keep class com.example.ui.performance.LauncherPerformanceTracker { *; }

# 5. Strip verbose/debug logging calls in release builds to eliminate string allocations on hot paths
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 6. Eliminate Kotlin Intrinsics null-check overhead in release builds for hot UI & cache loops
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(java.lang.Object);
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}
