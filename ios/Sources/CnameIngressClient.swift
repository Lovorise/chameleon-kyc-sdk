import Foundation

internal enum CnameIngressClient {

    static func uploadFrame(
        endpoint: URL,
        keyToken: String,
        headers: [String: String],
        sessionId: String,
        preset: VerificationPreset,
        payload: FramePayload
    ) async throws {
        let url = endpoint.appendingPathComponent("v1/sessions/\(sessionId)/frames")
        var request = URLRequest(url: url, timeoutInterval: 15)
        request.httpMethod = "POST"
        request.setValue("Bearer \(keyToken)", forHTTPHeaderField: "Authorization")
        request.setValue(payload.mimeType.rawValue, forHTTPHeaderField: "Content-Type")
        request.setValue(preset.rawValue, forHTTPHeaderField: "X-Chameleon-Preset")
        request.setValue(payload.kind.rawValue, forHTTPHeaderField: "X-Chameleon-Frame-Kind")
        request.setValue("\(payload.sequence)", forHTTPHeaderField: "X-Chameleon-Sequence")
        request.setValue(payload.nonce, forHTTPHeaderField: "X-Chameleon-Nonce")
        request.setValue("\(payload.timestampMs)", forHTTPHeaderField: "X-Chameleon-Timestamp")
        for (k, v) in headers {
            request.setValue(v, forHTTPHeaderField: k)
        }

        // Ephemeral Data view without retaining beyond URLSession upload
        let bodyData = Data(bytes: payload.bytes.baseAddress!, count: payload.bytes.count)
        let (_, response) = try await URLSession.shared.upload(for: request, from: bodyData)

        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            throw ChameleonError(code: .errTransport, message: "Ingress rejected frame: HTTP \(code)", recoverable: true)
        }
    }

    static func finalizeSession(
        endpoint: URL,
        keyToken: String,
        headers: [String: String],
        sessionId: String
    ) async throws -> SignedReceipt {
        let url = endpoint.appendingPathComponent("v1/sessions/\(sessionId)/complete")
        var request = URLRequest(url: url, timeoutInterval: 20)
        request.httpMethod = "POST"
        request.setValue("Bearer \(keyToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        for (k, v) in headers {
            request.setValue(v, forHTTPHeaderField: k)
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            throw ChameleonError(code: .errTransport, message: "Finalization failed: HTTP \(code)", recoverable: true)
        }

        let obj = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
        guard let jws = obj["jwsToken"] as? String else {
            throw ChameleonError(code: .errTransport, message: "Missing jwsToken in receipt", recoverable: false)
        }
        return SignedReceipt(
            sessionId: (obj["sessionId"] as? String) ?? sessionId,
            jwsToken: jws,
            completedAt: (obj["completedAt"] as? String) ?? ISO8601DateFormatter().string(from: Date())
        )
    }
}
