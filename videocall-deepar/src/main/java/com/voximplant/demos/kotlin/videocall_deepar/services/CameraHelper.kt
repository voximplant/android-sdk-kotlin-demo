package com.voximplant.demos.kotlin.videocall_deepar.services

import ai.deepar.ar.CameraResolutionPreset
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.voximplant.demos.kotlin.utils.APP_TAG
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraHelper(private var context: Context) {
    private var started: Boolean = false
    private lateinit var customLifecycle: CustomLifecycle
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraPreset: CameraResolutionPreset
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

    var onImageReceived: ((image: ImageProxy, mirror: Boolean) -> Unit)? = null

    fun startCamera(cameraPreset: CameraResolutionPreset, lensReset: Boolean = false) {
        this.cameraPreset = cameraPreset

        if (lensReset) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        }
        customLifecycle = CustomLifecycle()
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
        started = true
    }

    private fun bindCameraUseCases() {
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(cameraPreset.height, cameraPreset.width))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor) { image ->
            onImageReceived?.invoke(image, lensFacing == CameraSelector.LENS_FACING_FRONT)
            image.close()
        }

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                customLifecycle,
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(APP_TAG, "Use case binding failed", e)
        }
    }

    fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        if (started) {
            bindCameraUseCases()
        }
    }

    fun stopCamera() {
        if (started) {
            imageAnalysis.clearAnalyzer()
            Handler(Looper.getMainLooper()).post {
                cameraProvider.unbindAll()
            }
            cameraProviderFuture.cancel(true)
            customLifecycle.destroy()
            started = false
        }
    }
}

class CustomLifecycle : LifecycleOwner {
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun destroy() = Handler(Looper.getMainLooper()).post {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
}