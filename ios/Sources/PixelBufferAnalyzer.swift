import AVFoundation
import CoreImage
import Foundation
import Security
import UIKit

internal final class PixelBufferAnalyzer: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {

    private let ciContext = CIContext(options: [.useSoftwareRenderer: false])
    private let frameKindProvider: () -> FrameKind?
    private let onAcceptedBuffer: ([UInt8], FrameKind, Int, Int64, String, QualityMetrics) -> Void
    private let onQualityWarning: (ChameleonError) -> Void

    private var isBusy = false
    private var sequence = 0
    private var lastWarningTime: TimeInterval = 0

    init(
        frameKindProvider: @escaping () -> FrameKind?,
        onAcceptedBuffer: @escaping ([UInt8], FrameKind, Int, Int64, String, QualityMetrics) -> Void,
        onQualityWarning: @escaping (ChameleonError) -> Void
    ) {
        self.frameKindProvider = frameKindProvider
        self.onAcceptedBuffer = onAcceptedBuffer
        self.onQualityWarning = onQualityWarning
    }

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard !isBusy, let kind = frameKindProvider(),
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            return
        }

        isBusy = true
        defer { isBusy = false }

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        let metrics = evaluateYPlane(pixelBuffer)
        if let warning = validate(metrics) {
            let now = Date().timeIntervalSince1970
            if now - lastWarningTime > 1.2 {
                lastWarningTime = now
                DispatchQueue.main.async { self.onQualityWarning(warning) }
            }
            return
        }

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard var jpegData = ciContext.jpegRepresentation(
            of: ciImage,
            colorSpace: colorSpace,
            options: [kCGImageDestinationLossyCompressionQuality as CIImageRepresentationOption: 0.86]
        ) else { return }

        var byteArray = [UInt8](repeating: 0, count: jpegData.count)
        jpegData.copyBytes(to: &byteArray, count: jpegData.count)
        jpegData.withUnsafeMutableBytes { raw in
            if let base = raw.baseAddress { _ = memset_s(base, raw.count, 0, raw.count) }
        }

        sequence += 1
        let seq = sequence
        let ts = Int64(Date().timeIntervalSince1970 * 1000)
        let nonce = generateNonce()

        onAcceptedBuffer(byteArray, kind, seq, ts, nonce, metrics)
    }

    private func evaluateYPlane(_ pb: CVPixelBuffer) -> QualityMetrics {
        guard let baseAddress = CVPixelBufferGetBaseAddressOfPlane(pb, 0) else {
            return QualityMetrics(lumaMean: 0, glareRatio: 1, laplacianVariance: 0)
        }
        let width = CVPixelBufferGetWidthOfPlane(pb, 0)
        let height = CVPixelBufferGetHeightOfPlane(pb, 0)
        let stride = CVPixelBufferGetBytesPerRowOfPlane(pb, 0)
        let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

        let step = max(1, min(width, height) / 180)
        var sumLuma: Int64 = 0
        var glareCount: Int64 = 0
        var lapSum: Double = 0
        var samples: Int64 = 0

        for y in Swift.stride(from: step, to: height - step, by: step) {
            let row = y * stride
            let prev = (y - step) * stride
            let next = (y + step) * stride

            for x in Swift.stride(from: step, to: width - step, by: step) {
                let c = Int(ptr[row + x])
                sumLuma += Int64(c)
                if c >= 245 { glareCount += 1 }

                let lap = Int(ptr[prev + x]) + Int(ptr[next + x]) + Int(ptr[row + x - step]) + Int(ptr[row + x + step]) - (4 * c)
                lapSum += Double(lap * lap)
                samples += 1
            }
        }

        let count = max(1, samples)
        return QualityMetrics(
            lumaMean: Float(sumLuma) / Float(count),
            glareRatio: Float(glareCount) / Float(count),
            laplacianVariance: Float(lapSum / Double(count))
        )
    }

    private func validate(_ m: QualityMetrics) -> ChameleonError? {
        if m.lumaMean < 45 || m.lumaMean > 225 {
            return ChameleonError(code: .errPreflightLuma, message: "Adjust lighting conditions", recoverable: true)
        }
        if m.glareRatio > 0.08 {
            return ChameleonError(code: .errPreflightGlare, message: "Reduce surface glare", recoverable: true)
        }
        if m.laplacianVariance < 120 {
            return ChameleonError(code: .errPreflightBlur, message: "Hold steady to focus", recoverable: true)
        }
        return nil
    }

    private func generateNonce() -> String {
        var bytes = [UInt8](repeating: 0, count: 12)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return bytes.map { String(format: "%02x", $0) }.joined()
    }
}
