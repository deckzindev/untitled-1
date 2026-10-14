package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_builds")
data class ProjectBuildEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val projectName: String,
  val repoFullName: String,
  val branchName: String,
  val buildVariant: String, // "debug" or "release"
  val gradleTask: String = "assembleDebug",
  val commitSha: String? = null,
  val runId: Long? = null,
  val status: String = "queued", // "uploading", "queued", "in_progress", "completed", "failed"
  val conclusion: String? = null, // "success", "failure", "cancelled"
  val runUrl: String? = null,
  val artifactName: String? = null,
  val artifactId: Long? = null,
  val artifactDownloadUrl: String? = null,
  val localApkPath: String? = null,
  val apkFileName: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val completedAt: Long? = null,
  val totalFiles: Int = 0,
  val zipSizeBytes: Long = 0,
  val errorMessage: String? = null
)
