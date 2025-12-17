package com.docscanlite.ocr

import android.content.Context
import android.graphics.Bitmap
import com.docscanlite.ocr.mlkit.MLKitTextRecognizer
import com.docscanlite.ocr.model.OcrResult
import com.docscanlite.ocr.tesseract.TesseractTextRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main OCR processor that manages text recognition
 * Підтримує кілька OCR движків:
 * - Tesseract (рекомендовано) - підтримка української та багатьох інших мов
 * - ML Kit (опціонально) - швидкий, але обмежена підтримка мов
 */
@Singleton
class OcrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var textRecognizer: TextRecognizer? = null
    private var isInitializing = false

    /**
     * Ініціалізація OCR з Tesseract
     * Автоматично викликається при першому використанні
     *
     * @param languages Мови для розпізнавання (наприклад, "ukr+eng")
     */
    suspend fun initialize(languages: String = "ukr+eng"): Result<Unit> {
        if (textRecognizer?.isReady() == true) {
            return Result.success(Unit)
        }

        if (isInitializing) {
            return Result.failure(IllegalStateException("Already initializing"))
        }

        isInitializing = true
        return try {
            val tesseractRecognizer = TesseractTextRecognizer(context, languages)
            val result = tesseractRecognizer.initialize()

            if (result.isSuccess) {
                textRecognizer = tesseractRecognizer
                Result.success(Unit)
            } else {
                // Fallback to ML Kit якщо Tesseract не вдалося ініціалізувати
                initializeMLKit()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Fallback to ML Kit
            initializeMLKit()
            Result.failure(e)
        } finally {
            isInitializing = false
        }
    }

    /**
     * Initialize ML Kit text recognizer (fallback)
     * Обмежена підтримка мов - тільки латиниця
     */
    private fun initializeMLKit() {
        textRecognizer = MLKitTextRecognizer()
    }

    /**
     * Process image and extract text
     * Автоматично ініціалізує OCR якщо потрібно
     *
     * @param bitmap Image to process
     * @return Result with recognized text or error
     */
    suspend fun processImage(bitmap: Bitmap): Result<OcrResult> {
        // Автоматична ініціалізація якщо потрібно
        if (textRecognizer == null || !textRecognizer!!.isReady()) {
            val initResult = initialize()
            if (initResult.isFailure) {
                return Result.failure(
                    initResult.exceptionOrNull() ?: Exception("Failed to initialize OCR")
                )
            }
        }

        val recognizer = textRecognizer
            ?: return Result.failure(IllegalStateException("Text recognizer not initialized"))

        return recognizer.recognizeText(bitmap)
    }

    /**
     * Check if OCR is ready to process
     */
    fun isReady(): Boolean {
        return textRecognizer?.isReady() ?: false
    }

    /**
     * Get current OCR engine name
     */
    fun getEngineName(): String {
        return when (textRecognizer) {
            is TesseractTextRecognizer -> "Tesseract OCR"
            is MLKitTextRecognizer -> "ML Kit (Latin only)"
            else -> "Not initialized"
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        textRecognizer?.close()
        textRecognizer = null
    }

    /**
     * Перемкнути на інший движок OCR
     */
    suspend fun switchToTesseract(languages: String = "ukr+eng"): Result<Unit> {
        cleanup()
        return initialize(languages)
    }

    /**
     * Перемкнути на ML Kit (fallback)
     */
    fun switchToMLKit() {
        cleanup()
        initializeMLKit()
    }
}
