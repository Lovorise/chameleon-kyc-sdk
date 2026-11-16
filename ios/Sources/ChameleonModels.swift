import Foundation
import SwiftUI

public enum VerificationPreset: String, Sendable, CaseIterable {
    case govEID = "GOV_EID"
    case fintechKYC = "FINTECH_KYC"
    case kybEntity = "KYB_ENTITY"
    case commerceTrust = "COMMERCE_TRUST"
    case workforceScreening = "WORKFORCE_SCREENING"
    case healthPatient = "HEALTH_PATIENT"
    case socialBadge = "SOCIAL_BADGE"
    case ageGate = "AGE_GATE"
    case stepUpAuth = "STEP_UP_AUTH"
    case scannerOnly = "SCANNER_ONLY"
}

public enum DocumentKind: String, Sendable {
    case idCard = "ID_CARD"
    case passport = "PASSPORT"
    case drivingLicense = "DRIVING_LICENSE"
    case residencePermit = "RESIDENCE_PERMIT"
    case poaDocument = "POA_DOCUMENT"
    case businessRegistry = "BUSINESS_REGISTRY"
    case insuranceCard = "INSURANCE_CARD"
    case passQR = "PASS_QR"
}

public enum FrameKind: String, Sendable {
    case documentFront = "DOCUMENT_FRONT"
    case documentBack = "DOCUMENT_BACK"
    case poaDocument = "POA_DOCUMENT"
    case businessRegistry = "BUSINESS_REGISTRY"
    case livenessFrame = "LIVENESS_FRAME"
    case ageFrame = "AGE_FRAME"
    case portraitVideoFrame = "PORTRAIT_VIDEO_FRAME"
    case scannerFrame = "SCANNER_FRAME"
}

public enum ChameleonErrorCode: String, Sendable {
    case errCameraPermission = "ERR_CAMERA_PERMISSION"
    case errCameraUnavailable = "ERR_CAMERA_UNAVAILABLE"
    case errPreflightLuma = "ERR_PREFLIGHT_LUMA"
    case errPreflightGlare = "ERR_PREFLIGHT_GLARE"
    case errPreflightBlur = "ERR_PREFLIGHT_BLUR"
    case errTransport = "ERR_TRANSPORT"
    case errSessionExpired = "ERR_SESSION_EXPIRED"
    case errNfcUnavailable = "ERR_NFC_UNAVAILABLE"
    case errNfcReadFailed = "ERR_NFC_READ_FAILED"
}

public struct ChameleonError: Error, Sendable {
    public let code: ChameleonErrorCode
    public let message: String
    public let recoverable: Bool

    public init(code: ChameleonErrorCode, message: String, recoverable: Bool) {
        self.code = code
        self.message = message
        self.recoverable = recoverable
    }
}

public struct CountryOption: Identifiable, Sendable {
    public var id: String { code }
    public let code: String
    public let name: String
    public let docKinds: [DocumentKind]?

    public init(code: String, name: String, docKinds: [DocumentKind]? = nil) {
        self.code = code
        self.name = name
        self.docKinds = docKinds
    }
}

public struct QualityMetrics: Sendable {
    public let lumaMean: Float
    public let glareRatio: Float
    public let laplacianVariance: Float
}

public enum FrameMimeType: String, Sendable {
    case webp = "image/webp"
    case jpeg = "image/jpeg"
}

/// Volatile buffer loaned strictly during transport execution; scrubbed via C `memset_s` upon return.
public struct FramePayload {
    public let bytes: UnsafeRawBufferPointer
    public let mimeType: FrameMimeType
    public let kind: FrameKind
    public let sequence: Int
    public let timestampMs: Int64
    public let nonce: String
    public let qualityMetrics: QualityMetrics
}

public struct SignedReceipt: Sendable {
    public let sessionId: String
    public let jwsToken: String
    public let completedAt: String
}

public struct ChameleonLabels: Sendable {
    public var consentTitle: String?
    public var consentBody: String?
    public var consentAcceptCta: String?
    public var countrySelectTitle: String?
    public var documentCaptureHint: String?
    public var nfcScanPrompt: String?
    public var livenessPrompt: String?
    public var processingLabel: String?

    public init(
        consentTitle: String? = nil,
        consentBody: String? = nil,
        consentAcceptCta: String? = nil,
        countrySelectTitle: String? = nil,
        documentCaptureHint: String? = nil,
        nfcScanPrompt: String? = nil,
        livenessPrompt: String? = nil,
        processingLabel: String? = nil
    ) {
        self.consentTitle = consentTitle
        self.consentBody = consentBody
        self.consentAcceptCta = consentAcceptCta
        self.countrySelectTitle = countrySelectTitle
        self.documentCaptureHint = documentCaptureHint
        self.nfcScanPrompt = nfcScanPrompt
        self.livenessPrompt = livenessPrompt
        self.processingLabel = processingLabel
    }
}

public enum CaptureStep: String, Sendable {
    case consent, country, document, poa, businessRegistry
    case nfc, liveness, selfie, ageEstimation, portraitVideo, scanner, done
}
