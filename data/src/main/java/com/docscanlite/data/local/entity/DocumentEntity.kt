package com.docscanlite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.docscanlite.data.local.converter.FloatListConverter
import com.docscanlite.data.local.converter.StringListConverter

/**
 * Room database entity for Document
 */
@Entity(tableName = "documents")
@TypeConverters(StringListConverter::class, FloatListConverter::class)
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val originalPath: String,
    val processedPath: String?,
    val thumbnailPath: String,
    val ocrText: String?,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val tags: List<String>,
    /** Document bounds as list of 8 floats [x1,y1,x2,y2,x3,y3,x4,y4] */
    val bounds: List<Float>? = null,
    /** Selected filter name */
    val filterName: String? = null,
    /** Brightness adjustment (-100 to 100) */
    val brightness: Float = 0f,
    /** Contrast adjustment (-1 to 1) */
    val contrast: Float = 0f,
    /** Saturation adjustment (0 to 2, default 1) */
    val saturation: Float = 1f,
    /** Crop bounds as [left, top, right, bottom] normalized 0-1 */
    val cropBounds: List<Float>? = null,
    /** Rotation angle in degrees */
    val rotationAngle: Float = 0f
)
