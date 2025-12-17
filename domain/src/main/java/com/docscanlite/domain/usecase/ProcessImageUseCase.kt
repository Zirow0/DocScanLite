package com.docscanlite.domain.usecase

import android.graphics.Bitmap
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.OcrResult
import com.docscanlite.domain.repository.OcrRepository
import javax.inject.Inject

/**
 * Use case for processing an image with OCR to extract text
 */
class ProcessImageUseCase @Inject constructor(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<OcrResult> {
        return ocrRepository.processImage(bitmap)
    }
}
