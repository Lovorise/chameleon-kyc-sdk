# MTCHSHIP — Chameleon KYC SDK

Open-source, unbranded, adaptive UI capture modules for **MTCHSHIP Visual Clerk** (`Web TypeScript`, `Android Kotlin`, `iOS Swift`).

Chameleon handles the client-side capture lifecycle for identity verification, liveness detection, NFC chip reading, and document scanning across global deployments. It renders without vendor chrome, inherits the host application's theme and locale tokens at runtime, and processes camera and NFC payloads strictly in volatile memory.

Connect the SDK via your first-party `CNAME` and `Key Token` to route payloads through the **MTCHSHIP Visual Clerk diskless data plane**—executing your flat-rate verification pipeline in memory and streaming media directly into your own object storage—while retaining full ownership of your control plane and customer database.

---

## Architecture & Guarantees

### Control Plane, Storage & Unlimited Add-on Model
Chameleon and MTCHSHIP Visual Clerk operate strictly as a **diskless, stateless data plane**:

* **Customer-Owned Control Plane:** We operate no hosted control plane or customer dashboard. Your backend remains the sole control plane—owning user profiles, session orchestration, policy rules, and KYC decision state. Zero-dependency `JWS` signature verifiers (`server-helpers/`) and an unbranded `<chameleon-review-card>` web component (`@mtchship/chameleon-kyc-ui/admin`) are included so you can integrate verification signals directly into your backoffice.
* **Flat-Rate Unlimited Add-ons via CNAME:** All supported verification capabilities—**Document OCR/MRZ**, **Presentation Attack Detection (Liveness PAD)**, **1:1 Face Comparison**, **ICAO 9303 NFC Chip Verification**, **Biometric Age Estimation**, **Proof-of-Address (PoA) Extraction**, **KYB Registry Lookups**, and **Stateless AML / PEP / Sanctions Screening**—are unlocked at the `Key Token` / `CNAME` level under an unlimited flat-rate subscription.
* **Bring Your Own Object Storage (BYOS):** Visual Clerk provisions no persistent disk storage. Processed media files are streamed directly from volatile memory into your designated S3-compatible or cloud object storage bucket in your chosen jurisdiction via scoped write-only credentials or pre-signed URLs.
* **Zero Data Custody:** Neither the client SDK nor Visual Clerk retains media files, biometric templates, AML screening inputs, or customer records after request completion. See [`COMPLIANCE.md`](./COMPLIANCE.md) for DPIA architectural statements.

### Memory Lifecycle & Buffer Ownership
* **Buffer Ownership:** `payload.bytes` is loaned strictly for the execution scope of the transport callback. Retaining a reference after completion results in reading zeroed memory.
* **Web (`Uint8Array`):** Pre-allocated `ArrayBuffer` pool zeroed via `Uint8Array.fill(0)` in `finally` blocks.
* **Android (`ByteArray`):** CameraX `ImageProxy` and `IsoDep` NFC buffers overwritten via `Arrays.fill(bytes, 0)` immediately upon handoff return.
* **iOS (`Data`):** Locked `CVPixelBuffer` and `CoreNFC` buffers scrubbed via C `memset_s` inside `defer` blocks to prevent Copy-on-Write retention.

### On-Device Pre-Flight Gate & Stateless Compliance
* **Pre-Flight Quality Gate:** Evaluates luma distribution, specular glare, and Laplacian edge sharpness on a downsampled 360p plane in volatile RAM (`< 3ms` on mid-tier ARM64 hardware, `< 25 MB` peak transient memory) before network serialisation.
* **Global Compliance Alignment:** The client SDK extracts no biometric templates on device. Combined with diskless BYOS forwarding, this supports data-minimisation obligations across **UK GDPR / DPA 2018**, **EU GDPR (Art. 9)**, **Swiss FADP**, and **CCPA/CPRA**, while multi-frame liveness and NFC presets supply pipelines aligned with **ISO/IEC 30107-3**, **UK DIATF (GPG 45)**, **eIDAS**, and **NIST SP 800-63A**.

---

## Repository Structure

