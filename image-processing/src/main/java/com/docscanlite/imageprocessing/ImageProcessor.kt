package com.docscanlite.imageprocessing

import android.graphics.Bitmap
import com.docscanlite.imageprocessing.filter.ImageFilter
import com.docscanlite.imageprocessing.transform.ImageTransform

/**
 * Main entry point for image processing operations
 * Combines filters and transformations
 */
object ImageProcessor {

    /**
     * Processing options for document images
     */
    data class ProcessingOptions(
        val filter: FilterType = FilterType.NONE,
        val brightness: Float = 0f,        // -255 to 255
        val contrast: Float = 0f,          // -1 to 1
        val saturation: Float = 1f,        // 0 to 2
        val rotation: Float = 0f,          // degrees
        val autoEnhance: Boolean = false,
        val sharpen: Float = 0f            // 0 to 1
    )

    /**
     * Available filter types
     */
    enum class FilterType {
        NONE,
        GRAYSCALE,
        BLACK_AND_WHITE,
        SEPIA,
        AUTO_ENHANCE
    }

    /**
     * Process bitmap with specified options
     */
    fun processBitmap(bitmap: Bitmap, options: ProcessingOptions): Bitmap {
        var result = bitmap

        // Apply rotation first
        if (options.rotation != 0f) {
            val rotated = ImageFilter.rotateBitmap(result, options.rotation)
            if (result != bitmap) result.recycle()
            result = rotated
        }

        // Apply filter
        result = when (options.filter) {
            FilterType.GRAYSCALE -> {
                val filtered = ImageFilter.applyGrayscale(result)
                if (result != bitmap) result.recycle()
                filtered
            }
            FilterType.BLACK_AND_WHITE -> {
                val filtered = ImageFilter.applyBlackAndWhite(result)
                if (result != bitmap) result.recycle()
                filtered
            }
            FilterType.SEPIA -> {
                val filtered = ImageFilter.applySepia(result)
                if (result != bitmap) result.recycle()
                filtered
            }
            FilterType.AUTO_ENHANCE -> {
                val filtered = ImageFilter.autoEnhance(result)
                if (result != bitmap) result.recycle()
                filtered
            }
            FilterType.NONE -> result
        }

        // Apply brightness
        if (options.brightness != 0f) {
            val adjusted = ImageFilter.adjustBrightness(result, options.brightness)
            if (result != bitmap) result.recycle()
            result = adjusted
        }

        // Apply contrast
        if (options.contrast != 0f) {
            val adjusted = ImageFilter.adjustContrast(result, options.contrast)
            if (result != bitmap) result.recycle()
            result = adjusted
        }

        // Apply saturation
        if (options.saturation != 1f && options.filter != FilterType.GRAYSCALE &&
            options.filter != FilterType.BLACK_AND_WHITE) {
            val adjusted = ImageFilter.adjustSaturation(result, options.saturation)
            if (result != bitmap) result.recycle()
            result = adjusted
        }

        // Apply sharpen
        if (options.sharpen > 0f) {
            val sharpened = ImageFilter.applySharpen(result, options.sharpen)
            if (result != bitmap) result.recycle()
            result = sharpened
        }

        // Apply auto enhance if requested
        if (options.autoEnhance && options.filter == FilterType.NONE) {
            val enhanced = ImageFilter.autoEnhance(result)
            if (result != bitmap) result.recycle()
            result = enhanced
        }

        return result
    }

    /**
     * Quick presets for common use cases
     */
    object Presets {
        val DOCUMENT_SCAN = ProcessingOptions(
            filter = FilterType.AUTO_ENHANCE,
            sharpen = 0.3f
        )

        val BLACK_AND_WHITE_DOCUMENT = ProcessingOptions(
            filter = FilterType.BLACK_AND_WHITE,
            contrast = 0.2f
        )

        val COLOR_DOCUMENT = ProcessingOptions(
            filter = FilterType.NONE,
            brightness = 10f,
            contrast = 0.1f,
            saturation = 1.1f
        )

        val PHOTO = ProcessingOptions(
            autoEnhance = true
        )

        val VINTAGE = ProcessingOptions(
            filter = FilterType.SEPIA,
            brightness = -20f
        )
    }

    /**
     * Crop and process in one operation
     */
    fun cropAndProcess(
        bitmap: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        options: ProcessingOptions = Presets.DOCUMENT_SCAN
    ): Bitmap {
        val cropped = ImageTransform.cropBitmapNormalized(bitmap, left, top, right, bottom)
        val processed = processBitmap(cropped, options)

        if (processed != cropped) {
            cropped.recycle()
        }

        return processed
    }

    /**
     * Auto-crop and process document
     */
    fun autoProcessDocument(bitmap: Bitmap): Bitmap {
        val corners = ImageTransform.detectDocumentCorners(bitmap)

        return if (corners != null && corners.size == 8) {
            val cropped = ImageTransform.applyPerspectiveTransform(
                bitmap,
                corners,
                bitmap.width,
                bitmap.height
            )
            val processed = processBitmap(cropped, Presets.DOCUMENT_SCAN)

            if (processed != cropped) {
                cropped.recycle()
            }

            processed
        } else {
            processBitmap(bitmap, Presets.DOCUMENT_SCAN)
        }
    }
}
