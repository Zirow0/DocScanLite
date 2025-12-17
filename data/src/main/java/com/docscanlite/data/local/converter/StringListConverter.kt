package com.docscanlite.data.local.converter

import androidx.room.TypeConverter

/**
 * Room type converter for List<String>
 */
class StringListConverter {

    @TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",")
        }
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(",")
    }
}
