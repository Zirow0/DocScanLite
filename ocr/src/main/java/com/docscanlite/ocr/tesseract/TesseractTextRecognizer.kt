package com.docscanlite.ocr.tesseract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.docscanlite.ocr.TextRecognizer
import com.docscanlite.ocr.model.OcrResult
import com.docscanlite.ocr.model.TextBlock
import com.docscanlite.ocr.model.TextElement
import com.docscanlite.ocr.model.TextLine
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

/**
 * Tesseract OCR implementation of TextRecognizer
 * Підтримує українську мову та багато інших мов
 *
 * Переваги:
 * - Повна підтримка української (кирилиці)
 * - Високу точність розпізнавання
 * - Працює повністю offline
 * - Підтримка 100+ мов
 *
 * Вимоги:
 * - Потрібні мовні файли (.traineddata) у папці tessdata
 * - Мовні файли можна завантажити з https://github.com/tesseract-ocr/tessdata
 *
 * @param context Android Context для доступу до assets
 * @param languages Список мов для розпізнавання (наприклад, "ukr+eng" для української та англійської)
 */
class TesseractTextRecognizer(
    private val context: Context,
    private val languages: String = "ukr+eng" // Українська + Англійська за замовчуванням
) : TextRecognizer {

    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false
    private val tessDataPath: String by lazy {
        context.filesDir.absolutePath
    }

    /**
     * Ініціалізація Tesseract OCR
     * Копіює мовні файли з assets до filesDir якщо потрібно
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                return@withContext Result.success(Unit)
            }

            // Створюємо папку tessdata якщо не існує
            val tessDataDir = File(tessDataPath, "tessdata")
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }

            // Копіюємо мовні файли з assets
            copyLanguageFiles(languages.split("+"))

            // Ініціалізуємо Tesseract
            tessBaseAPI = TessBaseAPI().apply {
                if (!init(tessDataPath, languages)) {
                    throw Exception("Failed to initialize Tesseract with languages: $languages")
                }
                // Налаштування для кращої якості
                setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            }

            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Копіює мовні файли з assets до tessdata
     */
    private fun copyLanguageFiles(languages: List<String>) {
        val tessDataDir = File(tessDataPath, "tessdata")

        languages.forEach { lang ->
            val langFile = File(tessDataDir, "$lang.traineddata")

            // Копіюємо тільки якщо файл не існує
            if (!langFile.exists()) {
                try {
                    context.assets.open("tessdata/$lang.traineddata").use { input ->
                        FileOutputStream(langFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    throw Exception("Failed to copy language file: $lang.traineddata. " +
                            "Make sure the file exists in assets/tessdata/", e)
                }
            }
        }
    }

    override suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext Result.failure(
                    IllegalStateException("Tesseract not initialized. Call initialize() first.")
                )
            }

            val api = tessBaseAPI ?: return@withContext Result.failure(
                IllegalStateException("Tesseract API is null")
            )

            var processingTime = 0L
            var text = ""
            val blocks = mutableListOf<TextBlock>()

            measureTimeMillis {
                // Встановлюємо зображення
                api.setImage(bitmap)

                // Отримуємо текст
                text = api.utF8Text ?: ""

                // Отримуємо детальну інформацію про розпізнані блоки
                val iterator = api.resultIterator
                if (iterator != null) {
                    val blocksList = mutableListOf<MutableList<TextLine>>()
                    var currentBlock = mutableListOf<TextLine>()

                    do {
                        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                        val lineText = iterator.getUTF8Text(level) ?: continue

                        // Конвертуємо IntArray в Rect
                        val boundingBoxArray = iterator.getBoundingBox(level)
                        val boundingBox = if (boundingBoxArray != null && boundingBoxArray.size == 4) {
                            Rect(
                                boundingBoxArray[0], // left
                                boundingBoxArray[1], // top
                                boundingBoxArray[2], // right
                                boundingBoxArray[3]  // bottom
                            )
                        } else null

                        val confidence = iterator.confidence(level) / 100f

                        // Отримуємо елементи (слова) в рядку
                        val elements = mutableListOf<TextElement>()
                        val wordLevel = TessBaseAPI.PageIteratorLevel.RIL_WORD

                        // Створюємо TextLine
                        val textLine = TextLine(
                            text = lineText.trim(),
                            boundingBox = boundingBox,
                            elements = elements,
                            confidence = confidence
                        )
                        currentBlock.add(textLine)

                        // Якщо це останній рядок в блоці, зберігаємо блок
                        if (iterator.isAtFinalElement(TessBaseAPI.PageIteratorLevel.RIL_BLOCK, level)) {
                            blocksList.add(currentBlock)
                            currentBlock = mutableListOf()
                        }

                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))

                    // Додаємо останній блок якщо він не порожній
                    if (currentBlock.isNotEmpty()) {
                        blocksList.add(currentBlock)
                    }

                    // Конвертуємо в TextBlock
                    blocksList.forEach { linesList ->
                        if (linesList.isNotEmpty()) {
                            val blockText = linesList.joinToString("\n") { it.text }
                            val blockConfidence = linesList.map { it.confidence }.average().toFloat()

                            blocks.add(
                                TextBlock(
                                    text = blockText,
                                    boundingBox = null, // Можна обчислити з boundingBox ліній
                                    lines = linesList,
                                    confidence = blockConfidence
                                )
                            )
                        }
                    }

                    iterator.delete()
                }
            }.also { processingTime = it }

            val result = OcrResult(
                text = text,
                blocks = blocks,
                confidence = api.meanConfidence() / 100f,
                processingTimeMs = processingTime
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        try {
            tessBaseAPI?.recycle()
            tessBaseAPI = null
            isInitialized = false
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Змінити мови розпізнавання
     * Потребує реініціалізації
     */
    suspend fun setLanguages(newLanguages: String): Result<Unit> {
        close()
        return TesseractTextRecognizer(context, newLanguages).initialize()
    }
}
