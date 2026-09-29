package com.example.attendanceapp.ui.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * Full-bleed CameraX front-camera preview with an oval face-guide overlay and a capture
 * button. Reused by both face enrolment and attendance marking so the two flows feel identical.
 */
@Composable
fun CameraCaptureScreen(
    instructionText: String,
    isProcessing: Boolean,
    onCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    requireLocationPermission: Boolean = false,
    onLocationPermissionAvailable: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // False until the permission dialog has been shown at least once, so we don't show the
    // "denied" messaging before the user has had a chance to respond.
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
                hasLocationPermission
        permissionRequested = true
    }

    fun requestMissingPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (requireLocationPermission && !hasLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(Unit) { requestMissingPermissions() }

    // Fires the instant location permission is confirmed available — immediately if it was
    // already granted, or right after the user grants it via the dialog above. Starting the
    // caller's location acquisition here (rather than earlier) avoids racing ahead of the grant.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) onLocationPermissionAvailable()
    }

    val permissionsSatisfied = hasCameraPermission && (!requireLocationPermission || hasLocationPermission)

    if (!permissionsSatisfied) {
        if (!permissionRequested) {
            // Waiting on the first system prompt(s); nothing to show yet.
            return
        }

        val activity = context as? Activity
        val missingPermissions = buildList {
            if (!hasCameraPermission) add(Manifest.permission.CAMERA)
            if (requireLocationPermission && !hasLocationPermission) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val permanentlyDenied = activity != null && missingPermissions.isNotEmpty() &&
            missingPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

        val message = if (requireLocationPermission) {
            "Camera and location permissions are both required to mark attendance. " +
                "Please grant both permissions to continue."
        } else {
            "Camera permission is required to capture a selfie."
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            if (permanentlyDenied) {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            } else {
                Button(onClick = { requestMissingPermissions() }) {
                    Text("Grant permissions")
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
            Text(
                "Cancel",
                modifier = Modifier.padding(4.dp).clickableText(onCancel)
            )
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidViewCameraPreview(
            imageCapture = imageCapture,
            lifecycleOwner = lifecycleOwner
        )

        // Oval guide overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ovalWidth = size.width * 0.7f
            val ovalHeight = size.height * 0.42f
            val left = (size.width - ovalWidth) / 2
            val top = size.height * 0.18f
            drawOval(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Top-left Back Arrow Button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(instructionText, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Button(
                    onClick = {
                        capturePhoto(
                            imageCapture = imageCapture,
                            executor = executor,
                            onResult = onCaptured
                        )
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Capture")
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
            Text(
                "Cancel",
                color = Color.White,
                modifier = Modifier.padding(4.dp).clickableText(onCancel)
            )
        }
    }
}

@Composable
private fun AndroidViewCameraPreview(
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraCaptureScreen", "Error unbinding camera on dispose", e)
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> CameraSelector.DEFAULT_FRONT_CAMERA
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                } catch (e: Exception) {
                    Log.e("CameraCaptureScreen", "Error binding camera use cases", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.ExecutorService,
    onResult: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                onResult(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // Swallow: the UI simply stays on the capture screen for a retry.
            }
        }
    )
}

private fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
