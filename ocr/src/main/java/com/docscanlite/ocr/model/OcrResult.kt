package com.docscanlite.ocr.model

import android.graphics.Rect

/**
 * Result of OCR text recognition
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
    val boundingBox: Rect?,
    val lines: List<TextLine> = emptyList(),
    val confidence: Float = 0f
)

/**
 * Text line within a block
 */
data class TextLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<TextElement> = emptyList(),
    val confidence: Float = 0f
)

/**
 * Individual text element (word/character)
 */
data class TextElement(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float = 0f
)
