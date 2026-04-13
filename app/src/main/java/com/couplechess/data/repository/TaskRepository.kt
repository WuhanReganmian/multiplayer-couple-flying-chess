package com.couplechess.data.repository

import com.couplechess.data.db.TaskDao
import com.couplechess.data.db.TaskEntity
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val dao: TaskDao) {

    // ===== Observable queries =====

    fun observeAll(): Flow<List<Task>> =
        dao.observeAllActive().map { list -> list.map(TaskEntity::toModel) }

    fun observeByLevel(level: TaskLevel): Flow<List<Task>> =
        dao.observeByLevel(level).map { list -> list.map(TaskEntity::toModel) }

    // ===== Suspend queries =====

    suspend fun getRandomTask(level: TaskLevel): Task? =
        dao.randomByLevel(level, 1).firstOrNull()?.toModel()

    suspend fun getRandomTasksForGame(levels: Map<TaskLevel, Int>): Map<TaskLevel, List<Task>> =
        levels.mapValues { (level, count) ->
            dao.randomByLevel(level, count).map(TaskEntity::toModel)
        }

    suspend fun getAllGroupedByLevel(): Map<TaskLevel, List<Task>> =
        dao.getAllActive()
            .map(TaskEntity::toModel)
            .groupBy { it.level }

    // ===== CRUD =====

    suspend fun addCustomTask(task: Task) {
        dao.insertAll(listOf(task.toEntity(isCustom = true)))
    }

    suspend fun updateTask(task: Task) {
        val existing = dao.getById(task.id) ?: return
        dao.update(
            existing.copy(
                description = task.description,
                level = task.level,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(id: String) {
        dao.softDelete(id)
    }

    // ===== Seeding / Reset =====

    suspend fun seedIfEmpty(defaultTasks: List<Task>) {
        if (dao.countDefaults() == 0) {
            dao.insertAll(defaultTasks.map { it.toEntity(isCustom = false) })
        }
    }

    suspend fun resetToDefaults(defaultTasks: List<Task>) {
        dao.deleteAllDefaults()
        dao.insertAll(defaultTasks.map { it.toEntity(isCustom = false) })
        dao.purgeDeletedCustomTasks()
    }
}

// ===== Mapping extensions =====

private fun TaskEntity.toModel() = Task(
    id = id,
    level = level,
    description = description,
    isCustom = isCustom
)

private fun Task.toEntity(isCustom: Boolean) = TaskEntity(
    id = id,
    level = level,
    description = description,
    isCustom = isCustom
)
