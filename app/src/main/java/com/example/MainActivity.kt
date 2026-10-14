package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.security.SecurityAuthHelper
import com.example.ui.LauncherScreen

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels()
  private var packageReceiver: BroadcastReceiver? = null
  private var wallpaperReceiver: BroadcastReceiver? = null

  private var pendingAuthSuccess: (() -> Unit)? = null
  private var pendingAuthError: ((String) -> Unit)? = null

  private val deviceCredentialLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      val onSuccess = pendingAuthSuccess
      pendingAuthSuccess = null
      pendingAuthError = null
      onSuccess?.invoke()
    } else {
      val onError = pendingAuthError
      pendingAuthSuccess = null
      pendingAuthError = null
      onError?.invoke("Autenticação cancelada")
    }
  }

  fun requestSecurityAuth(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: ((String) -> Unit)? = null
  ) {
    if (!SecurityAuthHelper.isDeviceSecure(this)) {
      Toast.makeText(
        this,
        "Configure um bloqueio de tela no Android para proteger aplicativos.",
        Toast.LENGTH_LONG
      ).show()
      SecurityAuthHelper.openSecuritySettings(this)
      onError?.invoke("Dispositivo não protegido")
      return
    }

    SecurityAuthHelper.authenticate(
      activity = this,
      title = title,
      subtitle = subtitle,
      onSuccess = onSuccess,
      onError = onError,
      onFallbackToKeyguard = { intent ->
        pendingAuthSuccess = onSuccess
        pendingAuthError = onError
        deviceCredentialLauncher.launch(intent)
      }
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
      window.isStatusBarContrastEnforced = false
    }
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_CHANGED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addDataScheme("package")
    }

    packageReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        try {
          val packageName = intent?.data?.schemeSpecificPart
          val action = intent?.action
          if (!packageName.isNullOrEmpty() && packageName != applicationContext.packageName) {
            when (action) {
              Intent.ACTION_PACKAGE_REMOVED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                  viewModel.onPackageRemoved(packageName)
                }
              }
              Intent.ACTION_PACKAGE_ADDED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                  viewModel.onPackageAdded(packageName)
                } else {
                  viewModel.onPackageChanged(packageName)
                }
              }
              Intent.ACTION_PACKAGE_CHANGED, Intent.ACTION_PACKAGE_REPLACED -> {
                viewModel.onPackageChanged(packageName)
              }
              else -> {
                viewModel.invalidatePackage(packageName)
              }
            }
          }
        } catch (e: Throwable) {
          Log.e(TAG, "Error in packageReceiver onReceive", e)
        }
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
      } else {
        registerReceiver(packageReceiver, filter)
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to register packageReceiver", e)
    }

    @Suppress("DEPRECATION")
    val wallpaperFilter = IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
    wallpaperReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        viewModel.refreshWallpaper()
      }
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(wallpaperReceiver, wallpaperFilter, Context.RECEIVER_EXPORTED)
      } else {
        registerReceiver(wallpaperReceiver, wallpaperFilter)
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to register wallpaperReceiver", e)
    }

    setContent {
      LauncherTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          LauncherScreen(
            viewModel = viewModel,
            onRequestSecurityAuth = ::requestSecurityAuth
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    try {
      viewModel.onLauncherResumed()
    } catch (e: Throwable) {
      Log.e(TAG, "Error in onResume", e)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    try {
      viewModel.onHomePressed()
    } catch (e: Throwable) {
      Log.e(TAG, "Error handling onNewIntent", e)
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    try {
      if (
        level >= TRIM_MEMORY_UI_HIDDEN ||
        level >= TRIM_MEMORY_RUNNING_LOW ||
        level == TRIM_MEMORY_RUNNING_CRITICAL ||
        level == TRIM_MEMORY_COMPLETE ||
        level == TRIM_MEMORY_MODERATE
      ) {
        viewModel.trimMemory(level)
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Error in onTrimMemory", e)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    packageReceiver?.let {
      try {
        unregisterReceiver(it)
      } catch (e: Exception) {
        Log.w(TAG, "Exception unregistering packageReceiver", e)
      }
    }
    wallpaperReceiver?.let {
      try {
        unregisterReceiver(it)
      } catch (e: Exception) {
        Log.w(TAG, "Exception unregistering wallpaperReceiver", e)
      }
    }
  }
}


