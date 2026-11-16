import Foundation

internal enum PresetStateMachine {

    static func resolveSteps(preset: VerificationPreset, nfcAvailable: Bool) -> [CaptureStep] {
        let raw: [CaptureStep] = switch preset {
        case .govEID:
            [.consent, .country, .document, .nfc, .liveness, .done]
        case .fintechKYC:
            [.consent, .country, .document, .poa, .nfc, .liveness, .done]
        case .kybEntity:
            [.consent, .country, .document, .businessRegistry, .liveness, .done]
        case .commerceTrust:
            [.consent, .document, .selfie, .done]
        case .workforceScreening, .healthPatient:
            [.consent, .country, .document, .liveness, .done]
        case .socialBadge:
            [.consent, .country, .document, .portraitVideo, .done]
        case .ageGate:
            [.consent, .country, .ageEstimation, .done]
        case .stepUpAuth:
            [.liveness, .done]
        case .scannerOnly:
            [.scanner, .done]
        }
        return nfcAvailable ? raw : raw.filter { $0 != .nfc }
    }

    static func targetFrames(preset: VerificationPreset, step: CaptureStep) -> Int {
        switch step {
        case .liveness: return preset == .govEID ? 7 : 5
        case .selfie, .ageEstimation: return 3
        case .document, .poa, .businessRegistry, .portraitVideo, .scanner: return 1
        default: return 0
        }
    }

    static func frameKind(for step: CaptureStep) -> FrameKind {
        switch step {
        case .document: return .documentFront
        case .poa: return .poaDocument
        case .businessRegistry: return .businessRegistry
        case .liveness, .selfie: return .livenessFrame
        case .ageEstimation: return .ageFrame
        case .portraitVideo: return .portraitVideoFrame
        case .scanner: return .scannerFrame
        default: return .documentFront
        }
    }
}
