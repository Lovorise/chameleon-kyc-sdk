package com.mtchship.chameleon.kyc

import androidx.compose.runtime.Immutable

enum class VerificationPreset {
    GOV_EID, FINTECH_KYC, KYB_ENTITY, COMMERCE_TRUST, WORKFORCE_SCREENING,
    HEALTH_PATIENT, SOCIAL_BADGE, AGE_GATE, STEP_UP_AUTH, SCANNER_ONLY
}

enum class DocumentKind {
    ID_CARD, PASSPORT, DRIVING_LICENSE, RESIDENCE_PERMIT,
    POA_DOCUMENT, BUSINESS_REGISTRY, INSURANCE_CARD, PASS_QR
}

enum class FrameKind {
    DOCUMENT_FRONT, DOCUMENT_BACK, POA_DOCUMENT, BUSINESS_REGISTRY,
    LIVENESS_FRAME, AGE_FRAME, PORTRAIT_VIDEO_FRAME, SCANNER_FRAME
}

enum class ChameleonErrorCode {
    ERR_CAMERA_PERMISSION, ERR_CAMERA_UNAVAILABLE, ERR_PREFLIGHT_LUMA,
    ERR_PREFLIGHT_GLARE, ERR_PREFLIGHT_BLUR, ERR_TRANSPORT,
    ERR_SESSION_EXPIRED, ERR_NFC_UNAVAILABLE, ERR_NFC_READ_FAILED
}

@Immutable
data class ChameleonError(
    val code: ChameleonErrorCode,
    val message: String,
    val recoverable: Boolean
)

@Immutable
data class CountryOption(
    val code: String,
    val name: String,
    val docKinds: List<DocumentKind>? = null
)

@Immutable
data class AutoContextResult(
    val locale: String,
    val isRtl: Boolean,
    val isDarkTheme: Boolean,
    val countryCode: String? = null
)

@Immutable
data class QualityMetrics(
    val lumaMean: Float,
    val glareRatio: Float,
    val laplacianVariance: Float
)

enum class FrameMimeType(val value: String) {
    WEBP("image/webp"),
    JPEG("image/jpeg")
}

class FramePayload(
    val bytes: ByteArray,
    val mimeType: FrameMimeType,
    val kind: FrameKind,
    val sequence: Int,
    val timestampMs: Long,
    val nonce: String,
    val qualityMetrics: QualityMetrics
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramePayload) return false
        return sequence == other.sequence && timestampMs == other.timestampMs &&
            nonce == other.nonce && kind == other.kind
    }

    override fun hashCode(): Int =
        31 * (31 * (31 * sequence + timestampMs.hashCode()) + nonce.hashCode()) + kind.hashCode()
}

@Immutable
data class SignedReceipt(
    val sessionId: String,
    val jwsToken: String,
    val completedAt: String
)

@Immutable
data class ChameleonLabels(
    val consentTitle: String? = null,
    val consentBody: String? = null,
    val consentAcceptCta: String? = null,
    val countrySelectTitle: String? = null,
    val documentCaptureHint: String? = null,
    val nfcScanPrompt: String? = null,
    val livenessPrompt: String? = null,
    val processingLabel: String? = null
)
