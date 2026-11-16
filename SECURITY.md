# Security Policy & Threat Model

## Architectural Boundaries
1. **Zero Client Persistence:** No frames, MRZ strings, or NFC datagroups are written to `localStorage`, IndexedDB, `MediaStore`, or `NSTemporaryDirectory`.
2. **Write-Only Client Keys:** Frontend `vc_pk_...` tokens can only initiate sessions and ingest frames over HTTPS `CNAME` endpoints bound to configured `Origin` headers. Verification outputs (`JWS`) are only readable server-to-server using `vc_sk_...` or public-key signature verification.
3. **Serial Backpressure:** Only one volatile frame buffer is active in flight at any time to prevent memory exhaustion and race conditions on buffer scrubbing.

## Reporting a Vulnerability
Report security issues privately to `security@mtchship.com`. Do not open public GitHub issues for unpatched vulnerabilities.
