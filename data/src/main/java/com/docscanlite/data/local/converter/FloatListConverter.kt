package com.docscanlite.data.local.converter

import androidx.room.TypeConverter

/**
 * Room type converter for List<Float> (used for bounds storage)
 */
class FloatListConverter {

    @TypeConverter
    fun fromString(value: String?): List<Float>? {
        if (value.isNullOrEmpty()) return null
        return value.split(",").mapNotNull { it.toFloatOrNull() }
    }

    @TypeConverter
    fun toString(list: List<Float>?): String? {
        return list?.joinToString(",")
    }
}
