package com.couplechess.data.db

import androidx.room.TypeConverter
import com.couplechess.data.model.TaskLevel

class Converters {
    @TypeConverter
    fun fromTaskLevel(level: TaskLevel): String = level.name

    @TypeConverter
    fun toTaskLevel(name: String): TaskLevel = TaskLevel.valueOf(name)
}
