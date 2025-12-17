package com.docscanlite.domain.model

/**
 * OCR result domain model
 * Represents the result of text recognition from an image
 */
data class OcrResult(
    val text: String,
    val blocks: List<TextBlock> = emptyList(),
    val confidence: Float = 0f,
    val processingTimeMs: Long = 0L
)

/**
 * Text block with position and text
 */
data class TextBlock(
    val text: String,
    val boundingBox: BoundingBox?,
    val lines: List<TextLine> = emptyList(),
    val confidence: Float = 0f
)

/**
 * Text line within a block
 */
data class TextLine(
    val text: String,
    val boundingBox: BoundingBox?,
    val elements: List<TextElement> = emptyList(),
    val confidence: Float = 0f
)

/**
 * Individual text element (word/character)
 */
data class TextElement(
    val text: String,
    val boundingBox: BoundingBox?,
    val confidence: Float = 0f
)

/**
 * Platform-independent bounding box representation
 */
data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}
