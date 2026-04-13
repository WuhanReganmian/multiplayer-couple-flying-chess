package com.couplechess.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.couplechess.data.model.TaskLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ===== Reads =====

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY level, createdAt")
    fun observeAllActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE level = :level AND isDeleted = 0 ORDER BY createdAt")
    fun observeByLevel(level: TaskLevel): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE level = :level AND isDeleted = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun randomByLevel(level: TaskLevel, count: Int = 1): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY level, createdAt")
    suspend fun getAllActive(): List<TaskEntity>

    // ===== Writes =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 0, updatedAt = :now WHERE isCustom = 0")
    suspend fun restoreDefaults(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE isCustom = 0")
    suspend fun deleteAllDefaults()

    @Query("DELETE FROM tasks WHERE isCustom = 1 AND isDeleted = 1")
    suspend fun purgeDeletedCustomTasks()

    // ===== Bulk seed =====

    @Query("SELECT COUNT(*) FROM tasks WHERE isCustom = 0")
    suspend fun countDefaults(): Int
}
