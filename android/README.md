# com.mtchship.chameleon:kyc-ui (Android)

Open-source, unbranded, RAM-only KYC capture SDK for Android (`Kotlin`, `Jetpack Compose`, `CameraX`, `Android NFC`).

## Features

* **Adaptive Material 3 UI:** Automatically inherits your host app's `MaterialTheme.colorScheme` and `MaterialTheme.typography` (Light/Dark mode and RTL layouts).
* **Volatile Memory Pipeline:** Evaluates CameraX `YUV_420_888` luma planes directly in native `ByteBuffer` memory (`< 3ms`) without allocating `Bitmap` copies, and overwrites payload buffers via `Arrays.fill(bytes, 0)` in `finally` blocks.
* **Compile-Time Transport Safety:** Enforces either managed first-party `CNAME` ingestion or a custom `suspend` transport handler via a sealed interface.

## Installation

Add the dependency to your module's `build.gradle.kts` (`minSdk = 24`, Jetpack Compose enabled):

```kotlin
dependencies {
    implementation("com.mtchship.chameleon:kyc-ui:1.0.0")
}
```

### Permissions & Hardware Manifest

The library AAR merges the required permissions automatically:

* `android.permission.CAMERA` (runtime permission prompt is handled inside `ChameleonKycHost`).
* `android.permission.NFC` and `<uses-feature android:name="android.hardware.nfc" android:required="false" />` (devices without NFC hardware remain eligible on Google Play and automatically skip optional chip steps).

## Quickstart

### Managed CNAME Ingestion

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

### Custom Transport Handler

```kotlin
ChameleonKycHost(
    preset = VerificationPreset.FINTECH_KYC,
    transport = ChameleonTransport.Custom { payload ->
        // Stream payload.bytes immediately; buffer is overwritten with zeros upon return
        yourBackendClient.uploadFrame(payload.bytes, payload.metadata)
    },
    onComplete = { receipt -> /* ... */ },
    onClose = { }
)
```

## Licence

Copyright © 2026 **Lovorise Ltd**. Licensed under the [Apache License, Version 2.0](../LICENSE).

