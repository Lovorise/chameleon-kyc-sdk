import Foundation

/// Compile-time XOR constraint: callers provide either `.cname` or `.custom`.
public enum ChameleonTransport {
    case cname(endpoint: URL, keyToken: String, headers: [String: String] = [:])
    case custom((FramePayload) async throws -> Void)
}

@inline(__always)
internal func dispatchAndScrub(
    buffer: inout [UInt8],
    mimeType: FrameMimeType,
    kind: FrameKind,
    sequence: Int,
    timestampMs: Int64,
    nonce: String,
    metrics: QualityMetrics,
    handler: (FramePayload) async throws -> Void
) async throws {
    let count = buffer.count
    defer {
        if count > 0 {
            buffer.withUnsafeMutableBytes { rawBuf in
                if let base = rawBuf.baseAddress {
                    _ = memset_s(base, rawBuf.count, 0, rawBuf.count)
                }
            }
        }
    }

    // Extract raw pointer scope safely without CoW copies
    var capturedError: Error?
    var didExecute = false

    // Bridge async execution with guaranteed pointer lifetime
    let snapshotCount = buffer.count
    let rawPtr = UnsafeMutableRawPointer.allocate(byteCount: max(1, snapshotCount), alignment: 16)
    defer {
        _ = memset_s(rawPtr, max(1, snapshotCount), 0, max(1, snapshotCount))
        rawPtr.deallocate()
    }

    if snapshotCount > 0 {
        buffer.withUnsafeBytes { src in
            if let srcBase = src.baseAddress {
                rawPtr.copyMemory(from: srcBase, byteCount: snapshotCount)
            }
        }
    }

    let payload = FramePayload(
        bytes: UnsafeRawBufferPointer(start: rawPtr, count: snapshotCount),
        mimeType: mimeType,
        kind: kind,
        sequence: sequence,
        timestampMs: timestampMs,
        nonce: nonce,
        qualityMetrics: metrics
    )
    try await handler(payload)
}
