package com.mtchship.chameleon.kyc.internal

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.mtchship.chameleon.kyc.ChameleonError
import com.mtchship.chameleon.kyc.ChameleonErrorCode
import com.mtchship.chameleon.kyc.FrameKind
import com.mtchship.chameleon.kyc.FrameMimeType
import com.mtchship.chameleon.kyc.FramePayload
import com.mtchship.chameleon.kyc.QualityMetrics
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class YuvPreFlightAnalyzer(
    private val frameKindProvider: () -> FrameKind?,
    private val onAcceptedFrame: (FramePayload) -> Unit,
    private val onQualityWarning: (ChameleonError) -> Unit
) : ImageAnalysis.Analyzer {

    private val isBusy = AtomicBoolean(false)
    private val sequenceCounter = AtomicInteger(0)
    private val rng = SecureRandom()
    private var lastWarningMs = 0L

    override fun analyze(image: ImageProxy) {
        val kind = frameKindProvider()
        if (kind == null || !isBusy.compareAndSet(false, true)) {
            image.close()
            return
        }

        try {
            val metrics = evaluateYPlane(image)
            val warning = validateMetrics(metrics)
            if (warning != null) {
                val now = System.currentTimeMillis()
                if (now - lastWarningMs > 1_200L) {
                    lastWarningMs = now
                    onQualityWarning(warning)
                }
                return
            }

            val encoded = encodeNv21ToCompressedBytes(image)
            onAcceptedFrame(
                FramePayload(
                    bytes = encoded,
                    mimeType = FrameMimeType.JPEG,
                    kind = kind,
                    sequence = sequenceCounter.incrementAndGet(),
                    timestampMs = System.currentTimeMillis(),
                    nonce = generateNonce(),
                    qualityMetrics = metrics
                )
            )
        } finally {
            isBusy.set(false)
            image.close()
        }
    }

    private fun evaluateYPlane(image: ImageProxy): QualityMetrics {
        val yPlane = image.planes
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = image.width
        val height = image.height

        val step = maxOf(1, minOf(width, height) / 180)
        var sumLuma = 0L
        var glareCount = 0L
        var laplacianSum = 0.0
        var samples = 0L

        for (y in step until (height - step) step step) {
            val rowOffset = y * rowStride
            val prevRow = (y - step) * rowStride
            val nextRow = (y + step) * rowStride

            for (x in step until (width - step) step step) {
                val center = buffer.get(rowOffset + x * pixelStride).toInt() and 0xFF
                sumLuma += center
                if (center >= 245) glareCount++

                val top = buffer.get(prevRow + x * pixelStride).toInt() and 0xFF
                val bottom = buffer.get(nextRow + x * pixelStride).toInt() and 0xFF
                val left = buffer.get(rowOffset + (x - step) * pixelStride).toInt() and 0xFF
                val right = buffer.get(rowOffset + (x + step) * pixelStride).toInt() and 0xFF

                val lap = (top + bottom + left + right) - (4 * center)
                laplacianSum += (lap * lap).toDouble()
                samples++
            }
        }

        val count = maxOf(1L, samples)
        return QualityMetrics(
            lumaMean = sumLuma.toFloat() / count,
            glareRatio = glareCount.toFloat() / count,
            laplacianVariance = (laplacianSum / count).toFloat()
        )
    }

    private fun validateMetrics(m: QualityMetrics): ChameleonError? = when {
        m.lumaMean < 45f || m.lumaMean > 225f -> ChameleonError(
            ChameleonErrorCode.ERR_PREFLIGHT_LUMA, "Adjust lighting conditions", true
        )
        m.glareRatio > 0.08f -> ChameleonError(
            ChameleonErrorCode.ERR_PREFLIGHT_GLARE, "Reduce surface glare", true
        )
        m.laplacianVariance < 120f -> ChameleonError(
            ChameleonErrorCode.ERR_PREFLIGHT_BLUR, "Hold steady to focus", true
        )
        else -> null
    }

    private fun encodeNv21ToCompressedBytes(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val nv21 = ByteArray(width * height * 3 / 2)

        try {
            val yPlane = image.planes
            val uPlane = image.planes
            val vPlane = image.planes

            var pos = 0
            for (row in 0 until height) {
                val yStart = row * yPlane.rowStride
                for (col in 0 until width) {
                    nv21[pos++] = yPlane.buffer.get(yStart + col * yPlane.pixelStride)
                }
            }
            for (row in 0 until height / 2) {
                val vStart = row * vPlane.rowStride
                val uStart = row * uPlane.rowStride
                for (col in 0 until width / 2) {
                    nv21[pos++] = vPlane.buffer.get(vStart + col * vPlane.pixelStride)
                    nv21[pos++] = uPlane.buffer.get(uStart + col * uPlane.pixelStride)
                }
            }

            val out = ByteArrayOutputStream(64 * 1024)
            YuvImage(nv21, ImageFormat.NV21, width, height, null)
                .compressToJpeg(Rect(0, 0, width, height), 86, out)
            return out.toByteArray()
        } finally {
            Arrays.fill(nv21, 0.toByte())
        }
    }

    private fun generateNonce(): String {
        val raw = ByteArray(12)
        rng.nextBytes(raw)
        return raw.joinToString("") { "%02x".format(it) }
    }
}
