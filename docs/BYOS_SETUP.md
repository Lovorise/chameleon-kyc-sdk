# Bring Your Own Object Storage (BYOS) Setup

Visual Clerk provisions no persistent storage. Media artifacts are streamed directly from volatile memory into your designated S3-compatible or cloud object storage bucket.

## Security Principle: Write-Only Ingestion

To guarantee cryptographic and architectural non-custody, configure Visual Clerk with **write-only** permissions (`PutObject`). Visual Clerk never requires `GetObject`, `ListBucket`, or `DeleteObject` permissions.

---

## AWS S3 / Cloudflare R2 IAM Policy

Attach the following minimal IAM policy to the dedicated ingestion role or access key configured for your `CNAME` / `Key Token`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "VisualClerkWriteOnlyIngestion",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::your-kyc-evidence-bucket/sessions/*"
    }
  ]
}
```

### Recommended Bucket Hardening

* **Server-Side Encryption:** Enable SSE-KMS (with your customer-managed CMK) or SSE-S3 by default on the target bucket.
* **Block Public Access:** Keep all public access blocks enabled.
* **Object Lock (Optional):** Enable S3 Object Lock (Governance or Compliance mode) if your jurisdiction requires immutable KYC retention windows (e.g., 5–10 years under AMLD / GwG).
* **Lifecycle Rules:** Configure automated expiration rules aligned with your data retention schedule.

---

## Object Key Layout

Visual Clerk writes artifacts deterministically using the session identifier:

```text
sessions/
└── <sessionId>/
    ├── document_front_001.jpg
    ├── document_back_002.jpg
    ├── nfc_ef_sod_900.bin
    ├── liveness_003.jpg
    └── receipt.jws
```

Once `onComplete` fires on the client and your backend verifies `receipt.jwsToken`, your control plane can fetch these objects directly using its own internal read credentials.
