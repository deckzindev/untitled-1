package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ProjectFileInfo(
  val path: String,
  val base64Content: String,
  val sizeBytes: Long,
  val isExecutable: Boolean = false
)

data class ZipAnalysisResult(
  val isValidAndroidProject: Boolean,
  val detectedProjectName: String,
  val files: List<ProjectFileInfo>,
  val totalFiles: Int,
  val totalSizeBytes: Long,
  val hasGradleWrapper: Boolean,
  val hasSettingsGradle: Boolean,
  val hasAppModule: Boolean,
  val error: String? = null
)

object ZipProjectParser {

  private val IGNORED_PREFIXES = listOf(
    ".gradle/",
    "build/",
    "app/build/",
    ".idea/",
    ".git/",
    ".kotlin/",
    ".DS_Store",
    "__MACOSX/"
  )

  private val IGNORED_FILES = setOf(
    "local.properties",
    ".DS_Store",
    "Thumbs.db"
  )

  fun analyzeAndExtractZip(context: Context, uri: Uri): ZipAnalysisResult {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri)
        ?: return ZipAnalysisResult(
          isValidAndroidProject = false,
          detectedProjectName = "Desconhecido",
          files = emptyList(),
          totalFiles = 0,
          totalSizeBytes = 0,
          hasGradleWrapper = false,
          hasSettingsGradle = false,
          hasAppModule = false,
          error = "Não foi possível abrir o arquivo ZIP selecionado."
        )

      val result = parseZipStream(inputStream, extractFileName(context, uri))
      inputStream.close()
      result
    } catch (e: Exception) {
      ZipAnalysisResult(
        isValidAndroidProject = false,
        detectedProjectName = "Erro",
        files = emptyList(),
        totalFiles = 0,
        totalSizeBytes = 0,
        hasGradleWrapper = false,
        hasSettingsGradle = false,
        hasAppModule = false,
        error = e.localizedMessage ?: "Erro ao analisar arquivo ZIP."
      )
    }
  }

  fun parseZipStream(inputStream: InputStream, defaultProjectName: String): ZipAnalysisResult {
    val zipStream = ZipInputStream(inputStream)
    val filesList = mutableListOf<ProjectFileInfo>()
    var totalBytes = 0L
    var hasGradleWrapper = false
    var hasSettingsGradle = false
    var hasAppModule = false
    var detectedProjectName = defaultProjectName.removeSuffix(".zip")

    var entry: ZipEntry? = zipStream.nextEntry

    // Detect common root folder in zip if everything is inside a single parent directory
    val rawEntries = mutableListOf<Pair<String, ByteArray>>()
    while (entry != null) {
      if (!entry.isDirectory) {
        val path = entry.name.replace('\\', '/')
        val shouldIgnore = IGNORED_PREFIXES.any { path.contains(it) } ||
            IGNORED_FILES.any { path.endsWith(it) }

        if (!shouldIgnore) {
          val buffer = ByteArrayOutputStream()
          val temp = ByteArray(4096)
          var len: Int
          while (zipStream.read(temp).also { len = it } > 0) {
            buffer.write(temp, 0, len)
          }
          val bytes = buffer.toByteArray()
          rawEntries.add(path to bytes)
        }
      }
      zipStream.closeEntry()
      entry = zipStream.nextEntry
    }

    if (rawEntries.isEmpty()) {
      return ZipAnalysisResult(
        isValidAndroidProject = false,
        detectedProjectName = detectedProjectName,
        files = emptyList(),
        totalFiles = 0,
        totalSizeBytes = 0,
        hasGradleWrapper = false,
        hasSettingsGradle = false,
        hasAppModule = false,
        error = "O arquivo ZIP está vazio ou não contém arquivos válidos."
      )
    }

    // Check if all files share a common root directory prefix (e.g. "MyProject/...")
    val firstSlash = rawEntries.first().first.indexOf('/')
    val normalizedEntries: List<Pair<String, ByteArray>> = if (firstSlash > 0) {
      val candidatePrefix = rawEntries.first().first.substring(0, firstSlash + 1)
      val allSharePrefix = rawEntries.all { it.first.startsWith(candidatePrefix) }
      if (allSharePrefix) {
        detectedProjectName = candidatePrefix.removeSuffix("/")
        rawEntries.map { (path, bytes) -> path.removePrefix(candidatePrefix) to bytes }
      } else {
        rawEntries
      }
    } else {
      rawEntries
    }

    for ((cleanPath, bytes) in normalizedEntries) {
      if (cleanPath.isBlank()) continue
      totalBytes += bytes.size

      if (cleanPath == "gradlew" || cleanPath.endsWith("/gradlew")) {
        hasGradleWrapper = true
      }
      if (cleanPath.startsWith("settings.gradle")) {
        hasSettingsGradle = true
      }
      if (cleanPath.startsWith("app/")) {
        hasAppModule = true
      }

      val isExecutable = cleanPath == "gradlew" || cleanPath.endsWith(".sh")
      val base64 = encodeBase64(bytes)

      filesList.add(
        ProjectFileInfo(
          path = cleanPath,
          base64Content = base64,
          sizeBytes = bytes.size.toLong(),
          isExecutable = isExecutable
        )
      )
    }

    val isValid = hasSettingsGradle || hasAppModule || filesList.any { it.path.contains("build.gradle") }

    return ZipAnalysisResult(
      isValidAndroidProject = isValid,
      detectedProjectName = detectedProjectName.ifBlank { "AndroidApp" },
      files = filesList,
      totalFiles = filesList.size,
      totalSizeBytes = totalBytes,
      hasGradleWrapper = hasGradleWrapper,
      hasSettingsGradle = hasSettingsGradle,
      hasAppModule = hasAppModule,
      error = if (!isValid) "Aviso: Estrutura padrão do Android (build.gradle/settings.gradle) não foi detectada, mas você ainda pode compilar." else null
    )
  }

  private fun extractFileName(context: Context, uri: Uri): String {
    var name = "projeto.zip"
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          name = cursor.getString(nameIndex) ?: name
        }
      }
    } catch (_: Exception) {
      name = uri.lastPathSegment ?: "projeto.zip"
    }
    return name
  }

  fun encodeBase64(bytes: ByteArray): String {
    return try {
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Throwable) {
      java.util.Base64.getEncoder().encodeToString(bytes)
    }
  }
}
