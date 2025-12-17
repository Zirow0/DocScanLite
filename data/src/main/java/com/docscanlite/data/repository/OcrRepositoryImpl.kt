package com.docscanlite.data.repository

import android.graphics.Bitmap
import com.docscanlite.data.mapper.toDomain
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.repository.OcrRepository
import com.docscanlite.ocr.OcrProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OcrRepository
 * Handles OCR operations using ML Kit through OcrProcessor
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val ocrProcessor: OcrProcessor
) : OcrRepository {

    override suspend fun processImage(bitmap: Bitmap): Result<com.docscanlite.domain.model.OcrResult> {
        val result = ocrProcessor.processImage(bitmap)

        return result.fold(
            onSuccess = { ocrResult ->
                Result.Success(ocrResult.toDomain())
            },
            onFailure = { exception ->
                Result.Error(exception)
            }
        )
    }

    override fun isReady(): Boolean {
        return ocrProcessor.isReady()
    }
}
