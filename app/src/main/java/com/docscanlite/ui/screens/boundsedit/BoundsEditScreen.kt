package com.docscanlite.ui.screens.boundsedit

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.sqrt

/**
 * Screen for editing document boundaries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoundsEditScreen(
    imagePath: String,
    viewModel: BoundsEditViewModel = hiltViewModel(),
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val corners by viewModel.corners.collectAsState()

    LaunchedEffect(imagePath) {
        viewModel.loadImage(imagePath)
    }

    // Handle success state
    LaunchedEffect(uiState) {
        if (uiState is BoundsEditUiState.Success) {
            val documentId = (uiState as BoundsEditUiState.Success).documentId
            onConfirm(documentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adjust Document Bounds") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    // Confirm button
                    IconButton(
                        onClick = { viewModel.confirmBounds(imagePath) },
                        enabled = uiState is BoundsEditUiState.Ready && corners.size == 4
                    ) {
                        Icon(Icons.Default.Check, "Confirm")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is BoundsEditUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is BoundsEditUiState.Ready -> {
                    BoundsEditor(
                        imagePath = state.imagePath,
                        imageWidth = state.imageWidth,
                        imageHeight = state.imageHeight,
                        corners = corners,
                        onCornerDrag = { index, newPosition ->
                            viewModel.updateCorner(index, newPosition)
                        }
                    )
                }

                is BoundsEditUiState.Processing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Processing...",
                            modifier = Modifier.padding(top = 64.dp)
                        )
                    }
                }

                is BoundsEditUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.loadImage(imagePath) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                is BoundsEditUiState.Success -> {
                    // Handled by LaunchedEffect above
                }
            }
        }
    }
}

@Composable
fun BoundsEditor(
    imagePath: String,
    imageWidth: Int,
    imageHeight: Int,
    corners: List<PointF>,
    onCornerDrag: (Int, PointF) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Real container dimensions in pixels
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // Calculate image display size after ContentScale.Fit
        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val containerAspect = containerWidthPx / containerHeightPx

        val (displayWidthPx, displayHeightPx) = if (imageAspect > containerAspect) {
            // Image is wider - constrained by width
            containerWidthPx to containerWidthPx / imageAspect
        } else {
            // Image is taller - constrained by height
            containerHeightPx * imageAspect to containerHeightPx
        }

        val density = LocalDensity.current
        val displayWidthDp = with(density) { displayWidthPx.toDp() }
        val displayHeightDp = with(density) { displayHeightPx.toDp() }

        // Scale factors (image pixels -> screen pixels)
        val scaleX = displayWidthPx / imageWidth
        val scaleY = displayHeightPx / imageHeight

        // Display image with exact calculated size
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Document",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(displayWidthDp)
                .height(displayHeightDp)
        )

        // Overlay with draggable corners - same size as image
        if (corners.size == 4) {
            Box(
                modifier = Modifier
                    .width(displayWidthDp)
                    .height(displayHeightDp)
            ) {
                // Draw the boundary lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaledCorners = corners.map { corner ->
                        Offset(
                            x = corner.x * scaleX,
                            y = corner.y * scaleY
                        )
                    }

                    // Draw lines connecting corners
                    for (i in scaledCorners.indices) {
                        val start = scaledCorners[i]
                        val end = scaledCorners[(i + 1) % 4]
                        drawLine(
                            color = Color(0xFF00BCD4),
                            start = start,
                            end = end,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                // Draw draggable corner handles
                corners.forEachIndexed { index, _ ->
                    DraggableCorner(
                        index = index,
                        corners = corners,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        density = density,
                        onCornerDrag = onCornerDrag
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableCorner(
    index: Int,
    corners: List<PointF>,
    scaleX: Float,  // pixels per image pixel
    scaleY: Float,  // pixels per image pixel
    imageWidth: Int,
    imageHeight: Int,
    density: Density,
    onCornerDrag: (Int, PointF) -> Unit
) {
    val corner = corners.getOrNull(index) ?: return

    // Ensure corner position is within bounds for display
    val safeCornerX = corner.x.coerceIn(0f, imageWidth.toFloat())
    val safeCornerY = corner.y.coerceIn(0f, imageHeight.toFloat())

    // Convert pixel position to dp for offset
    val cornerXDp = with(density) { (safeCornerX * scaleX).toDp() }
    val cornerYDp = with(density) { (safeCornerY * scaleY).toDp() }

    // Always get the latest corners list
    val currentCorners by rememberUpdatedState(corners)

    // Track the corner position at the start of the drag gesture
    var dragStartCorner by remember { mutableStateOf(corner) }

    // Minimum margin from edges (in image coordinates)
    val minMargin = 10f
    val maxX = (imageWidth - minMargin).coerceAtLeast(minMargin)
    val maxY = (imageHeight - minMargin).coerceAtLeast(minMargin)

    Box(
        modifier = Modifier
            .offset(x = cornerXDp - 36.dp, y = cornerYDp - 36.dp)
            .size(72.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        // Capture the current corner position when drag starts
                        val currentCorner = currentCorners.getOrNull(index) ?: return@detectDragGestures
                        // Ensure start position is clamped
                        dragStartCorner = PointF(
                            currentCorner.x.coerceIn(minMargin, maxX),
                            currentCorner.y.coerceIn(minMargin, maxY)
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        // dragAmount is in pixels, scaleX/Y converts image coords to pixels
                        val newX = dragStartCorner.x + (dragAmount.x / scaleX)
                        val newY = dragStartCorner.y + (dragAmount.y / scaleY)

                        // Clamp to image bounds with margin
                        val clampedX = newX.coerceIn(minMargin, maxX)
                        val clampedY = newY.coerceIn(minMargin, maxY)

                        // Update the drag start position for next iteration (use clamped values!)
                        dragStartCorner = PointF(clampedX, clampedY)

                        onCornerDrag(index, PointF(clampedX, clampedY))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Draw outer circle (stroke)
            drawCircle(
                color = Color(0xFF00BCD4),
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw inner circle (filled)
            drawCircle(
                color = Color(0x8000BCD4),
                radius = 12.dp.toPx(),
                center = center
            )
        }
    }
}
