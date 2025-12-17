package com.docscanlite.imageprocessing.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Applies perspective transformation to crop and straighten document
 * Uses Android native Matrix API for perspective correction
 */
@Singleton
class PerspectiveTransform @Inject constructor() {

    enum class DimensionMode {
        /**
         * Use average of opposing sides - maintains correct aspect ratio
         * Best for general use and accurate document proportions
         */
        AVERAGE,

        /**
         * Use maximum of opposing sides - preserves maximum quality
         * May slightly distort aspect ratio but keeps all detail
         */
        MAXIMUM
    }

    /**
     * Apply perspective transform to crop and straighten document
     * @param bitmap Original image
     * @param corners Four corners in order: top-left, top-right, bottom-right, bottom-left
     * @param dimensionMode How to calculate output dimensions (default: AVERAGE for correct proportions)
     * @return Cropped and straightened document bitmap with perspective correction
     */
    fun transform(
        bitmap: Bitmap,
        corners: List<PointF>,
        dimensionMode: DimensionMode = DimensionMode.AVERAGE
    ): Bitmap? {
        if (corners.size != 4) return null

        try {
            // Extract corners (assuming order: top-left, top-right, bottom-right, bottom-left)
            val topLeft = corners[0]
            val topRight = corners[1]
            val bottomRight = corners[2]
            val bottomLeft = corners[3]

            // Calculate target dimensions for the output rectangle
            val widthTop = distance(topLeft, topRight)
            val widthBottom = distance(bottomLeft, bottomRight)
            val heightLeft = distance(topLeft, bottomLeft)
            val heightRight = distance(topRight, bottomRight)

            // Calculate dimensions based on selected mode
            val targetWidth = when (dimensionMode) {
                DimensionMode.AVERAGE -> ((widthTop + widthBottom) / 2f).toInt()
                DimensionMode.MAXIMUM -> max(widthTop, widthBottom).toInt()
            }.coerceAtLeast(1)

            val targetHeight = when (dimensionMode) {
                DimensionMode.AVERAGE -> ((heightLeft + heightRight) / 2f).toInt()
                DimensionMode.MAXIMUM -> max(heightLeft, heightRight).toInt()
            }.coerceAtLeast(1)

            // Source points (the quadrilateral corners from user selection)
            val srcPoints = floatArrayOf(
                topLeft.x, topLeft.y,           // top-left
                topRight.x, topRight.y,         // top-right
                bottomRight.x, bottomRight.y,   // bottom-right
                bottomLeft.x, bottomLeft.y      // bottom-left
            )

            // Destination points (perfect rectangle)
            val dstPoints = floatArrayOf(
                0f, 0f,                         // top-left
                targetWidth.toFloat(), 0f,      // top-right
                targetWidth.toFloat(), targetHeight.toFloat(),  // bottom-right
                0f, targetHeight.toFloat()      // bottom-left
            )

            // Create transformation matrix using perspective transformation
            val matrix = Matrix()
            matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

            // Create output bitmap with corrected perspective
            val result = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )

            // Apply transformation
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, matrix, paint)

            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Calculate Euclidean distance between two points
     */
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
