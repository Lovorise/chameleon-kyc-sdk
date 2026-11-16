package com.mtchship.chameleon.kyc.internal

import com.mtchship.chameleon.kyc.ChameleonTransport
import com.mtchship.chameleon.kyc.FramePayload
import com.mtchship.chameleon.kyc.SignedReceipt
import com.mtchship.chameleon.kyc.VerificationPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object CnameIngressClient {

    suspend fun uploadFrame(
        config: ChameleonTransport.Cname,
        sessionId: String,
        preset: VerificationPreset,
        payload: FramePayload
    ) = withContext(Dispatchers.IO) {
        val url = URL("${config.endpoint.trimEnd('/')}/v1/sessions/$sessionId/frames")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer ${config.keyToken}")
            setRequestProperty("Content-Type", payload.mimeType.value)
            setRequestProperty("X-Chameleon-Preset", preset.name)
            setRequestProperty("X-Chameleon-Frame-Kind", payload.kind.name)
            setRequestProperty("X-Chameleon-Sequence", payload.sequence.toString())
            setRequestProperty("X-Chameleon-Nonce", payload.nonce)
            setRequestProperty("X-Chameleon-Timestamp", payload.timestampMs.toString())
            config.headers.forEach { (k, v) -> setRequestProperty(k, v) }
            setFixedLengthStreamingMode(payload.bytes.size)
        }

        try {
            conn.outputStream.use { os ->
                os.write(payload.bytes)
                os.flush()
            }
            if (conn.responseCode !in 200..299) {
                throw IOException("Visual Clerk ingress rejected frame: HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun finalizeSession(
        config: ChameleonTransport.Cname,
        sessionId: String
    ): SignedReceipt = withContext(Dispatchers.IO) {
        val url = URL("${config.endpoint.trimEnd('/')}/v1/sessions/$sessionId/complete")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer ${config.keyToken}")
            setRequestProperty("Accept", "application/json")
            config.headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        try {
            if (conn.responseCode !in 200..299) {
                throw IOException("Visual Clerk session finalization failed: HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            SignedReceipt(
                sessionId = json.optString("sessionId", sessionId),
                jwsToken = json.getString("jwsToken"),
                completedAt = json.optString("completedAt", "")
            )
        } finally {
            conn.disconnect()
        }
    }
}
