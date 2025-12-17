package com.docscanlite.imageprocessing.transform

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Image transformation operations
 */
object ImageTransform {

    /**
     * Crop bitmap to specified rectangle
     */
    fun cropBitmap(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val safeLeft = left.coerceIn(0, bitmap.width)
        val safeTop = top.coerceIn(0, bitmap.height)
        val safeWidth = width.coerceIn(1, bitmap.width - safeLeft)
        val safeHeight = height.coerceIn(1, bitmap.height - safeTop)

        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
    }

    /**
     * Crop bitmap with normalized coordinates (0-1)
     */
    fun cropBitmapNormalized(
        bitmap: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Bitmap {
        val x = (left * bitmap.width).toInt()
        val y = (top * bitmap.height).toInt()
        val width = ((right - left) * bitmap.width).toInt()
        val height = ((bottom - top) * bitmap.height).toInt()

        return cropBitmap(bitmap, x, y, width, height)
    }

    /**
     * Resize bitmap to specified dimensions
     */
    fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Resize bitmap maintaining aspect ratio
     */
    fun resizeBitmapKeepAspect(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        if (ratio >= 1f) return bitmap

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return resizeBitmap(bitmap, newWidth, newHeight)
    }

    /**
     * Apply perspective transformation
     * @param srcPoints Four corner points of the source rectangle
     * @param dstWidth Width of the output bitmap
     * @param dstHeight Height of the output bitmap
     */
    fun applyPerspectiveTransform(
        bitmap: Bitmap,
        srcPoints: FloatArray,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        val dstPoints = floatArrayOf(
            0f, 0f,                          // Top-left
            dstWidth.toFloat(), 0f,          // Top-right
            dstWidth.toFloat(), dstHeight.toFloat(), // Bottom-right
            0f, dstHeight.toFloat()          // Bottom-left
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

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
     * Auto-crop with corner detection (placeholder for ML Kit integration)
     */
    fun autoCrop(bitmap: Bitmap): Bitmap {
        // TODO: Implement with ML Kit or custom edge detection
        // For now, apply simple margin-based crop
        val margin = 0.05f // 5% margin

        return cropBitmapNormalized(
            bitmap,
            margin,
            margin,
            1f - margin,
            1f - margin
        )
    }

    /**
     * Calculate crop rectangle that fits within image bounds
     */
    fun calculateCropRect(
        imageWidth: Int,
        imageHeight: Int,
        cropRect: RectF
    ): RectF {
        val left = max(0f, cropRect.left).coerceAtMost(imageWidth.toFloat())
        val top = max(0f, cropRect.top).coerceAtMost(imageHeight.toFloat())
        val right = max(left, cropRect.right).coerceAtMost(imageWidth.toFloat())
        val bottom = max(top, cropRect.bottom).coerceAtMost(imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }

    /**
     * Apply corner-based perspective transformation
     * Common for document scanning
     */
    fun applyDocumentPerspective(
        bitmap: Bitmap,
        topLeft: Pair<Float, Float>,
        topRight: Pair<Float, Float>,
        bottomRight: Pair<Float, Float>,
        bottomLeft: Pair<Float, Float>
    ): Bitmap {
        // Calculate output dimensions based on the largest side
        val width1 = distance(topLeft, topRight)
        val width2 = distance(bottomLeft, bottomRight)
        val height1 = distance(topLeft, bottomLeft)
        val height2 = distance(topRight, bottomRight)

        val maxWidth = max(width1, width2).toInt()
        val maxHeight = max(height1, height2).toInt()

        val srcPoints = floatArrayOf(
            topLeft.first, topLeft.second,
            topRight.first, topRight.second,
            bottomRight.first, bottomRight.second,
            bottomLeft.first, bottomLeft.second
        )

        return applyPerspectiveTransform(bitmap, srcPoints, maxWidth, maxHeight)
    }

    /**
     * Calculate distance between two points
     */
    private fun distance(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Float {
        val dx = p2.first - p1.first
        val dy = p2.second - p1.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Detect document corners (simplified version)
     * TODO: Replace with ML Kit or OpenCV
     */
    fun detectDocumentCorners(bitmap: Bitmap): FloatArray? {
        // Placeholder: Return corners with 5% margin
        val margin = 0.05f
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        return floatArrayOf(
            width * margin, height * margin,           // Top-left
            width * (1 - margin), height * margin,     // Top-right
            width * (1 - margin), height * (1 - margin), // Bottom-right
            width * margin, height * (1 - margin)      // Bottom-left
        )
    }

    /**
     * Pad bitmap with specified color
     */
    fun padBitmap(
        bitmap: Bitmap,
        paddingLeft: Int,
        paddingTop: Int,
        paddingRight: Int,
        paddingBottom: Int,
        color: Int = android.graphics.Color.WHITE
    ): Bitmap {
        val newWidth = bitmap.width + paddingLeft + paddingRight
        val newHeight = bitmap.height + paddingTop + paddingBottom

        val output = Bitmap.createBitmap(newWidth, newHeight, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(color)
        canvas.drawBitmap(bitmap, paddingLeft.toFloat(), paddingTop.toFloat(), null)

        return output
    }
}
