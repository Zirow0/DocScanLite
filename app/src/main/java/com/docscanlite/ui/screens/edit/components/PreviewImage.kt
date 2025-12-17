package com.docscanlite.ui.screens.edit.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

/**
 * Preview Image Component
 * Shows preview bitmap if available (with filters/adjustments applied),
 * otherwise shows the original image file
 *
 * @param imagePath Path to the original/processed image file (fallback)
 * @param previewBitmap Optional bitmap with filters/adjustments applied (priority)
 * @param contentScale How to scale the image (default: Fit)
 * @param modifier Modifier for the container
 */
@Composable
fun PreviewImage(
    imagePath: String?,
    previewBitmap: Bitmap?,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Priority 1: Show preview bitmap if available
            previewBitmap != null -> {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Preview with filters",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            // Priority 2: Show original/processed image file
            imagePath != null -> {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Document",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            // Priority 3: Empty state (shouldn't happen in normal flow)
            else -> {
                // Could add placeholder or loading indicator here if needed
            }
        }
    }
}
