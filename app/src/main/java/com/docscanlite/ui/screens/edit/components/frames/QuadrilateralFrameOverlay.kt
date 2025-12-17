package com.docscanlite.ui.screens.edit.components.frames

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Quadrilateral frame overlay with 4 draggable corner points
 * Used for document bounds detection with arbitrary quadrilateral shapes
 *
 * @param imageWidth Original image width in pixels
 * @param imageHeight Original image height in pixels
 * @param corners List of 4 corner points in image coordinates (PointF)
 * @param transform Current image transformation state (for zoom/pan sync)
 * @param onCornerDrag Callback when a corner is dragged with new position in image coordinates
 * @param frameColor Color of the frame lines
 * @param handleSize Size of the draggable corner handles
 * @param strokeWidth Width of the frame lines
 */
@Composable
fun QuadrilateralFrameOverlay(
    imageWidth: Int,
    imageHeight: Int,
    corners: List<PointF>,
    transform: ImageTransform = ImageTransform.Default,
    onCornerDrag: (Int, PointF) -> Unit,
    modifier: Modifier = Modifier,
    frameColor: Color = Color(0xFF00BCD4),
    handleSize: Dp = 72.dp,
    strokeWidth: Dp = 3.dp
) {
    if (corners.size != 4) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Container dimensions in pixels
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // Calculate image display size after ContentScale.Fit
        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val containerAspect = containerWidthPx / containerHeightPx

        val (displayWidthPx, displayHeightPx) = if (imageAspect > containerAspect) {
            containerWidthPx to containerWidthPx / imageAspect
        } else {
            containerHeightPx * imageAspect to containerHeightPx
        }

        val density = LocalDensity.current
        val displayWidthDp = with(density) { displayWidthPx.toDp() }
        val displayHeightDp = with(density) { displayHeightPx.toDp() }

        // Scale factors (image pixels -> screen pixels)
        val scaleX = displayWidthPx / imageWidth
        val scaleY = displayHeightPx / imageHeight

        // Overlay box - same size as image, with transform applied
        Box(
            modifier = Modifier
                .width(displayWidthDp)
                .height(displayHeightDp)
                .graphicsLayer {
                    this.scaleX = transform.scale
                    this.scaleY = transform.scale
                    this.translationX = transform.offsetX
                    this.translationY = transform.offsetY
                }
        ) {
            // Draw the boundary lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaledCorners = corners.map { corner ->
                    Offset(
                        x = corner.x * scaleX,
                        y = corner.y * scaleY
                    )
                }

                // Draw lines connecting corners in a loop
                for (i in scaledCorners.indices) {
                    val start = scaledCorners[i]
                    val end = scaledCorners[(i + 1) % 4]
                    drawLine(
                        color = frameColor,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth.toPx()
                    )
                }
            }

            // Draw draggable corner handles
            corners.forEachIndexed { index, _ ->
                DraggableCornerHandle(
                    index = index,
                    corners = corners,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    transform = transform,
                    density = density,
                    handleSize = handleSize,
                    handleColor = frameColor,
                    onCornerDrag = onCornerDrag
                )
            }
        }
    }
}

/**
 * Draggable corner handle
 */
@Composable
private fun DraggableCornerHandle(
    index: Int,
    corners: List<PointF>,
    scaleX: Float,
    scaleY: Float,
    imageWidth: Int,
    imageHeight: Int,
    transform: ImageTransform,
    density: Density,
    handleSize: Dp,
    handleColor: Color,
    onCornerDrag: (Int, PointF) -> Unit
) {
    val corner = corners.getOrNull(index) ?: return

    // Convert pixel position to dp for offset
    val cornerXDp = with(density) { (corner.x * scaleX).toDp() }
    val cornerYDp = with(density) { (corner.y * scaleY).toDp() }

    val currentCorners by rememberUpdatedState(corners)
    var dragStartCorner by remember { mutableStateOf(corner) }

    // Minimum margin from edges
    val minMargin = 10f
    val maxX = (imageWidth - minMargin).coerceAtLeast(minMargin)
    val maxY = (imageHeight - minMargin).coerceAtLeast(minMargin)

    Box(
        modifier = Modifier
            .offset(x = cornerXDp - handleSize / 2, y = cornerYDp - handleSize / 2)
            .size(handleSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        val currentCorner = currentCorners.getOrNull(index) ?: return@detectDragGestures
                        dragStartCorner = PointF(
                            currentCorner.x.coerceIn(minMargin, maxX),
                            currentCorner.y.coerceIn(minMargin, maxY)
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        // Adjust dragAmount for current transform scale
                        val adjustedDragX = dragAmount.x / transform.scale
                        val adjustedDragY = dragAmount.y / transform.scale

                        // dragAmount is in pixels, scaleX/Y converts image coords to pixels
                        val newX = dragStartCorner.x + (adjustedDragX / scaleX)
                        val newY = dragStartCorner.y + (adjustedDragY / scaleY)

                        // Clamp to image bounds with margin
                        val clampedX = newX.coerceIn(minMargin, maxX)
                        val clampedY = newY.coerceIn(minMargin, maxY)

                        dragStartCorner = PointF(clampedX, clampedY)

                        onCornerDrag(index, PointF(clampedX, clampedY))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Outer circle (stroke)
            drawCircle(
                color = handleColor,
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner circle (filled with alpha)
            drawCircle(
                color = handleColor.copy(alpha = 0.5f),
                radius = 12.dp.toPx(),
                center = center
            )
        }
    }
}
