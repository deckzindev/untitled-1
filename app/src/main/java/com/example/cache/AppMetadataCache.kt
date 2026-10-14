package com.example.cache

import android.content.Context
import android.util.Log
import com.example.model.AppInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

private const val TAG = "AppMetadataCache"
private const val FILE_NAME = "launcher_apps_metadata.json"

class AppMetadataCache(context: Context) {

    private val cacheFile = File(context.filesDir, FILE_NAME)
    private val lock = Any()

    /**
     * Loads the cached list of installed applications from persistent storage.
     * Returns null if cache does not exist or is corrupted.
     */
    fun loadFromDisk(): List<AppInfo>? {
        synchronized(lock) {
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                return null
            }

            return try {
                val jsonString = cacheFile.readText(StandardCharsets.UTF_8)
                val jsonArray = JSONArray(jsonString)
                val list = ArrayList<AppInfo>(jsonArray.length())

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val label = obj.optString("label", "")
                    val packageName = obj.optString("packageName", "")
                    val activityName = obj.optString("activityName", "")
                    val lastUpdateTime = obj.optLong("lastUpdateTime", 0L)
                    val normalizedLabel = obj.optString("normalizedLabel", "").ifEmpty {
                        AppInfo.normalize(label)
                    }

                    if (label.isNotEmpty() && packageName.isNotEmpty() && activityName.isNotEmpty()) {
                        list.add(AppInfo(label, packageName, activityName, lastUpdateTime, normalizedLabel))
                    }
                }

                if (list.isNotEmpty()) {
                    Log.d(TAG, "Successfully loaded ${list.size} apps from persistent metadata cache")
                    list
                } else {
                    null
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Metadata cache is corrupted or incompatible. Recovering safely...", e)
                try {
                    cacheFile.delete()
                } catch (_: Exception) {}
                null
            }
        }
    }

    /**
     * Atomically saves the list of applications to persistent storage using a temporary file.
     */
    fun saveToDisk(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        synchronized(lock) {
            val tmpFile = File(cacheFile.parentFile, "$FILE_NAME.tmp")
            try {
                val jsonArray = JSONArray()
                for (app in apps) {
                    val obj = JSONObject().apply {
                        put("label", app.label)
                        put("packageName", app.packageName)
                        put("activityName", app.activityName)
                        put("lastUpdateTime", app.lastUpdateTime)
                        put("normalizedLabel", app.normalizedLabel)
                    }
                    jsonArray.put(obj)
                }

                FileOutputStream(tmpFile).use { fos ->
                    OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                        writer.write(jsonArray.toString())
                        writer.flush()
                    }
                }

                if (tmpFile.exists() && tmpFile.length() > 0L) {
                    if (cacheFile.exists()) {
                        cacheFile.delete()
                    }
                    val success = tmpFile.renameTo(cacheFile)
                    if (success) {
                        Log.d(TAG, "Persisted ${apps.size} apps to disk cache")
                    } else {
                        Log.w(TAG, "Failed to rename tmp file to metadata cache file")
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error saving apps metadata cache to disk", e)
                try {
                    if (tmpFile.exists()) tmpFile.delete()
                } catch (_: Exception) {}
            }
        }
    }
}
