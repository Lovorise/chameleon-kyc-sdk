import AVFoundation
import SwiftUI

public struct ChameleonKycView: View {

    private let preset: VerificationPreset
    private let transport: ChameleonTransport
    private let countries: [CountryOption]
    private let enableNFC: Bool
    private let labels: ChameleonLabels?
    private let onStepChanged: ((CaptureStep) -> Void)?
    private let onComplete: (SignedReceipt) -> Void
    private let onError: ((ChameleonError) -> Void)?
    private let onClose: () -> Void

    @State private var sessionId = UUID().uuidString
    @State private var stepIndex = 0
    @State private var capturedInStep = 0
    @State private var statusHint: String?
    @State private var isUploading = false
    @State private var isDestroyed = false
    @State private var nfcReader: CoreNfcPassportReader?

    private var steps: [CaptureStep] {
        PresetStateMachine.resolveSteps(
            preset: preset,
            nfcAvailable: enableNFC && CoreNfcPassportReader.isAvailable
        )
    }

    private var currentStep: CaptureStep {
        steps.indices.contains(stepIndex) ? steps[stepIndex] : .done
    }

    public init(
        preset: VerificationPreset,
        transport: ChameleonTransport,
        locale: String? = nil,
        detectedIpCountry: String? = nil,
        countries: [CountryOption]? = nil,
        docKinds: [DocumentKind]? = nil,
        enableNFC: Bool = true,
        labels: ChameleonLabels? = nil,
        onStepChanged: ((CaptureStep) -> Void)? = nil,
        onComplete: @escaping (SignedReceipt) -> Void,
        onError: ((ChameleonError) -> Void)? = nil,
        onClose: @escaping () -> Void
    ) {
        self.preset = preset
        self.transport = transport
        self.countries = countries ?? [
            CountryOption(code: "CH", name: "Switzerland"),
            CountryOption(code: "DE", name: "Germany"),
            CountryOption(code: "AT", name: "Austria"),
            CountryOption(code: "GB", name: "United Kingdom"),
            CountryOption(code: "US", name: "United States")
        ]
        self.enableNFC = enableNFC
        self.labels = labels
        self.onStepChanged = onStepChanged
        self.onComplete = onComplete
        self.onError = onError
        self.onClose = onClose
    }

