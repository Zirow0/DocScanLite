package com.docscanlite.core.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for file management
 */
object FileUtils {

    private const val DOCUMENTS_DIRECTORY = "Documents"
    private const val THUMBNAILS_DIRECTORY = "Thumbnails"
    private const val TEMP_DIRECTORY = "Temp"

    /**
     * Get the documents directory
     */
    fun getDocumentsDirectory(context: Context): File {
        val dir = File(context.filesDir, DOCUMENTS_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get the thumbnails directory
     */
    fun getThumbnailsDirectory(context: Context): File {
        val dir = File(context.filesDir, THUMBNAILS_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get the temp directory
     */
    fun getTempDirectory(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create a temporary image file for camera capture
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        return File.createTempFile(
            imageFileName,
            ".jpg",
            getTempDirectory(context)
        )
    }

    /**
     * Create a permanent image file in documents directory
     */
    fun createDocumentImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "DOC_${timeStamp}.jpg"
        return File(getDocumentsDirectory(context), imageFileName)
    }

    /**
     * Create a thumbnail file
     */
    fun createThumbnailFile(context: Context, documentId: String): File {
        val thumbnailFileName = "thumb_${documentId}.jpg"
        return File(getThumbnailsDirectory(context), thumbnailFileName)
    }

    /**
     * Delete a file safely
     */
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size in human-readable format
     */
    fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.2f MB", mb)
            kb >= 1 -> String.format(Locale.getDefault(), "%.2f KB", kb)
            else -> String.format(Locale.getDefault(), "%d B", size)
        }
    }

    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDir = getTempDirectory(context)
            tempDir.listFiles()?.forEach { file ->
                // Delete files older than 1 hour
                val hourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                if (file.lastModified() < hourAgo) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
