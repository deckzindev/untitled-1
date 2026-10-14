package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProjectBuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectBuildDao {
  @Query("SELECT * FROM project_builds ORDER BY createdAt DESC")
  fun getAllBuilds(): Flow<List<ProjectBuildEntity>>

  @Query("SELECT * FROM project_builds WHERE id = :id LIMIT 1")
  suspend fun getBuildById(id: Long): ProjectBuildEntity?

  @Query("SELECT * FROM project_builds WHERE status IN ('uploading', 'queued', 'in_progress') ORDER BY createdAt DESC")
  fun getActiveBuilds(): Flow<List<ProjectBuildEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBuild(build: ProjectBuildEntity): Long

  @Update
  suspend fun updateBuild(build: ProjectBuildEntity)

  @Delete
  suspend fun deleteBuild(build: ProjectBuildEntity)

  @Query("DELETE FROM project_builds WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM project_builds")
  suspend fun clearAll()
}