    public var body: some View {
        VStack(spacing: 0) {
            HStack {
                ProgressView(value: Double(stepIndex + 1), total: Double(max(1, steps.count)))
                    .tint(.accentColor)
                Button("Close") {
                    cleanup()
                    onClose()
                }
                .padding(.leading, 12)
            }
            .padding()

            stepContent
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, 20)
        }
        .background(Color(.systemBackground))
        .onChange(of: currentStep) { newStep in
            handleStepTransition(newStep)
        }
        .onDisappear { cleanup() }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch currentStep {
        case .consent:
            VStack(alignment: .leading, spacing: 16) {
                Text(labels?.consentTitle ?? "Identity Verification")
                    .font(.title2.bold())
                Text(labels?.consentBody ?? "Your camera images and document data are processed in volatile memory and transmitted directly to the service provider.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                Spacer()
                Button(labels?.consentAcceptCta ?? "Continue") { stepIndex += 1 }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                Button("Cancel") {
                    cleanup()
                    onClose()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
            }
            .padding(.bottom, 24)

        case .country:
            VStack(alignment: .leading, spacing: 12) {
                Text(labels?.countrySelectTitle ?? "Select Issuing Country")
                    .font(.title2.bold())
                List(countries) { country in
                    Button {
                        stepIndex += 1
                    } label: {
                        HStack {
                            Text(country.name)
                            Spacer()
                            Text(country.code).foregroundStyle(.secondary)
                        }
                    }
                }
                .listStyle(.plain)
            }

        case .nfc:
            VStack(spacing: 16) {
                ProgressView()
                Text(labels?.nfcScanPrompt ?? "Hold Document to Top of iPhone")
                    .font(.headline)
                Text(statusHint ?? "Reading ICAO 9303 chip in volatile memory")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button("Skip chip scan") {
                    nfcReader?.stop()
                    stepIndex += 1
                }
                .padding(.top, 16)
            }

        case .done:
            VStack(spacing: 16) {
                ProgressView()
                Text(labels?.processingLabel ?? "Finalizing Verification")
                    .font(.headline)
            }

        default:
            VStack(spacing: 16) {
                CameraPreviewRepresentable(
                    useFrontCamera: [.liveness, .selfie, .ageEstimation, .portraitVideo].contains(currentStep),
                    frameKindProvider: {
                        guard !isUploading, !isDestroyed else { return nil }
                        return PresetStateMachine.frameKind(for: currentStep)
                    },
                    onAcceptedBuffer: { buffer, kind, seq, ts, nonce, metrics in
                        handleCapturedFrame(buffer: buffer, kind: kind, seq: seq, ts: ts, nonce: nonce, metrics: metrics)
                    },
                    onQualityWarning: { err in
                        statusHint = err.message
                        onError?(err)
                    }
                )
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.accentColor, lineWidth: 2))

                Text(statusHint ?? "Align within frame (\(capturedInStep) / \(PresetStateMachine.targetFrames(preset: preset, step: currentStep)))")
                    .font(.headline)
                    .padding(.bottom, 16)
            }
        }
    }

    private func handleStepTransition(_ step: CaptureStep) {
        capturedInStep = 0
        statusHint = nil
        onStepChanged?(step)

        if step == .nfc {
            let reader = CoreNfcPassportReader(
                onChipReady: { bytes, nonce in
                    handleCapturedFrame(
                        buffer: bytes,
                        kind: .documentFront,
                        seq: 900,
                        ts: Int64(Date().timeIntervalSince1970 * 1000),
                        nonce: nonce,
                        metrics: QualityMetrics(lumaMean: 128, glareRatio: 0, laplacianVariance: 1000)
                    )
                },
                onError: { err in
                    statusHint = err.message
                    onError?(err)
                }
            )
            nfcReader = reader
            reader.start(alertMessage: labels?.nfcScanPrompt ?? "Hold document to top of iPhone")
        } else if step == .done {
            Task {
                switch transport {
                case let .cname(endpoint, keyToken, headers):
                    do {
                        let receipt = try await CnameIngressClient.finalizeSession(
                            endpoint: endpoint, keyToken: keyToken, headers: headers, sessionId: sessionId
                        )
                        onComplete(receipt)
                    } catch let err as ChameleonError {
                        onError?(err)
                    } catch {
                        onError?(ChameleonError(code: .errTransport, message: error.localizedDescription, recoverable: true))
                    }
                case .custom:
                    onComplete(SignedReceipt(sessionId: sessionId, jwsToken: "", completedAt: ISO8601DateFormatter().string(from: Date())))
                }
            }
        }
    }

    private func handleCapturedFrame(
        buffer: [UInt8],
        kind: FrameKind,
        seq: Int,
        ts: Int64,
        nonce: String,
        metrics: QualityMetrics
    ) {
        isUploading = true
        var mutableBuffer = buffer
        Task {
            defer { isUploading = false }
            do {
                try await dispatchAndScrub(
                    buffer: &mutableBuffer,
                    mimeType: .jpeg,
                    kind: kind,
                    sequence: seq,
                    timestampMs: ts,
                    nonce: nonce,
                    metrics: metrics
                ) { payload in
                    switch transport {
                    case let .cname(endpoint, keyToken, headers):
                        try await CnameIngressClient.uploadFrame(
                            endpoint: endpoint, keyToken: keyToken, headers: headers,
                            sessionId: sessionId, preset: preset, payload: payload
                        )
                    case let .custom(handler):
                        try await handler(payload)
                    }
                }
                let target = max(1, PresetStateMachine.targetFrames(preset: preset, step: currentStep))
                capturedInStep += 1
                if capturedInStep >= target {
                    stepIndex += 1
                }
