import CoreNFC
import Foundation
import Security

internal final class CoreNfcPassportReader: NSObject, NFCTagReaderSessionDelegate {

    private var session: NFCTagReaderSession?
    private let onChipReady: ([UInt8], String) -> Void
    private let onError: (ChameleonError) -> Void

    static var isAvailable: Bool {
        NFCTagReaderSession.readingAvailable
    }

    init(
        onChipReady: @escaping ([UInt8], String) -> Void,
        onError: @escaping (ChameleonError) -> Void
    ) {
        self.onChipReady = onChipReady
        self.onError = onError
    }

    func start(alertMessage: String) {
        guard NFCTagReaderSession.readingAvailable else {
            onError(ChameleonError(code: .errNfcUnavailable, message: "NFC not available", recoverable: true))
            return
        }
        session = NFCTagReaderSession(pollingOption: [.iso14443], delegate: self)
        session?.alertMessage = alertMessage
        session?.begin()
    }

    func stop() {
        session?.invalidate()
        session = nil
    }

    func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {}

    func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
        if let nfcErr = error as? NFCReaderError, nfcErr.code == .readerSessionInvalidationErrorUserCanceled {
            return
        }
        onError(ChameleonError(code: .errNfcReadFailed, message: error.localizedDescription, recoverable: true))
    }

    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        guard let first = tags.first, case let .iso7816(tag) = first else { return }

        session.connect(to: first) { [weak self] error in
            guard let self else { return }
            if let error {
                session.invalidate(errorMessage: error.localizedDescription)
                return
            }

            // Select eMRTD AID (A0 00 00 02 47 10 01)
            let selectAPDU = NFCISO7816APDU(
                instructionClass: 0x00,
                instructionCode: 0xA4,
                p1Parameter: 0x04,
                p2Parameter: 0x0C,
                data: Data([0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01]),
                expectedResponseLength: -1
            )

            tag.sendCommand(apdu: selectAPDU) { data, sw1, sw2, cmdError in
                if let cmdError {
                    session.invalidate(errorMessage: cmdError.localizedDescription)
                    return
                }
                guard sw1 == 0x90, sw2 == 0x00 else {
                    session.invalidate(errorMessage: "eMRTD chip application not found")
                    return
                }

                var chipBytes = [UInt8](data)
                var nonceBytes = [UInt8](repeating: 0, count: 12)
                _ = SecRandomCopyBytes(kSecRandomDefault, nonceBytes.count, &nonceBytes)
                let nonce = nonceBytes.map { String(format: "%02x", $0) }.joined()

                session.alertMessage = "Chip verified"
                session.invalidate()
                self.onChipReady(chipBytes, nonce)
                _ = chipBytes.withUnsafeMutableBytes { raw in
                    if let base = raw.baseAddress { memset_s(base, raw.count, 0, raw.count) }
                }
            }
        }
    }
}
