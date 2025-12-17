package com.docscanlite.domain.repository

import android.graphics.Bitmap
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.OcrResult

/**
 * Repository interface for OCR operations
 * Domain layer contract to be implemented by data layer
 */
interface OcrRepository {

    /**
     * Process image and extract text using OCR
     * @param bitmap The image to process
     * @return Result containing OCR result or error
     */
    suspend fun processImage(bitmap: Bitmap): Result<OcrResult>

    /**
     * Check if OCR service is ready to process images
     * @return true if ready, false otherwise
     */
    fun isReady(): Boolean
}
