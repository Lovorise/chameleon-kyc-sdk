# ChameleonKYCUI (iOS)

Open-source, unbranded, RAM-only KYC capture SDK for iOS (`Swift 5.9+`, `SwiftUI`, `AVFoundation`, `CoreNFC`).

## Features

* **Adaptive SwiftUI Chrome:** Inherits your host application's tint color, Dynamic Type, Light/Dark appearance, and RTL layout direction automatically.
* **Volatile Memory Pipeline:** Evaluates locked `CVPixelBuffer` Y-planes directly in RAM (`< 3ms`) and scrubs all frame and NFC buffers using C `memset_s` inside `defer` blocks to prevent Copy-on-Write retention.
* **Apple Privacy Manifest:** Includes `PrivacyInfo.xcprivacy` declaring zero tracking and zero persistent user data collection.
* **Compile-Time Transport Safety:** Enforces either managed first-party `CNAME` ingestion or a custom async frame handler via a Swift enum.

## Installation (Swift Package Manager)

Add the repository URL in Xcode (`File → Add Package Dependencies`) or in your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com", from: "1.0.0")
]
```

Add the required usage descriptions to your host app's `Info.plist`:

* `NSCameraUsageDescription`
* `NFCReaderUsageDescription` (optional; skipped automatically on devices without NFC hardware)

## Quickstart

### Managed CNAME Ingestion

```swift
import SwiftUI
import ChameleonKYCUI

struct VerificationScreen: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ChameleonKycView(
            preset: .fintechKYC,
            transport: .cname(
                endpoint: URL(string: "https://yourdomain.com")!,
                keyToken: "vc_pk_..."
            ),
            enableNFC: true,
            onComplete: { receipt in
                // Forward receipt.jwsToken to your backend control plane
                dismiss()
            },
            onClose: { dismiss() }
        )
    }
}
```

### Custom Transport Handler

```swift
ChameleonKycView(
    preset: .fintechKYC,
    transport: .custom { payload in
        // Read payload.bytes immediately; memory is zeroed via memset_s upon return
        try await yourBackendClient.upload(bytes: Data(payload.bytes), kind: payload.kind)
    },
    onComplete: { receipt in dismiss() },
    onClose: { dismiss() }
)
```

## Licence

Copyright © 2026 **Lovorise Ltd**. Licensed under the [Apache License, Version 2.0](../LICENSE).
