package com.docscanlite.data.mapper

import android.graphics.Rect
import com.docscanlite.ocr.model.OcrResult as OcrModuleResult
import com.docscanlite.ocr.model.TextBlock as OcrModuleTextBlock
import com.docscanlite.ocr.model.TextLine as OcrModuleTextLine
import com.docscanlite.ocr.model.TextElement as OcrModuleTextElement
import com.docscanlite.domain.model.OcrResult as DomainOcrResult
import com.docscanlite.domain.model.TextBlock as DomainTextBlock
import com.docscanlite.domain.model.TextLine as DomainTextLine
import com.docscanlite.domain.model.TextElement as DomainTextElement
import com.docscanlite.domain.model.BoundingBox

/**
 * Mapper functions to convert OCR module models to domain models
 */

fun OcrModuleResult.toDomain(): DomainOcrResult {
    return DomainOcrResult(
        text = text,
        blocks = blocks.map { it.toDomain() },
        confidence = confidence,
        processingTimeMs = processingTimeMs
    )
}

fun OcrModuleTextBlock.toDomain(): DomainTextBlock {
    return DomainTextBlock(
        text = text,
        boundingBox = boundingBox?.toBoundingBox(),
        lines = lines.map { it.toDomain() },
        confidence = confidence
    )
}

fun OcrModuleTextLine.toDomain(): DomainTextLine {
    return DomainTextLine(
        text = text,
        boundingBox = boundingBox?.toBoundingBox(),
        elements = elements.map { it.toDomain() },
        confidence = confidence
    )
}

fun OcrModuleTextElement.toDomain(): DomainTextElement {
    return DomainTextElement(
        text = text,
        boundingBox = boundingBox?.toBoundingBox(),
        confidence = confidence
    )
}

/**
 * Convert Android Rect to domain BoundingBox
 */
private fun Rect.toBoundingBox(): BoundingBox {
    return BoundingBox(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}
