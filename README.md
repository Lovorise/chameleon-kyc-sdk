# MTCHSHIP — Chameleon KYC SDK

Open-source, unbranded, adaptive UI capture modules for **MTCHSHIP Visual Clerk** (`Web TypeScript`, `Android Kotlin`, `iOS Swift`).

Chameleon handles the client-side capture lifecycle for identity verification, liveness detection, NFC chip reading, and document scanning across global deployments. It renders without vendor chrome, inherits the host application's theme and locale tokens at runtime, and processes camera and NFC payloads strictly in volatile memory.

Connect the SDK via your first-party `CNAME` and `Key Token` to route payloads through the **MTCHSHIP Visual Clerk diskless data plane**—executing your flat-rate verification pipeline in memory and streaming media directly into your own object storage—while retaining full ownership of your control plane and customer database.

---

## Architecture & Guarantees

### Control Plane, Storage & Unlimited Add-on Model
Chameleon and MTCHSHIP Visual Clerk operate strictly as a **diskless, stateless data plane**:

* **Customer-Owned Control Plane:** We operate no hosted control plane or customer dashboard. Your backend remains the sole control plane—owning user profiles, session orchestration, policy rules, and KYC decision state. Zero-dependency `JWS` signature verifiers and an unbranded `<chameleon-review-card>` component are included under `server-helpers/` for direct integration into your backoffice.
* **Flat-Rate Unlimited Add-ons via CNAME:** All supported verification capabilities—**Document OCR/MRZ**, **Presentation Attack Detection (Liveness PAD)**, **1:1 Face Comparison**, **ICAO 9303 NFC Chip Verification**, **Biometric Age Estimation**, **Proof-of-Address (PoA) Extraction**, **KYB Registry Lookups**, and **Stateless AML / PEP / Sanctions Screening**—are unlocked at the `Key Token` / `CNAME` level under an unlimited flat-rate subscription.
* **Bring Your Own Object Storage (BYOS):** Visual Clerk provisions no persistent disk storage. Processed media files are streamed directly from volatile memory into your designated object storage bucket (AWS S3, GCS, Azure Blob, or any S3-compatible endpoint in your chosen jurisdiction) via scoped write-only credentials or pre-signed URLs.
* **Zero Data Custody:** Neither the client SDK nor Visual Clerk retains media files, biometric templates, AML screening inputs, or customer records after request completion.

### Memory Lifecycle & Buffer Ownership
* **Buffer Ownership:** `payload.bytes` is loaned strictly for the execution scope of the transport callback. Retaining a reference after completion results in reading zeroed memory.
* **Web (`Uint8Array`):** Pre-allocated `ArrayBuffer` pool zeroed via `Uint8Array.fill(0)` in `finally` blocks.
* **Android (`ByteArray`):** CameraX `ImageProxy` and `IsoDep` NFC buffers overwritten via `Arrays.fill(bytes, 0)` immediately upon handoff return.
* **iOS (`Data`):** Locked `CVPixelBuffer` and `CoreNFC` buffers scrubbed via C `memset_s` inside `defer` blocks to prevent Copy-on-Write retention.

---

## Repository Structure

| Directory | Artifact | Stack & Role |
| :--- | :--- | :--- |
| [`web/`](./web) | `@mtchship/chameleon-kyc-ui` | TypeScript ESM + Shadow DOM `<chameleon-kyc>`, WebRTC, 0 runtime deps |
| [`android/`](./android) | `com.mtchship.chameleon:kyc-ui` | Kotlin, Jetpack Compose (Material 3), CameraX, Android NFC |
| [`ios/`](./ios) | `ChameleonKYCUI` | Swift, SwiftUI, AVFoundation, CoreNFC, `PrivacyInfo.xcprivacy` |
| [`server-helpers/`](./server-helpers) | `@mtchship/chameleon-admin` | Zero-dep `JWS` receipt verifiers (`Node`, `Python`, `Go`) |
| [`spec/`](./spec) | OpenAPI 3.1 | Wire protocol specification for `CNAME` ingestion and signed `JWS` receipts |

---

## Licence

Copyright © 2026 **Lovorise Ltd** (Registered in England & Wales). Licensed under the [Apache License, Version 2.0](./LICENSE). See [NOTICE](./NOTICE).
