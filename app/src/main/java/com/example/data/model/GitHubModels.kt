package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
  val login: String,
  val id: Long? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  val name: String? = null,
  @Json(name = "public_repos") val publicRepos: Int = 0,
  @Json(name = "html_url") val htmlUrl: String? = null,
  val bio: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
  val id: Long? = null,
  val name: String,
  @Json(name = "full_name") val fullName: String,
  val private: Boolean = false,
  @Json(name = "default_branch") val defaultBranch: String = "main",
  @Json(name = "html_url") val htmlUrl: String? = null,
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
  val name: String,
  val description: String = "Repositório de compilação automatizada com GitHub Actions",
  val private: Boolean = true,
  @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateOrUpdateFileRequest(
  val message: String,
  val content: String, // base64 encoded
  val branch: String? = null,
  val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentFileResponse(
  val name: String? = null,
  val path: String? = null,
  val sha: String? = null,
  val size: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
  val content: String, // base64
  val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
  val sha: String,
  val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitTreeItem(
  val path: String,
  val mode: String = "100644", // 100644 for standard file, 100755 for executable
  val type: String = "blob",
  val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
  @Json(name = "base_tree") val baseTree: String? = null,
  val tree: List<GitTreeItem>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
  val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
  val message: String,
  val tree: String,
  val parents: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
  val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
  val ref: String, // e.g. "refs/heads/build/MyProject-12345"
  val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
  val sha: String,
  val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitObject(
  val sha: String,
  val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefResponse(
  val ref: String,
  @Json(name = "object") val gitObject: GitObject
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchPayload(
  val ref: String,
  val inputs: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
  val id: Long,
  val name: String? = null,
  @Json(name = "head_branch") val headBranch: String? = null,
  @Json(name = "head_sha") val headSha: String? = null,
  val status: String? = null, // "queued", "in_progress", "completed"
  val conclusion: String? = null, // "success", "failure", "cancelled"
  @Json(name = "html_url") val htmlUrl: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowJobsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  val jobs: List<WorkflowJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowJob(
  val id: Long,
  @Json(name = "run_id") val runId: Long,
  val name: String,
  val status: String? = null,
  val conclusion: String? = null,
  @Json(name = "html_url") val htmlUrl: String? = null,
  val steps: List<WorkflowStep>? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowStep(
  val name: String,
  val status: String? = null,
  val conclusion: String? = null,
  val number: Int = 0
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  val artifacts: List<GitHubArtifact> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubArtifact(
  val id: Long,
  val name: String,
  @Json(name = "size_in_bytes") val sizeInBytes: Long = 0,
  @Json(name = "archive_download_url") val archiveDownloadUrl: String? = null,
  val expired: Boolean = false
)
