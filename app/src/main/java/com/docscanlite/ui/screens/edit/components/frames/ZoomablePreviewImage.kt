package com.docscanlite.ui.screens.edit.components.frames

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

/**
 * Zoomable and pannable preview image
 * Uses cached bitmap from ViewModel for performance
 * Handles zoom and pan gestures when enabled
 */
@Composable
fun ZoomablePreviewImage(
    previewBitmap: Bitmap?,
    transform: ImageTransform = ImageTransform.Default,
    enableZoom: Boolean = false,
    onTransformChange: (ImageTransform) -> Unit = {},
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.offsetX
                translationY = transform.offsetY
            }
            .then(
                if (enableZoom) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (transform.scale * zoom).coerceIn(
                                ImageTransform.MIN_SCALE,
                                ImageTransform.MAX_SCALE
                            )
                            val newOffsetX = transform.offsetX + pan.x
                            val newOffsetY = transform.offsetY + pan.y

                            onTransformChange(
                                ImageTransform(
                                    scale = newScale,
                                    offsetX = newOffsetX,
                                    offsetY = newOffsetY
                                )
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )

        // Display cached bitmap from ViewModel
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Документ",
                contentScale = contentScale,
                modifier = imageModifier
            )
        }
    }
}
