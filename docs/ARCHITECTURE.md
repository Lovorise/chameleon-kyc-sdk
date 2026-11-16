# System Architecture & Diskless Data Flow

Chameleon KYC and **MTCHSHIP Visual Clerk** separate capture, stateless biometric/document evaluation, and persistent storage into strict trust boundaries.

## End-to-End Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SDK as Chameleon Client SDK (RAM)
    participant Edge as Customer CNAME Ingress
    participant VC as Visual Clerk Data Plane (RAM)
    participant BYOS as Customer Object Storage (S3/R2/GCS)
    participant API as Customer Backend (Control Plane)

    User->>SDK: Capture Document / NFC / Liveness
    SDK->>SDK: Pre-Flight Quality Gate (<3ms, 360p Y-plane)
    SDK->>Edge: Stream Frame via HTTPS + Key Token
    SDK->>SDK: Scrub Buffer (fill(0) / memset_s)
    Edge->>VC: Forward Volatile Stream
    VC->>VC: OCR, Liveness PAD, Face Match, NFC, AML (In-Memory)
    VC-->>BYOS: Stream Media via Write-Only Credentials
    VC-->>Edge: Return Signed JWS Receipt (No PII)
    Edge-->>SDK: Deliver JWS Receipt
    SDK->>API: Forward receipt.jwsToken
    API->>API: Verify JWS Signature & Policy Rules
    API->>BYOS: Read Verified Media & Audit Artifacts
```

## Trust Boundaries

* **Client Device (Volatile RAM):** Captures frames and NFC APDUs in pre-allocated buffers. No files are written to flash storage, SQLite, or IndexedDB. Buffers are scrubbed immediately upon network handoff.
* **Customer CNAME Edge:** Routes traffic under the customer's first-party domain, preserving TLS termination control and regional ingress routing.
* **Visual Clerk Data Plane (Diskless):** Executes verification models strictly in volatile memory. No disk volumes, databases, or multi-tenant buckets are attached to worker nodes.
* **Customer BYOS Bucket:** Receives media streams directly from Visual Clerk memory using scoped write-only IAM credentials or short-lived pre-signed URLs.
* **Customer Control Plane:** Retains sole ownership of user records, session state, and KYC approval logic by verifying the cryptographic `JWS` receipt.
