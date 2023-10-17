package com.medium.cameraapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.ScaleGestureDetector
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            mainExecutor
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
suspend fun Context.createVideoCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView,
    context: Context
): VideoCapture<Recorder> {
    val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    val qualitySelector = QualitySelector.from(
        Quality.FHD,
        FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
    )
    val recorder = Recorder.Builder()
        .setExecutor(mainExecutor)
        .setQualitySelector(qualitySelector)
        .build()
    val videoCapture = VideoCapture.withOutput(recorder)

    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        videoCapture
    )
    // Getting the CameraControl instance from the camera
    val cameraControl = camera.cameraControl

    val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Get the camera's current zoom ratio
            val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 0F

            // Get the pinch gesture's scaling factor
            val delta = detector.scaleFactor

            // Update the camera's zoom ratio. This is an asynchronous operation that returns
            // a ListenableFuture, allowing you to listen to when the operation completes.
            cameraControl.setZoomRatio(currentZoomRatio * delta)

            // Return true, as the event was handled
            return true
        }
    }
    val scaleGestureDetector = ScaleGestureDetector(context, listener)

// Attach the pinch gesture listener to the viewfinder
    previewView.setOnTouchListener { _, event ->
        scaleGestureDetector.onTouchEvent(event)
        return@setOnTouchListener true
    }

    return videoCapture
}

@SuppressLint("MissingPermission")
fun startRecordingVideo(
    context: Context,
    filenameFormat: String,
    videoCapture: VideoCapture<Recorder>,
    outputDirectory: File,
    executor: Executor,
    audioEnabled: Boolean,
    consumer: Consumer<VideoRecordEvent>
): Recording {
    val videoFile = File(
        outputDirectory,
        SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply { if (audioEnabled) withAudioEnabled() }
        .start(executor, consumer)
}
