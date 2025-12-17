package com.docscanlite.export.image

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * ImageExporter - Handles image export and sharing
 */
class ImageExporter(private val context: Context) {

    /**
     * Share image file via system share sheet
     * @param imageFile Source image file to share
     * @return Intent for sharing, or null if file doesn't exist
     */
    fun shareImage(imageFile: File): Intent? {
        if (!imageFile.exists()) {
            return null
        }

        try {
            // Create shared directory in cache
            val sharedDir = File(context.cacheDir, "shared")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }

            // Create copy with timestamp to avoid conflicts
            val fileName = "document_${System.currentTimeMillis()}.png"
            val sharedFile = File(sharedDir, fileName)

            // Copy file to shared location
            imageFile.copyTo(sharedFile, overwrite = true)

            // Get URI via FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "com.docscanlite.fileprovider",
                sharedFile
            )

            // Create share intent
            return Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
