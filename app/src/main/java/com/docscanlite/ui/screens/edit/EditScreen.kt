package com.docscanlite.ui.screens.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.docscanlite.ui.screens.edit.adjust.AdjustTabContent
import com.docscanlite.ui.screens.edit.adjust.AdjustmentOverlay
import com.docscanlite.ui.screens.edit.bounds.BoundsTabContent
import com.docscanlite.ui.screens.edit.crop.CropTabContent
import com.docscanlite.ui.screens.edit.filter.FilterTabContent
import com.docscanlite.ui.screens.edit.rotate.RotateTabContent

/**
 * Edit Screen - Main coordinator for document editing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    imagePath: String? = null,
    documentId: String? = null,
    viewModel: EditViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val originalImagePath by viewModel.originalImagePath.collectAsState()
    val croppedImagePath by viewModel.croppedImagePath.collectAsState()
    val imageWidth by viewModel.imageWidth.collectAsState()
    val imageHeight by viewModel.imageHeight.collectAsState()
    val documentName by viewModel.documentName.collectAsState()
    val originalPreviewBitmap by viewModel.originalPreviewBitmap.collectAsState()
    val croppedPreviewBitmap by viewModel.croppedPreviewBitmap.collectAsState()

    // Get sub-ViewModels
    val boundsViewModel = viewModel.getBoundsViewModel()
    val filterViewModel = viewModel.getFilterViewModel()
    val adjustViewModel = viewModel.getAdjustViewModel()
    val rotateViewModel = viewModel.getRotateViewModel()
    val cropViewModel = viewModel.getCropViewModel()

    // Collect states from sub-ViewModels
    val corners by boundsViewModel.corners.collectAsState()
    val originalImageWidth by boundsViewModel.originalImageWidth.collectAsState()
    val originalImageHeight by boundsViewModel.originalImageHeight.collectAsState()
    val boundsApplied by boundsViewModel.boundsApplied.collectAsState()

    val selectedFilter by filterViewModel.selectedFilter.collectAsState()
    val previewBitmap by filterViewModel.previewBitmap.collectAsState()

    val brightness by adjustViewModel.brightness.collectAsState()
    val contrast by adjustViewModel.contrast.collectAsState()
    val saturation by adjustViewModel.saturation.collectAsState()
    val savedBrightness by adjustViewModel.savedBrightness.collectAsState()
    val savedContrast by adjustViewModel.savedContrast.collectAsState()
    val savedSaturation by adjustViewModel.savedSaturation.collectAsState()

    val rotationAngle by rotateViewModel.rotationAngle.collectAsState()

    val cropState by cropViewModel.cropState.collectAsState()

    // Adjustment overlay state (shared across screen)
    var adjustmentOverlayState by remember { mutableStateOf<com.docscanlite.ui.screens.edit.adjust.AdjustmentOverlayState?>(null) }

    // Rename dialog state
    var showRenameDialog by remember { mutableStateOf(false) }

    // Start with BOUNDS tab if coming from camera (has imagePath)
    var selectedTab by remember {
        mutableStateOf(if (imagePath != null) EditTab.BOUNDS else EditTab.FILTER)
    }

    // Load image on first composition
    LaunchedEffect(imagePath, documentId) {
        when {
            imagePath != null -> viewModel.loadImageFromPath(imagePath)
            documentId != null -> viewModel.loadDocumentById(documentId)
        }
    }

    // Handle saved state
    LaunchedEffect(uiState) {
        if (uiState is EditUiState.Saved) {
            val docId = (uiState as EditUiState.Saved).documentId
            onSave(docId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = documentName ?: "Новий документ",
                            modifier = Modifier.clickable { showRenameDialog = true }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveDocument() },
                            enabled = uiState is EditUiState.Ready
                        ) {
                            Text("Зберегти")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                EditToolbar(
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        // Auto-apply bounds when leaving BOUNDS tab if not yet applied
                        if (selectedTab == EditTab.BOUNDS && newTab != EditTab.BOUNDS && !boundsApplied && corners.size == 4) {
                            viewModel.applyBounds()
                        }

                        // Auto-apply crop when leaving CROP tab if crop mode is active
                        if (selectedTab == EditTab.CROP && newTab != EditTab.CROP &&
                            cropState.mode != CropMode.NONE && cropState.bounds != null) {
                            viewModel.applyCrop()
                        }

                        selectedTab = newTab
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main content with padding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (val state = uiState) {
                        is EditUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        is EditUiState.ProcessingBounds -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Обробка границь...")
                            }
                        }

                        is EditUiState.Saving -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Збереження...")
                            }
                        }

                        is EditUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Помилка: ${state.message}",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = { imagePath?.let { viewModel.loadImageFromPath(it) } },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Повторити")
                                }
                            }
                        }

                        is EditUiState.Ready, is EditUiState.Saved -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                when (selectedTab) {
                                    EditTab.BOUNDS -> {
                                        BoundsTabContent(
                                            previewBitmap = originalPreviewBitmap,
                                            imageWidth = originalImageWidth,
                                            imageHeight = originalImageHeight,
                                            corners = corners,
                                            onCornerDrag = { index, newPosition ->
                                                boundsViewModel.updateCorner(index, newPosition)
                                            }
                                        )
                                    }

                                    EditTab.FILTER -> {
                                        val displayPath = croppedImagePath ?: originalImagePath
                                        FilterTabContent(
                                            imagePath = displayPath,
                                            previewBitmap = previewBitmap,
                                            selectedFilter = selectedFilter,
                                            onFilterSelected = { filter ->
                                                filterViewModel.setFilter(
                                                    filter,
                                                    displayPath,
                                                    brightness,
                                                    contrast,
                                                    saturation
                                                )
                                            }
                                        )
                                    }

                                    EditTab.ADJUST -> {
                                        val displayPath = croppedImagePath ?: originalImagePath
                                        AdjustTabContent(
                                            imagePath = displayPath,
                                            previewBitmap = previewBitmap,
                                            brightness = brightness,
                                            contrast = contrast,
                                            saturation = saturation,
                                            savedBrightness = savedBrightness,
                                            savedContrast = savedContrast,
                                            savedSaturation = savedSaturation,
                                            onBrightnessChange = {
                                                adjustViewModel.setBrightness(it)
                                                viewModel.generatePreview()
                                            },
                                            onContrastChange = {
                                                adjustViewModel.setContrast(it)
                                                viewModel.generatePreview()
                                            },
                                            onSaturationChange = {
                                                adjustViewModel.setSaturation(it)
                                                viewModel.generatePreview()
                                            },
                                            onReset = {
                                                adjustViewModel.resetAdjustments()
                                                filterViewModel.clearPreview()
                                            },
                                            onOverlayStateChange = { adjustmentOverlayState = it }
                                        )
                                    }

                                    EditTab.ROTATE -> {
                                        val displayPath = croppedImagePath ?: originalImagePath
                                        RotateTabContent(
                                            imagePath = displayPath,
                                            currentAngle = rotationAngle,
                                            onRotate = { degrees -> viewModel.rotateImage(degrees) }
                                        )
                                    }

                                    EditTab.CROP -> {
                                        // Use cropped bitmap if available, otherwise original
                                        val displayBitmap = croppedPreviewBitmap ?: originalPreviewBitmap
                                        CropTabContent(
                                            previewBitmap = displayBitmap,
                                            cropState = cropState,
                                            onPresetClick = { preset, width, height ->
                                                cropViewModel.setCropPreset(preset, width, height)
                                            },
                                            onBoundsChange = { cropViewModel.updateCropBounds(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Adjustment overlay - positioned in center
        if (adjustmentOverlayState != null) {
            adjustmentOverlayState?.let { state ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AdjustmentOverlay(
                        parameter = state.parameter,
                        value = state.value,
                        range = state.range,
                        formatValue = state.formatValue,
                        onDismiss = { adjustmentOverlayState = null }
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var tempName by remember { mutableStateOf(documentName ?: "") }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Назва документа") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Введіть назву") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.setDocumentName(tempName)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun EditToolbar(
    selectedTab: EditTab,
    onTabSelected: (EditTab) -> Unit
) {
    NavigationBar {
        EditTab.entries.forEach { tab ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) },
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

enum class EditTab(val title: String, val icon: ImageVector) {
    BOUNDS("Границі", Icons.Default.CropFree),
    FILTER("Фільтри", Icons.Default.FilterAlt),
    ADJUST("Налаштування", Icons.Default.Tune),
    ROTATE("Повернути", Icons.Default.Rotate90DegreesCw),
    CROP("Обрізати", Icons.Default.Crop)
}
