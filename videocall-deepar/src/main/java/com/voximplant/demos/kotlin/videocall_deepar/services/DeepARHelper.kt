package com.voximplant.demos.kotlin.videocall_deepar.services

import ai.deepar.ar.*
import ai.deepar.ar.DeepAR
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.voximplant.demos.kotlin.videocall_deepar.BuildConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder


class DeepARHelper(private var context: Context) : AREventListener {
    private var deepAR: DeepAR = DeepAR(context)
    private var initialized: Boolean = false

    private var masks: ArrayList<String> = ArrayList()
    private var effects: ArrayList<String> = ArrayList()
    private var filters: ArrayList<String> = ArrayList()

    private val currentMask = 1
    private val currentEffect = 0
    private val currentFilter = 0

    private var buffers: Array<ByteBuffer?>? = null
    private var currentBuffer = 0
    private val numberOfBuffers = 2

    init {
        deepAR.setLicenseKey(BuildConfig.DEEP_AR_TOKEN)
        initializeFilters()
    }

    fun startDeepAR() = deepAR.initialize(context, this)

    fun setRenderSurface(surface: Surface?, size: Size) =
        deepAR.setRenderSurface(surface, size.width, size.height)

    private fun initializeFilters() {
        masks = arrayListOf(
            "none",
            "aviators",
            "bigmouth",
            "dalmatian",
            "flowers",
            "koala",
            "lion",
            "smallface",
            "teddycigar",
            "background_segmentation",
            "tripleface",
            "sleepingmask",
            "fatify",
            "obama",
            "mudmask",
            "pug",
            "slash",
            "twistedface",
            "grumpycat",
        )
        effects = arrayListOf(
            "none",
            "fire",
            "rain",
            "heart",
            "blizzard",
        )
        filters = arrayListOf(
            "none",
            "filmcolorperfection",
            "tv80",
            "drawingmanga",
            "sepia",
            "bleachbypass",
        )
    }

    private fun getFilterPath(filterName: String): String? =
        if (filterName == "none") null else "file:///android_asset/$filterName"

    fun stopDeepAR() {
        deepAR.release()
        initialized = false
    }

    fun processImage(image: ImageProxy, mirroring: Boolean) {
        if (initialized) {
            buffers = arrayOfNulls(numberOfBuffers)
            for (i in 0 until numberOfBuffers) {
                buffers?.set(i, ByteBuffer.allocateDirect(image.width * image.height * 3))
                buffers?.get(i)?.order(ByteOrder.nativeOrder())
                buffers?.get(i)?.position(0)
            }

            val byteData: ByteArray
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            byteData = ByteArray(ySize + uSize + vSize)

            yBuffer[byteData, 0, ySize]
            vBuffer[byteData, ySize, vSize]
            uBuffer[byteData, ySize + vSize, uSize]
            buffers?.get(currentBuffer)?.put(byteData)
            buffers?.get(currentBuffer)?.position(0)
            deepAR.receiveFrame(
                buffers?.get(currentBuffer),
                image.width, image.height,
                image.imageInfo.rotationDegrees,
                mirroring,
                DeepARImageFormat.YUV_420_888,
                image.planes[1].pixelStride
            )
            currentBuffer = (currentBuffer + 1) % numberOfBuffers
        }
    }

    override fun screenshotTaken(p0: Bitmap?) {}

    override fun videoRecordingStarted() {}

    override fun videoRecordingFinished() {}

    override fun videoRecordingFailed() {}

    override fun videoRecordingPrepared() {}

    override fun shutdownFinished() {}

    override fun initialized() {
        deepAR.switchEffect("mask", getFilterPath(masks[currentMask]))
        deepAR.switchEffect("effect", getFilterPath(effects[currentEffect]))
        deepAR.switchEffect("filter", getFilterPath(filters[currentFilter]))
        initialized = true
    }

    override fun faceVisibilityChanged(p0: Boolean) {}

    override fun imageVisibilityChanged(p0: String?, p1: Boolean) {}

    override fun frameAvailable(p0: Image?) {}

    override fun error(p0: ARErrorType?, p1: String?) {}

    override fun effectSwitched(p0: String?) {}
}