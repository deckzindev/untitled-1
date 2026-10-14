package com.example

import android.app.Application
import android.util.Log

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LauncherApplication", "Uncaught exception in thread ${thread.name}", throwable)
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
                // Prevent crash loop
            }
        }
    }
}

