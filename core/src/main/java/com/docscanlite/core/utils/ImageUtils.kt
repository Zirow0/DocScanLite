package com.docscanlite.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Utility functions for image processing
 */
object ImageUtils {

    private const val THUMBNAIL_MAX_SIZE = 512
    private const val THUMBNAIL_QUALITY = 85

    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }

    /**
     * Get image dimensions with EXIF orientation applied
     */
    fun getOrientedImageDimensions(file: File): Pair<Int, Int> {
        val (width, height) = getImageDimensions(file)
        val orientation = getImageOrientation(file)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                // Swap width and height for rotated images
                Pair(height, width)
            }
            else -> Pair(width, height)
        }
    }

    /**
     * Get image orientation from EXIF data
     */
    fun getImageOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Create thumbnail from image file
     */
    fun createThumbnail(
        sourceFile: File,
        thumbnailFile: File,
        maxSize: Int = THUMBNAIL_MAX_SIZE
    ): Boolean {
        return try {
            // Load image with proper orientation
            val bitmap = loadBitmapWithOrientation(sourceFile, maxSize)

            // Save thumbnail
            FileOutputStream(thumbnailFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }

            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load bitmap with proper orientation and scale
     * Scales image so that the smaller side equals maxSize (if smaller side > maxSize)
     */
    fun loadBitmapWithOrientation(
        file: File,
        maxSize: Int
    ): Bitmap {
        // Calculate sample size
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        // Calculate scale based on SMALLER side
        val smallerSide = min(options.outWidth, options.outHeight)
        val scale = (smallerSide / maxSize).coerceAtLeast(1)

        // Load scaled bitmap
        options.apply {
            inJustDecodeBounds = false
            inSampleSize = scale
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

        // Apply orientation
        return rotateBitmapIfNeeded(bitmap, file)
    }

    /**
     * Rotate bitmap according to EXIF orientation
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, file: File): Bitmap {
        val orientation = getImageOrientation(file)

        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        if (rotated != bitmap) {
            bitmap.recycle()
        }

        return rotated
    }

    /**
     * Calculate optimal sample size for loading bitmap
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Load scaled bitmap
     */
    fun loadScaledBitmap(
        file: File,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            rotateBitmapIfNeeded(bitmap, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load bitmap from file with proper orientation
     * For OCR or full-size processing
     */
    fun loadBitmapFromFile(
        file: File,
        maxSize: Int = 2048
    ): Bitmap? {
        return try {
            loadBitmapWithOrientation(file, maxSize)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copy image file to permanent location
     */
    fun copyImageFile(source: File, destination: File): Boolean {
        return try {
            source.copyTo(destination, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Compress image file
     */
    fun compressImage(
        sourceFile: File,
        destinationFile: File,
        quality: Int = 90
    ): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            FileOutputStream(destinationFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save bitmap to file
     */
    fun saveBitmap(
        bitmap: Bitmap,
        destinationFile: File,
        quality: Int = 95
    ): Boolean {
        return try {
            FileOutputStream(destinationFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
