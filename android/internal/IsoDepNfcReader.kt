package com.mtchship.chameleon.kyc.internal

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.mtchship.chameleon.kyc.ChameleonError
import com.mtchship.chameleon.kyc.ChameleonErrorCode
import com.mtchship.chameleon.kyc.FrameKind
import com.mtchship.chameleon.kyc.FrameMimeType
import com.mtchship.chameleon.kyc.FramePayload
import com.mtchship.chameleon.kyc.QualityMetrics
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Arrays

internal class IsoDepNfcReader(
    private val activity: Activity,
    private val onChipPayloadReady: (FramePayload) -> Unit,
    private val onNfcError: (ChameleonError) -> Unit
) : NfcAdapter.ReaderCallback {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val rng = SecureRandom()

    fun start() {
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter?.enableReaderMode(activity, this, flags, null)
    }

    fun stop() {
        runCatching { adapter?.disableReaderMode(activity) }
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: run {
            onNfcError(ChameleonError(ChameleonErrorCode.ERR_NFC_READ_FAILED, "Unsupported NFC tag", true))
            return
        }

        val buffer = ByteArrayOutputStream(16 * 1024)
        try {
            isoDep.timeout = 5_000
            isoDep.connect()

            val selectAid = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x0C, 0x07,
                0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
            )
            val aidResponse = isoDep.transceive(selectAid)
            try {
                if (!isStatusOk(aidResponse)) {
                    throw IllegalStateException("eMRTD application not present on chip")
                }
                buffer.write(aidResponse)
                readElementaryFile(isoDep, byteArrayOf(0x01, 0x1E), buffer)
                readElementaryFile(isoDep, byteArrayOf(0x01, 0x1D), buffer)
            } finally {
                Arrays.fill(aidResponse, 0.toByte())
            }

            val chipBytes = buffer.toByteArray()
            val nonceBytes = ByteArray(12).also { rng.nextBytes(it) }
            onChipPayloadReady(
                FramePayload(
                    bytes = chipBytes,
                    mimeType = FrameMimeType.WEBP,
                    kind = FrameKind.DOCUMENT_FRONT,
                    sequence = 900,
                    timestampMs = System.currentTimeMillis(),
                    nonce = nonceBytes.joinToString("") { "%02x".format(it) },
                    qualityMetrics = QualityMetrics(128f, 0f, 1000f)
                )
            )
        } catch (err: Exception) {
            onNfcError(
                ChameleonError(
                    ChameleonErrorCode.ERR_NFC_READ_FAILED,
                    err.message ?: "NFC chip read failed",
                    recoverable = true
                )
            )
        } finally {
            runCatching { isoDep.close() }
            buffer.reset()
        }
    }

    private fun readElementaryFile(isoDep: IsoDep, fileId: ByteArray, out: ByteArrayOutputStream) {
        val selectFile = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, fileId, fileId)
        val selResp = isoDep.transceive(selectFile)
        try {
            if (!isStatusOk(selResp)) return
            val readBinary = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x10)
            val dataResp = isoDep.transceive(readBinary)
            try {
                if (isStatusOk(dataResp)) out.write(dataResp)
            } finally {
                Arrays.fill(dataResp, 0.toByte())
            }
        } finally {
            Arrays.fill(selResp, 0.toByte())
        }
    }

    private fun isStatusOk(response: ByteArray): Boolean =
        response.size >= 2 &&
            response[response.size - 2] == 0x90.toByte() &&
            response[response.size - 1] == 0x00.toByte()
}
