package com.docscanlite.imageprocessing.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Image filter operations
 */
object ImageFilter {

    /**
     * Apply grayscale filter
     */
    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Apply black and white filter with threshold
     */
    fun applyBlackAndWhite(bitmap: Bitmap, threshold: Int = 128): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            // Calculate grayscale value
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // Apply threshold
            val bw = if (gray > threshold) 255 else 0

            pixels[i] = (0xFF shl 24) or (bw shl 16) or (bw shl 8) or bw
        }

        output.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return output
    }

    /**
     * Apply sepia tone filter
     */
    fun applySepia(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.set(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Adjust brightness
     */
    fun adjustBrightness(bitmap: Bitmap, value: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, value,
                0f, 1f, 0f, 0f, value,
                0f, 0f, 1f, 0f, value,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Adjust contrast
     */
    fun adjustContrast(bitmap: Bitmap, value: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val scale = value + 1f
        val translate = (-.5f * scale + .5f) * 255f

        val colorMatrix = ColorMatrix()
        colorMatrix.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Adjust saturation
     */
    fun adjustSaturation(bitmap: Bitmap, value: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(value)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Auto enhance (automatic brightness and contrast)
     */
    fun autoEnhance(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var minBrightness = 255
        var maxBrightness = 0

        // Calculate min and max brightness
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val brightness = (r + g + b) / 3

            if (brightness < minBrightness) minBrightness = brightness
            if (brightness > maxBrightness) maxBrightness = brightness
        }

        // Calculate contrast adjustment
        val range = maxBrightness - minBrightness
        if (range == 0) return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

        val scale = 255f / range
        val offset = -minBrightness * scale

        // Apply enhancement
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xff
            var r = ((pixel shr 16) and 0xff) * scale + offset
            var g = ((pixel shr 8) and 0xff) * scale + offset
            var b = (pixel and 0xff) * scale + offset

            r = r.coerceIn(0f, 255f)
            g = g.coerceIn(0f, 255f)
            b = b.coerceIn(0f, 255f)

            pixels[i] = (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return output
    }

    /**
     * Sharpen filter
     */
    fun applySharpen(bitmap: Bitmap, amount: Float = 1f): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val sharpenValue = -amount
        val centerValue = 1 + 4 * amount

        // Simple sharpen using ColorMatrix (approximation)
        val colorMatrix = ColorMatrix()
        colorMatrix.set(
            floatArrayOf(
                centerValue, sharpenValue, sharpenValue, 0f, 0f,
                sharpenValue, centerValue, sharpenValue, 0f, 0f,
                sharpenValue, sharpenValue, centerValue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }

    /**
     * Rotate bitmap
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Flip bitmap horizontally
     */
    fun flipHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.preScale(-1f, 1f)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Flip bitmap vertically
     */
    fun flipVertical(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.preScale(1f, -1f)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}
