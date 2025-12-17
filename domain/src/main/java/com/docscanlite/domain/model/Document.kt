package com.docscanlite.domain.model

import java.util.UUID

/**
 * Document domain model
 * Represents a scanned document in the system
 */
data class Document(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val originalPath: String,
    val processedPath: String? = null,
    val thumbnailPath: String,
    val ocrText: String? = null,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val tags: List<String> = emptyList(),
    /** Document bounds as list of 4 points [x1,y1,x2,y2,x3,y3,x4,y4] */
    val bounds: List<Float>? = null,
    /** Selected filter name (NONE, AUTO_ENHANCE, BLACK_AND_WHITE, GRAYSCALE, SEPIA, DOCUMENT) */
    val filterName: String? = null,
    /** Brightness adjustment (-100 to 100) */
    val brightness: Float = 0f,
    /** Contrast adjustment (-1 to 1) */
    val contrast: Float = 0f,
    /** Saturation adjustment (0 to 2, default 1) */
    val saturation: Float = 1f,
    /** Crop bounds as [left, top, right, bottom] normalized 0-1 */
    val cropBounds: List<Float>? = null,
    /** Rotation angle in degrees (0, 90, 180, 270) */
    val rotationAngle: Float = 0f
)
