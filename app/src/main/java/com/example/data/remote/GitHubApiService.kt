package com.example.data.remote

import com.example.data.model.ArtifactsResponse
import com.example.data.model.ContentFileResponse
import com.example.data.model.CreateBlobRequest
import com.example.data.model.CreateBlobResponse
import com.example.data.model.CreateCommitRequest
import com.example.data.model.CreateCommitResponse
import com.example.data.model.CreateOrUpdateFileRequest
import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.CreateTreeRequest
import com.example.data.model.CreateTreeResponse
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.GitRefResponse
import com.example.data.model.UpdateRefRequest
import com.example.data.model.WorkflowDispatchPayload
import com.example.data.model.WorkflowJobsResponse
import com.example.data.model.WorkflowRun
import com.example.data.model.WorkflowRunsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GitHubApiService {

  @GET("user")
  suspend fun getCurrentUser(): Response<GitHubUser>

  @GET("user/repos")
  suspend fun getUserRepos(
    @Query("per_page") perPage: Int = 100,
    @Query("sort") sort: String = "updated"
  ): Response<List<GitHubRepo>>

  @POST("user/repos")
  suspend fun createRepo(
    @Body request: CreateRepoRequest
  ): Response<GitHubRepo>

  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun getFileContent(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Query("ref") ref: String? = null
  ): Response<ContentFileResponse>

  @PUT("repos/{owner}/{repo}/contents/{path}")
  suspend fun createOrUpdateFile(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Body request: CreateOrUpdateFileRequest
  ): Response<ContentFileResponse>

  @POST("repos/{owner}/{repo}/git/blobs")
  suspend fun createBlob(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body request: CreateBlobRequest
  ): Response<CreateBlobResponse>

  @POST("repos/{owner}/{repo}/git/trees")
  suspend fun createTree(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body request: CreateTreeRequest
  ): Response<CreateTreeResponse>

  @POST("repos/{owner}/{repo}/git/commits")
  suspend fun createCommit(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body request: CreateCommitRequest
  ): Response<CreateCommitResponse>

  @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
  suspend fun getBranchRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String
  ): Response<GitRefResponse>

  @POST("repos/{owner}/{repo}/git/refs")
  suspend fun createRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body request: CreateRefRequest
  ): Response<GitRefResponse>

  @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
  suspend fun updateRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
    @Body request: UpdateRefRequest
  ): Response<GitRefResponse>

  @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
  suspend fun dispatchWorkflow(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("workflow_id") workflowId: String,
    @Body payload: WorkflowDispatchPayload
  ): Response<ResponseBody>

  @GET("repos/{owner}/{repo}/actions/runs")
  suspend fun getWorkflowRuns(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("per_page") perPage: Int = 20,
    @Query("branch") branch: String? = null
  ): Response<WorkflowRunsResponse>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
  suspend fun getWorkflowRun(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long
  ): Response<WorkflowRun>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
  suspend fun getWorkflowJobs(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long
  ): Response<WorkflowJobsResponse>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
  suspend fun getWorkflowArtifacts(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long
  ): Response<ArtifactsResponse>

  @GET("repos/{owner}/{repo}/actions/jobs/{job_id}/logs")
  suspend fun getJobLogs(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("job_id") jobId: Long
  ): Response<ResponseBody>

  @Streaming
  @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
  suspend fun downloadArtifactZip(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("artifact_id") artifactId: Long
  ): Response<ResponseBody>

  @Streaming
  @GET
  suspend fun downloadFromUrl(
    @Url url: String
  ): Response<ResponseBody>
}
