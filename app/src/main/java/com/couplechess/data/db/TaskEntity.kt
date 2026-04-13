package com.couplechess.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.couplechess.data.model.TaskLevel

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val level: TaskLevel,
    val description: String,
    val isCustom: Boolean = false,
    val isDeleted: Boolean = false,       // soft-delete to support reset-to-default
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
