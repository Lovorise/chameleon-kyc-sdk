# MTCHSHIP — Chameleon KYC SDK

Open-source, unbranded, adaptive UI capture modules for **MTCHSHIP Visual Clerk** (`Web TypeScript`, `Android Kotlin`, `iOS Swift`).

Chameleon handles the client-side capture lifecycle for identity verification, liveness detection, NFC chip reading, and document scanning across global deployments. It renders without vendor chrome, inherits the host application's theme and locale tokens at runtime, and processes camera and NFC frames strictly in volatile memory.

Connect the SDK via your first-party `CNAME` and `Key Token` to route payloads through the **MTCHSHIP Visual Clerk diskless data plane**—executing your flat-rate verification pipeline in memory and streaming media directly into your own object storage—while retaining full ownership of your control plane and customer database.

---

## Architecture & Guarantees

### Control Plane, Storage & Unlimited Add-on Model
Chameleon and MTCHSHIP Visual Clerk operate strictly as a **diskless, stateless data plane**:

* **Customer-Owned Control Plane:** We operate no hosted control plane or customer dashboard. Your backend remains the sole control plane—owning user profiles, session orchestration, policy rules, and KYC decision state. Open-source helpers (`JWS` signature verifiers and an unbranded `<chameleon-review-card>` web component) are included so you can integrate verification signals directly into your existing backoffice.
* **Flat-Rate Unlimited Add-ons via CNAME:** All supported verification capabilities—**Document OCR/MRZ**, **Presentation Attack Detection (Liveness PAD)**, **1:1 Face Comparison**, **ICAO 9303 NFC Chip Verification**, **Biometric Age Estimation**, **Proof-of-Address (PoA) Extraction**, **KYB Registry Lookups**, and **Stateless AML / PEP / Sanctions Screening**—are unlocked at the `Key Token` / `CNAME` level under an unlimited flat-rate subscription.
* **Bring Your Own Object Storage (BYOS):** Visual Clerk provisions no persistent disk storage. Processed media files are streamed directly from volatile memory into your designated object storage bucket (AWS S3, GCS, Azure Blob, or any S3-compatible endpoint in your chosen jurisdiction) via scoped write-only credentials or pre-signed URLs.
* **Zero Data Custody:** Neither the client SDK nor Visual Clerk retains media files, biometric templates, AML screening inputs, or customer records after request completion.

### Hardware-Accelerated In-Memory Processing Pipeline
When payloads hit your `CNAME` endpoint, Visual Clerk executes all enabled verification stages inside an isolated, volatile memory pipeline before returning a cryptographically signed `JWS` signal receipt to your backend:

* **Document Forensics & Extraction:** Multi-script OCR, MRZ checksum validation, PDF417/QR barcode decoding, hologram/font anomaly checks, and Proof-of-Address parsing.
* **Biometric Liveness & Matching:** Passive and multi-frame active liveness detection (ISO/IEC 30107-3 PAD), screen-replay/deepfake injection analysis, 1:1 facial similarity scoring, and age-bracket estimation.
* **NFC / ePassport Verification:** Validates passive and active cryptographic signatures (`SOD`, `DG1`, `DG2` / CSCA certificates) from ICAO 9303 compliant passports and national ID cards.
* **Stateless AML / PEP / Sanctions Screening:** Extracted identity attributes (name, DOB, nationality) are screened in-memory against global watchlists (**UK HMT/OFSI, EU, UN, US OFAC, SECO**) within the same transient request lifecycle.
* **Corporate Entity Verification (`KYB_ENTITY`):** Cross-references business registration documents against live corporate registries and UBO structures in real time.

### Memory Lifecycle & Buffer Ownership
Camera and NFC payloads are held in pre-allocated volatile buffers and never written to persistent client storage (`localStorage`, IndexedDB, `MediaStore`, `NSTemporaryDirectory`).

