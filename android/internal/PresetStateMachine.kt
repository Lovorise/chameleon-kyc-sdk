package com.mtchship.chameleon.kyc.internal

import com.mtchship.chameleon.kyc.CaptureStep
import com.mtchship.chameleon.kyc.FrameKind
import com.mtchship.chameleon.kyc.VerificationPreset

internal object PresetStateMachine {

    fun resolveSteps(preset: VerificationPreset, nfcAvailable: Boolean): List<CaptureStep> {
        val raw = when (preset) {
            VerificationPreset.GOV_EID -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.DOCUMENT,
                CaptureStep.NFC, CaptureStep.LIVENESS, CaptureStep.DONE
            )
            VerificationPreset.FINTECH_KYC -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.DOCUMENT,
                CaptureStep.POA, CaptureStep.NFC, CaptureStep.LIVENESS, CaptureStep.DONE
            )
            VerificationPreset.KYB_ENTITY -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.DOCUMENT,
                CaptureStep.BUSINESS_REGISTRY, CaptureStep.LIVENESS, CaptureStep.DONE
            )
            VerificationPreset.COMMERCE_TRUST -> listOf(
                CaptureStep.CONSENT, CaptureStep.DOCUMENT, CaptureStep.SELFIE, CaptureStep.DONE
            )
            VerificationPreset.WORKFORCE_SCREENING, VerificationPreset.HEALTH_PATIENT -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.DOCUMENT,
                CaptureStep.LIVENESS, CaptureStep.DONE
            )
            VerificationPreset.SOCIAL_BADGE -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.DOCUMENT,
                CaptureStep.PORTRAIT_VIDEO, CaptureStep.DONE
            )
            VerificationPreset.AGE_GATE -> listOf(
                CaptureStep.CONSENT, CaptureStep.COUNTRY, CaptureStep.AGE_ESTIMATION, CaptureStep.DONE
            )
            VerificationPreset.STEP_UP_AUTH -> listOf(CaptureStep.LIVENESS, CaptureStep.DONE)
            VerificationPreset.SCANNER_ONLY -> listOf(CaptureStep.SCANNER, CaptureStep.DONE)
        }
        return if (nfcAvailable) raw else raw.filterNot { it == CaptureStep.NFC }
    }

    fun targetFramesForStep(preset: VerificationPreset, step: CaptureStep): Int = when (step) {
        CaptureStep.LIVENESS -> if (preset == VerificationPreset.GOV_EID) 7 else 5
        CaptureStep.SELFIE, CaptureStep.AGE_ESTIMATION -> 3
        CaptureStep.DOCUMENT, CaptureStep.POA, CaptureStep.BUSINESS_REGISTRY,
        CaptureStep.PORTRAIT_VIDEO, CaptureStep.SCANNER -> 1
        else -> 0
    }

    fun frameKindForStep(step: CaptureStep): FrameKind = when (step) {
        CaptureStep.DOCUMENT -> FrameKind.DOCUMENT_FRONT
        CaptureStep.POA -> FrameKind.POA_DOCUMENT
        CaptureStep.BUSINESS_REGISTRY -> FrameKind.BUSINESS_REGISTRY
        CaptureStep.LIVENESS, CaptureStep.SELFIE -> FrameKind.LIVENESS_FRAME
        CaptureStep.AGE_ESTIMATION -> FrameKind.AGE_FRAME
        CaptureStep.PORTRAIT_VIDEO -> FrameKind.PORTRAIT_VIDEO_FRAME
        CaptureStep.SCANNER -> FrameKind.SCANNER_FRAME
        else -> FrameKind.DOCUMENT_FRONT
    }
}