| Directory | Artifact | Stack & Role |
| :--- | :--- | :--- |
| [`web/`](./web) | `@mtchship/chameleon-kyc-ui` | TypeScript ESM + Shadow DOM `<chameleon-kyc>` & `<chameleon-review-card>`, WebRTC, 0 runtime deps |
| [`android/`](./android) | `com.mtchship.chameleon:kyc-ui` | Kotlin, Jetpack Compose (Material 3), CameraX, Android NFC |
| [`ios/`](./ios) | `ChameleonKYCUI` | Swift, SwiftUI, AVFoundation, CoreNFC, `PrivacyInfo.xcprivacy` |
| [`server-helpers/`](./server-helpers) | `JWS` Receipt Verifiers | Zero-dependency signature verification helpers (`Node.js`, `Python`, `Go`) |
| [`COMPLIANCE.md`](./COMPLIANCE.md) | DPIA & Security Reference | Architectural statements for DPO, Legal, and Security reviews |
| [`SECURITY.md`](./SECURITY.md) | Threat Model & Disclosure | Memory hygiene boundaries and private vulnerability reporting |

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

## Quickstart

### Web (`TypeScript` / ESM)

```typescript
import { mountChameleonKYC, resolveAutoContext } from '@mtchship/chameleon-kyc-ui';
import '@mtchship/chameleon-kyc-ui/styles/chameleon.css';

const ctx = await resolveAutoContext({
  endpoint: 'https://yourdomain.com',
  keyToken: 'vc_pk_...'
});

const session = mountChameleonKYC(document.getElementById('kyc-root')!, {
  preset: 'FINTECH_KYC',
  locale: ctx.locale,
  detectedIpCountry: ctx.countryCode,
  transport: {
    endpoint: 'https://yourdomain.com',
    keyToken: 'vc_pk_...'
  },
  onComplete: (receipt) => {
    // Verify receipt.jwsToken on your backend control plane and read media from your own bucket
    session.destroy();
  },
  onClose: () => session.destroy()
});
```

### Android (`Jetpack Compose`)

```kotlin
ChameleonKycHost(
    preset = VerificationPreset.FINTECH_KYC,
    transport = ChameleonTransport.Cname(
        endpoint = "https://yourdomain.com",
        keyToken = "vc_pk_..."
    ),
    enableNfc = true,
    onComplete = { receipt -> navController.popBackStack() },
    onClose = { navController.popBackStack() }
)
```

### iOS (`SwiftUI`)

```swift
ChameleonKycView(
    preset: .fintechKYC,
    transport: .cname(
        endpoint: URL(string: "https://yourdomain.com")!,
        keyToken: "vc_pk_..."
    ),
    enableNFC: true,
    onComplete: { receipt in dismiss() },
    onClose: { dismiss() }
)
```

---

## Architectural Trade-offs vs. Hosted Drop-in SDKs

| Dimension | Chameleon + Visual Clerk | Standard Hosted SDKs |
| :--- | :--- | :--- |
| **Control & Custody** | Diskless data plane; customer retains control plane & PII | Vendor-hosted control plane storing customer PII & states |
| **Media Storage** | Direct RAM-to-Object-Storage forwarding (BYOS) | Stored on vendor-managed multi-tenant cloud buckets |
| **Verification Scope** | OCR, Liveness, Face Match, NFC, KYB & Stateless AML | Often split across separate products or dashboards |
| **Scaling & Pricing** | Flat-rate unlimited subscription via `Key Token` / `CNAME` | Per-check / per-module metered billing tiers |
| **Source & Audit** | Apache 2.0 source; inspectable memory and network paths | Obfuscated/minified proprietary binaries |
| **Client Footprint** | RAM-only capture with explicit buffer zeroing; 0 runtime deps | Temp files / IndexedDB caching; heavy bundles (`300 KB – 1.2 MB+`) |

---

## Licence

Copyright © 2026 **Lovorise Ltd** (Registered in England & Wales). Licensed under the [Apache License, Version 2.0](./LICENSE). See [NOTICE](./NOTICE).
