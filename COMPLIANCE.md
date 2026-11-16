# Global Compliance & Data Protection Architecture (`DPIA` / `PIA` Reference)

This document provides technical architecture statements for Data Protection Officers (DPOs), Privacy Counsel, Information Security teams, and regulatory reviewers evaluating **MTCHSHIP Chameleon KYC SDK** and the **MTCHSHIP Visual Clerk** data plane across global jurisdictions.

---

## 1. Data Custody & Processing Roles

| Layer | Component | Storage Footprint | Operational Role |
| :--- | :--- | :--- | :--- |
| **End-User Device** | Chameleon SDK (`Web`, `Android`, `iOS`) | **Volatile RAM only** (`fill(0)` / `Arrays.fill` / `memset_s`) | Stateless capture & `<3ms` image quality gate |
| **Inference Layer** | Visual Clerk (`CNAME` Ingress) | **Diskless / Zero Retention** | Transient in-memory verification (OCR, PAD, NFC, AML) |
| **Storage & Control** | Customer Infrastructure (`BYOS` + Backend) | **Customer-owned Object Storage & DB** | Sole Data Controller / Business & Control Plane |

* **No Data at Rest on MTCHSHIP Infrastructure:** Visual Clerk provisions no persistent disk volumes for verification payloads. Media files are streamed directly from volatile memory into the customer's designated object storage bucket (via scoped write-only credentials or short-lived pre-signed URLs).
* **No On-Device Biometric Templates:** The open-source client SDK evaluates raw pixel planes for brightness, glare, and sharpness only. It never extracts, stores, or caches biometric identifiers or feature vectors in `localStorage`, IndexedDB, `MediaStore`, or `NSTemporaryDirectory`.

---

## 2. Global Regulatory & Framework Alignment

### Privacy, Biometrics & Data Residency
* **Europe & UK (`UK GDPR / DPA 2018`, `EU GDPR Art. 9`, `Swiss FADP / DSG`):** Supports data minimisation and storage limitation by eliminating vendor-hosted storage of special-category biometric data. Customers retain exclusive control over regional bucket residency, KMS encryption keys, and retention schedules without cross-border transfer to third-party multi-tenant databases.
* **United States (`CCPA/CPRA`, `Illinois BIPA`, `Texas CUBI`, `Washington My Health My Data`):** Because Chameleon extracts no biometric templates on the client device and Visual Clerk operates strictly in volatile memory without retaining facial geometry or scans, customers maintain direct control over statutory biometric consent, retention, and deletion schedules inside their own infrastructure.
* **Asia-Pacific (`Singapore PDPA`, `Australia Privacy Act / APPs`, `Japan APPI`, `South Korea PIPA`):** Regional `CNAME` edge routing paired with customer-owned regional object storage supports strict cross-border data transfer restrictions and local data residency mandates.
* **Middle East & Latin America (`UAE PDPL`, `DIFC / ADGM Data Protection`, `Saudi PDPL`, `Brazil LGPD`):** Enables in-country storage of identity artefacts within the customer's own cloud tenancy while using Visual Clerk purely as a transient compute layer.

### Identity Assurance, Liveness & Financial Crime (AML)
* **Global Identity & Liveness Standards (`NIST SP 800-63A`, `ISO/IEC 30107-3`, `eIDAS`, `UK DIATF / GPG 45`):** Multi-frame liveness presets (`GOV_EID`, `FINTECH_KYC`) and ICAO 9303 NFC chip verification supply structured confidence signals (`JWS`) to support customer-defined Identity Assurance Levels (IAL2 / IAL3) and Presentation Attack Detection (PAD) requirements.
* **Global Age Assurance (`UK Online Safety Act / Ofcom`, `US COPPA / State Age Laws`, `EU DSA`):** The `AGE_GATE` preset evaluates ephemeral facial frames in memory without requiring government ID uploads or retaining facial imagery.
* **FATF-Aligned Stateless AML / PEP / Sanctions Screening:** Identity attributes extracted in memory are screened transiently against global watchlists (**UN Consolidated List, US OFAC, UK OFSI, EU Sanctions, CH SECO, MAS, DFAT**) within the same request lifecycle; only the cryptographically signed `JWS` outcome receipt is returned to the customer's control plane.

---

## 3. Auditability & Supply-Chain Transparency

* **100% Open-Source Client (`Apache 2.0`):** Customer AppSec teams can inspect, audit, and deterministically build the complete client capture codebase (`web/`, `android/`, `ios/`) with zero obfuscated binaries or hidden telemetry endpoints.
* **Platform Privacy Declarations:** The iOS package ships with `PrivacyInfo.xcprivacy` declaring zero tracking (`NSPrivacyTracking = false`) and zero persistent data collection; Android and Web modules execute with zero third-party analytics SDKs.
