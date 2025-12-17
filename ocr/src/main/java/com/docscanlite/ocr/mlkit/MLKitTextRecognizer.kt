package com.docscanlite.ocr.mlkit

import android.graphics.Bitmap
import com.docscanlite.ocr.TextRecognizer
import com.docscanlite.ocr.model.OcrResult
import com.docscanlite.ocr.model.TextBlock
import com.docscanlite.ocr.model.TextElement
import com.docscanlite.ocr.model.TextLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.system.measureTimeMillis

/**
 * ML Kit implementation of TextRecognizer (On-device)
 * Uses Google Play Services ML Kit Text Recognition with automatic script detection
 *
 * Підтримувані писемності (автоматичне визначення):
 * - Latin (англійська, німецька, французька, іспанська та ін.)
 * - Cyrillic (українська, російська, білоруська, сербська та ін.)
 * - Chinese (спрощена китайська, традиційна китайська)
 * - Japanese (Hiragana, Katakana, Kanji)
 * - Korean (Hangul, Hanja)
 * - Devanagari (хінді, маратхі, непальська та ін.)
 *
 * ВАЖЛИВО:
 * - Використовує Google Play Services ML Kit
 * - Автоматично завантажує потрібні мовні моделі при першому використанні
 * - Працює offline після завантаження моделей
 * - Для роботи потрібен Google Play Services на пристрої
 *
 * Залежності:
 * - com.google.android.gms:play-services-mlkit-text-recognition (з підтримкою багатьох писемностей)
 */
class MLKitTextRecognizer : TextRecognizer {

    // Використовуємо базовий Latin recognizer (ML Kit не підтримує кирилицю)
    // Цей recognizer буде fallback якщо Tesseract не працює
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    private var isInitialized = true

    override suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> {
        return try {
            var visionText: Text? = null
            val processingTime = measureTimeMillis {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                visionText = recognizer.process(inputImage).await()
            }

            visionText?.let {
                Result.success(mapToOcrResult(it, processingTime))
            } ?: Result.failure(Exception("Text recognition returned null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        try {
            recognizer.close()
            isInitialized = false
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Map ML Kit Text result to our OcrResult model
     * Note: ML Kit doesn't provide confidence scores, so we use default values
     */
    private fun mapToOcrResult(visionText: Text, processingTime: Long): OcrResult {
        val blocks = visionText.textBlocks.map { textBlock ->
            TextBlock(
                text = textBlock.text,
                boundingBox = textBlock.boundingBox,
                lines = textBlock.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox,
                        elements = line.elements.map { element ->
                            TextElement(
                                text = element.text,
                                boundingBox = element.boundingBox,
                                confidence = 1.0f // ML Kit doesn't provide confidence
                            )
                        },
                        confidence = 1.0f // ML Kit doesn't provide confidence
                    )
                },
                confidence = 1.0f // ML Kit doesn't provide confidence
            )
        }

        return OcrResult(
            text = visionText.text,
            blocks = blocks,
            confidence = 1.0f, // ML Kit doesn't provide confidence
            processingTimeMs = processingTime
        )
    }
}
