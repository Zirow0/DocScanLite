package com.docscanlite.ocr

import android.graphics.Bitmap
import com.docscanlite.ocr.model.OcrResult

/**
 * Interface for text recognition
 */
interface TextRecognizer {
    /**
     * Recognize text from bitmap image
     * @param bitmap Image to recognize text from
     * @return OCR result with recognized text
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult>

    /**
     * Check if recognizer is ready
     */
    fun isReady(): Boolean

    /**
     * Close and cleanup resources
     */
    fun close()
}
