package com.example.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleProjectGenerator {

  fun generateSampleProject(): ZipAnalysisResult {
    val baos = ByteArrayOutputStream()
    val zos = ZipOutputStream(baos)

    fun addFile(path: String, content: String) {
      val entry = ZipEntry("SampleApp/$path")
      zos.putNextEntry(entry)
      zos.write(content.toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()
    }

    // settings.gradle.kts
    addFile(
      "settings.gradle.kts",
      """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SampleApp"
include(":app")
      """.trimIndent()
    )

    // root build.gradle.kts
    addFile(
      "build.gradle.kts",
      """
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
      """.trimIndent()
    )

    // gradle.properties
    addFile(
      "gradle.properties",
      """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
      """.trimIndent()
    )

    // app/build.gradle.kts
    addFile(
      "app/build.gradle.kts",
      """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sample.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sample.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
      """.trimIndent()
    )

    // app/src/main/AndroidManifest.xml
    addFile(
      "app/src/main/AndroidManifest.xml",
      """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="SampleApp"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
      """.trimIndent()
    )

    // app/src/main/java/com/sample/app/MainActivity.kt
    addFile(
      "app/src/main/java/com/sample/app/MainActivity.kt",
      """
package com.sample.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "App compilado com sucesso pelo GitBuilder CI!"
            textSize = 20f
            setPadding(40, 40, 40, 40)
        }
        setContentView(textView)
    }
}
      """.trimIndent()
    )

    zos.finish()
    zos.close()

    val bytes = baos.toByteArray()
    val inputStream = ByteArrayInputStream(bytes)
    return ZipProjectParser.parseZipStream(inputStream, "SampleAndroidApp.zip")
  }
}
