package com.example.data.remote

import android.util.Base64
import com.example.data.model.CreateBlobRequest
import com.example.data.model.CreateCommitRequest
import com.example.data.model.CreateOrUpdateFileRequest
import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.CreateTreeRequest
import com.example.data.model.GitHubArtifact
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.GitTreeItem
import com.example.data.model.WorkflowJob
import com.example.data.model.WorkflowRun
import com.example.data.workflow.WorkflowTemplate
import com.example.util.ProjectFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

sealed class BuildStepProgress {
  data class UploadingFiles(val current: Int, val total: Int, val currentFile: String) : BuildStepProgress()
  data object CreatingTree : BuildStepProgress()
  data object CreatingCommit : BuildStepProgress()
  data object UpdatingBranch : BuildStepProgress()
  data object InstallingWorkflow : BuildStepProgress()
  data object WaitingForRunner : BuildStepProgress()
  data class WorkflowStarted(val runId: Long, val runUrl: String) : BuildStepProgress()
}

class GitHubRepository(private val apiService: GitHubApiService) {

  suspend fun validateToken(): Result<GitHubUser> = withContext(Dispatchers.IO) {
    try {
      val response = apiService.getCurrentUser()
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!)
      } else {
        Result.failure(Exception("Token inválido ou não autorizado (Código HTTP ${response.code()})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getUserRepos(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
    try {
      val response = apiService.getUserRepos()
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!)
      } else {
        Result.failure(Exception("Erro ao buscar repositórios (${response.code()})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createCompilerRepository(repoName: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
    try {
      val req = CreateRepoRequest(
        name = repoName,
        description = "Repositório dedicado para compilação de APKs com GitBuilder CI",
        private = true,
        autoInit = true
      )
      val response = apiService.createRepo(req)
      if (response.isSuccessful && response.body() != null) {
        val repo = response.body()!!
        // Install workflow immediately in new repo
        installWorkflow(repo.fullName)
        Result.success(repo)
      } else {
        Result.failure(Exception("Erro ao criar repositório (${response.code()}: ${response.errorBody()?.string()})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun installWorkflow(repoFullName: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val owner = parts[0]
      val repo = parts[1]

      // Check if workflow file exists
      val existing = apiService.getFileContent(owner, repo, WorkflowTemplate.WORKFLOW_PATH)
      val sha = if (existing.isSuccessful) existing.body()?.sha else null

      val base64Workflow = Base64.encodeToString(
        WorkflowTemplate.WORKFLOW_YAML.toByteArray(StandardCharsets.UTF_8),
        Base64.NO_WRAP
      )

      val request = CreateOrUpdateFileRequest(
        message = "ci: Configurar workflow de compilação GitBuilder CI",
        content = base64Workflow,
        sha = sha
      )

      val response = apiService.createOrUpdateFile(owner, repo, WorkflowTemplate.WORKFLOW_PATH, request)
      if (response.isSuccessful) {
        Result.success(true)
      } else {
        Result.failure(Exception("Falha ao gravar workflow: HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushProjectAndStartBuild(
    repoFullName: String,
    projectName: String,
    files: List<ProjectFileInfo>,
    buildVariant: String,
    onProgress: (BuildStepProgress) -> Unit
  ): Result<Triple<String, Long?, String?>> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Formato de repositório inválido. Use usuário/repositório"))
      val owner = parts[0]
      val repo = parts[1]

      // Verificar se o repositório existe; se retornar 404, cria automaticamente!
      val checkResp = apiService.getFileContent(owner, repo, "README.md")
      if (checkResp.code() == 404) {
        val userRepos = apiService.getUserRepos(perPage = 100)
        val exists = userRepos.body()?.any { it.name.equals(repo, ignoreCase = true) } == true
        if (!exists) {
          val createReq = CreateRepoRequest(
            name = repo,
            description = "Repositório de compilação automatizada com GitBuilder CI",
            private = true,
            autoInit = true
          )
          val createdResp = apiService.createRepo(createReq)
          if (createdResp.isSuccessful) {
            kotlinx.coroutines.delay(2000)
            installWorkflow(repoFullName)
          }
        }
      }

      val safeName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val branchName = "build/$safeName-${System.currentTimeMillis()}"

      val treeItems = mutableListOf<GitTreeItem>()

      // 1. Upload blobs
      files.forEachIndexed { index, fileInfo ->
        onProgress(BuildStepProgress.UploadingFiles(index + 1, files.size + 1, fileInfo.path))
        val blobReq = CreateBlobRequest(content = fileInfo.base64Content)
        val blobResp = apiService.createBlob(owner, repo, blobReq)
        if (!blobResp.isSuccessful || blobResp.body() == null) {
          throw Exception("Falha ao enviar arquivo ${fileInfo.path}: HTTP ${blobResp.code()}")
        }
        val mode = if (fileInfo.isExecutable) "100755" else "100644"
        treeItems.add(
          GitTreeItem(
            path = fileInfo.path,
            mode = mode,
            type = "blob",
            sha = blobResp.body()!!.sha
          )
        )
      }

      // 2. Ensure workflow file is in the tree
      val workflowBase64 = Base64.encodeToString(
        WorkflowTemplate.WORKFLOW_YAML.toByteArray(StandardCharsets.UTF_8),
        Base64.NO_WRAP
      )
      val wfBlob = apiService.createBlob(owner, repo, CreateBlobRequest(content = workflowBase64))
      if (wfBlob.isSuccessful && wfBlob.body() != null) {
        treeItems.add(
          GitTreeItem(
            path = WorkflowTemplate.WORKFLOW_PATH,
            mode = "100644",
            type = "blob",
            sha = wfBlob.body()!!.sha
          )
        )
      }

      // 3. Create Tree
      onProgress(BuildStepProgress.CreatingTree)
      val treeReq = CreateTreeRequest(tree = treeItems)
      val treeResp = apiService.createTree(owner, repo, treeReq)
      if (!treeResp.isSuccessful || treeResp.body() == null) {
        throw Exception("Falha ao criar árvore Git: HTTP ${treeResp.code()}")
      }
      val treeSha = treeResp.body()!!.sha

      // 4. Create Commit
      onProgress(BuildStepProgress.CreatingCommit)
      val commitReq = CreateCommitRequest(
        message = "GitBuilder CI: Compilando $projectName [$buildVariant]",
        tree = treeSha
      )
      val commitResp = apiService.createCommit(owner, repo, commitReq)
      if (!commitResp.isSuccessful || commitResp.body() == null) {
        throw Exception("Falha ao criar commit: HTTP ${commitResp.code()}")
      }
      val commitSha = commitResp.body()!!.sha

      // 5. Create Branch ref
      onProgress(BuildStepProgress.UpdatingBranch)
      val refReq = CreateRefRequest(
        ref = "refs/heads/$branchName",
        sha = commitSha
      )
      val refResp = apiService.createRef(owner, repo, refReq)
      if (!refResp.isSuccessful) {
        throw Exception("Falha ao criar branch $branchName: HTTP ${refResp.code()}")
      }

      // 6. Wait briefly and locate triggered workflow run
      onProgress(BuildStepProgress.WaitingForRunner)
      var runId: Long? = null
      var runUrl: String? = null

      for (attempt in 1..8) {
        kotlinx.coroutines.delay(2000)
        val runsResp = apiService.getWorkflowRuns(owner, repo, perPage = 5, branch = branchName)
        if (runsResp.isSuccessful) {
          val runs = runsResp.body()?.workflowRuns.orEmpty()
          val matched = runs.firstOrNull { it.headSha == commitSha || it.headBranch == branchName }
          if (matched != null) {
            runId = matched.id
            runUrl = matched.htmlUrl
            onProgress(BuildStepProgress.WorkflowStarted(matched.id, matched.htmlUrl ?: ""))
            break
          }
        }
      }

      Result.success(Triple(branchName, runId, commitSha))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getRunStatus(repoFullName: String, runId: Long): Result<WorkflowRun> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val response = apiService.getWorkflowRun(parts[0], parts[1], runId)
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!)
      } else {
        Result.failure(Exception("HTTP ${response.code()} ao consultar execução"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getRunJobs(repoFullName: String, runId: Long): Result<List<WorkflowJob>> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val response = apiService.getWorkflowJobs(parts[0], parts[1], runId)
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!.jobs)
      } else {
        Result.failure(Exception("HTTP ${response.code()} ao buscar passos"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getArtifacts(repoFullName: String, runId: Long): Result<List<GitHubArtifact>> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val response = apiService.getWorkflowArtifacts(parts[0], parts[1], runId)
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!.artifacts)
      } else {
        Result.failure(Exception("HTTP ${response.code()} ao buscar artefatos"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getJobLogs(repoFullName: String, jobId: Long): Result<String> = withContext(Dispatchers.IO) {
    try {
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val response = apiService.getJobLogs(parts[0], parts[1], jobId)
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!.string())
      } else {
        Result.failure(Exception("HTTP ${response.code()} ao buscar logs da execução"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun downloadAndExtractApk(
    repoFullName: String,
    artifactId: Long?,
    artifactUrl: String?,
    destinationDir: File,
    preferredName: String
  ): Result<File> = withContext(Dispatchers.IO) {
    try {
      destinationDir.mkdirs()
      val parts = repoFullName.split("/")
      if (parts.size != 2) return@withContext Result.failure(Exception("Nome de repositório inválido"))
      val owner = parts[0]
      val repo = parts[1]

      val response = if (artifactId != null && artifactId > 0) {
        apiService.downloadArtifactZip(owner, repo, artifactId)
      } else if (!artifactUrl.isNullOrBlank()) {
        apiService.downloadFromUrl(artifactUrl)
      } else {
        return@withContext Result.failure(Exception("Nenhum artefato disponível para download"))
      }

      if (!response.isSuccessful || response.body() == null) {
        return@withContext Result.failure(Exception("Falha no download do artefato: HTTP ${response.code()}"))
      }

      val body = response.body()!!
      val sanitizedBaseName = preferredName.replace(Regex("[^a-zA-Z0-9._-]"), "_").removeSuffix(".apk")
      val outputFile = File(destinationDir, "$sanitizedBaseName.apk")

      var foundApk = false
      val zipIn = ZipInputStream(body.byteStream())
      var entry = zipIn.nextEntry

      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
          outputFile.outputStream().use { out ->
            zipIn.copyTo(out)
          }
          foundApk = true
          zipIn.closeEntry()
          break
        }
        zipIn.closeEntry()
        entry = zipIn.nextEntry
      }

      if (!foundApk) {
        return@withContext Result.failure(Exception("O arquivo baixado não contém um pacote .apk."))
      }

      Result.success(outputFile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