* **Buffer Ownership:** `payload.bytes` is loaned strictly for the execution scope of `onFrameReady`. Retaining a reference to the underlying buffer after the callback settles results in reading zeroed memory.
* **Web (`Uint8Array`):** Frames are extracted from `VideoFrame` / `ImageBitmap` into a pooled `ArrayBuffer`. Because JS engines do not guarantee deterministic GC compaction, avoiding intermediate allocations minimises heap copies before `Uint8Array.fill(0)` runs in the `finally` block.
* **Android (`ByteArray`):** Reads directly from CameraX `ImageProxy` planes and `IsoDep` NFC transceivers. Buffers are overwritten via `Arrays.fill(bytes, 0)` immediately upon callback return and before `imageProxy.close()` is called.
* **iOS (`Data`):** Operates on locked `CVPixelBuffer` base addresses and `CoreNFC` tag sessions. Transient `Data` wrappers are scrubbed via C `memset_s` inside a `defer` block to prevent Copy-on-Write (CoW) retention on the heap.

### On-Device Pre-Flight Gate
Before a frame leaves the device, an in-memory evaluation pass checks luma distribution (underexposure/clipping), specular highlights (flash glare), and edge variance via a Laplacian kernel on a downsampled 360p luma plane. Frames failing thresholds are dropped locally before network serialisation.

* **Latency budget:** `< 3ms` per frame on mid-tier ARM64 hardware (e.g., Apple A14, Snapdragon 7 Gen 1).
* **Memory ceiling:** `< 25 MB` peak transient allocation during a 7-frame liveness burst.

---

## Repository Structure

| Directory | Artifact | Stack & Role |
| :--- | :--- | :--- |
| [`web/`](./web) | `@mtchship/chameleon-kyc-ui` | TypeScript ESM + Shadow DOM `<chameleon-kyc>`, WebRTC, 0 runtime deps |
| [`android/`](./android) | `com.mtchship.chameleon:kyc-ui` | Kotlin, Jetpack Compose (Material 3), CameraX, Android NFC |
| [`ios/`](./ios) | `ChameleonKYCUI` | Swift, SwiftUI, AVFoundation, CoreNFC, `PrivacyInfo.xcprivacy` |
| [`server-helpers/`](./server-helpers) | `@mtchship/chameleon-admin` | Zero-dep `JWS` receipt verifier & unbranded `<chameleon-review-card>` for your own control plane |
| [`spec/`](./spec) | OpenAPI 3.1 & State Machine | Wire protocol specification for `CNAME` ingestion and signed `JWS` payloads |

---

## Verification Flows (Presets)

| Preset | Target Domain | State Machine Sequence |
| :--- | :--- | :--- |
| `GOV_EID` | Public sector, e-Notary, QES | `Consent` → `Country` → `Doc + Optional NFC` → `7-Frame Liveness` → `Done` |
| `FINTECH_KYC` | Banking, Neobanks, Crypto, Insurance | `Consent` → `Country` → `Doc + PoA + Optional NFC` → `5-Frame Liveness` → `Done` |
| `KYB_ENTITY` | Corporate onboarding, UBO checks | `Consent` → `Country` → `ID + Business Registry` → `5-Frame Liveness` → `Done` |
| `COMMERCE_TRUST` | High-value checkout, rentals | `Consent` → `Doc (ID, Passport, DL)` → `3-Frame Selfie` → `Done` |
| `WORKFORCE_SCREENING` | HR onboarding, contractor vetting | `Consent` → `Country` → `Doc (ID, Residence Permit)` → `5-Frame Liveness` → `Done` |
| `HEALTH_PATIENT` | Telehealth, clinical trials | `Consent` → `Country` → `Doc (ID + Insurance Card)` → `5-Frame Liveness` → `Done` |
| `SOCIAL_BADGE` | Creator & community platforms | `Consent` → `Country` → `Doc (ID)` → `Portrait Video + Note` → `Done` |
| `AGE_GATE` | Age-restricted retail, gaming, online safety | `Consent` → `Country` → `3-Frame Age Estimation` → `Done` |
| `STEP_UP_AUTH` | Wire transfers, account recovery | `3-Frame Liveness` → `Done` |
| `SCANNER_ONLY` | Ticketing, transit gates, boarding | `Rear Camera (QR, Passport, ID)` → `Done` |

---

## Licence

Copyright © 2026 **Lovorise Ltd** (Registered in England & Wales). Licensed under the [Apache License, Version 2.0](./LICENSE). See [NOTICE](./NOTICE).
