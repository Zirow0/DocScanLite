package com.docscanlite.ui.screens.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscanlite.core.utils.FileUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File

/**
 * Camera Screen - Document capture screen with CameraX
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onPhotoTaken: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CameraUiState.PhotoCaptured -> {
                onPhotoTaken(state.imagePath)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Request camera permission if not granted
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cameraPermission.status.isGranted -> {
                // Camera preview
                CameraContent(
                    context = context,
                    onImageCaptureReady = { capture ->
                        imageCapture = capture
                    },
                    onNavigateBack = onNavigateBack,
                    onCaptureClick = {
                        isCapturing = true
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onPhotoCaptured = { file ->
                                isCapturing = false
                                viewModel.onPhotoCaptured(file)
                            },
                            onError = { error ->
                                isCapturing = false
                                // Show error (will be enhanced later with Snackbar)
                            }
                        )
                    },
                    isCapturing = isCapturing
                )

                // Loading overlay
                if (uiState is CameraUiState.Processing || isCapturing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            cameraPermission.status.shouldShowRationale -> {
                // Show rationale
                CameraPermissionRationale(
                    onRequestPermission = { cameraPermission.launchPermissionRequest() },
                    onNavigateBack = onNavigateBack
                )
            }
            else -> {
                // Permission denied permanently
                CameraPermissionDenied(onNavigateBack = onNavigateBack)
            }
        }
    }
}

@Composable
private fun CameraContent(
    context: Context,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onNavigateBack: () -> Unit,
    onCaptureClick: () -> Unit,
    isCapturing: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                androidx.camera.view.PreviewView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = androidx.camera.view.PreviewView.ScaleType.FIT_CENTER
                    implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            }
        )

        // Initialize Camera with both Preview and ImageCapture
        LaunchedEffect(previewView) {
            if (previewView != null) {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()

                // Create Preview use case with 9:16 aspect ratio
                val preview = androidx.camera.core.Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView!!.surfaceProvider)
                    }

                // Create ImageCapture use case with same 9:16 aspect ratio
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind BOTH use cases together
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )

                    onImageCaptureReady(imageCapture)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Top bar with back button
        TopAppBar(
            title = { Text("Зробити фото") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = Color.White
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Capture button
            Button(
                onClick = onCaptureClick,
                enabled = !isCapturing,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Зробити фото",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Дозвіл на камеру",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Для сканування документів потрібен доступ до камери",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text("Надати дозвіл")
            }
            TextButton(onClick = onNavigateBack) {
                Text("Скасувати")
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Дозвіл відхилено",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Надайте дозвіл на камеру в налаштуваннях додатку",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onNavigateBack) {
                Text("Назад")
            }
        }
    }
}

/**
 * Capture photo using ImageCapture
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onPhotoCaptured: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val imageFile = FileUtils.createTempImageFile(context)

    val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

    imageCapture?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoCaptured(imageFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
