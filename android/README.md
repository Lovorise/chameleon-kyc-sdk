# com.mtchship.chameleon:kyc-ui (Android)

Open-source, unbranded, RAM-only KYC capture SDK for Android (`Kotlin`, `Jetpack Compose`, `CameraX`, `Android NFC`).

## Features
* **Adaptive Material 3 UI:** Automatically inherits your host app's `MaterialTheme.colorScheme` and `MaterialTheme.typography` (Light/Dark mode and RTL layouts).
* **Volatile Memory Pipeline:** Evaluates CameraX `YUV_420_888` luma planes directly in native `ByteBuffer` memory (`<3ms`) without allocating `Bitmap` copies, and overwrites payload buffers via `Arrays.fill(bytes, 0)` in `finally` blocks.
* **Compile-Time Transport Safety:** Enforces either managed first-party `CNAME` ingestion or a custom `suspend` transport handler via a sealed interface.

## Quickstart

```kotlin
ChameleonKycHost(
    preset = VerificationPreset.FINTECH_KYC,
    transport = ChameleonTransport.Cname(
        endpoint = "https://yourdomain.com",
        keyToken = "vc_pk_..."
    ),
    enableNfc = true,
    detectedIpCountry = "CH",
    onComplete = { receipt ->
        // Forward receipt.jwsToken to your backend control plane
    },
    onClose = { }
)
```

## Licence

Copyright © 2026 Lovorise Ltd. Licensed under the [Apache License, Version 2.0](../LICENSE).
