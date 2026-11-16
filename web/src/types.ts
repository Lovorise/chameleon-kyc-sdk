export type VerificationPreset =
  | 'GOV_EID'
  | 'FINTECH_KYC'
  | 'KYB_ENTITY'
  | 'COMMERCE_TRUST'
  | 'WORKFORCE_SCREENING'
  | 'HEALTH_PATIENT'
  | 'SOCIAL_BADGE'
  | 'AGE_GATE'
  | 'STEP_UP_AUTH'
  | 'SCANNER_ONLY';

export type DocumentKind =
  | 'ID_CARD'
  | 'PASSPORT'
  | 'DRIVING_LICENSE'
  | 'RESIDENCE_PERMIT'
  | 'POA_DOCUMENT'
  | 'BUSINESS_REGISTRY'
  | 'INSURANCE_CARD'
  | 'PASS_QR';

export type FrameKind =
  | 'DOCUMENT_FRONT'
  | 'DOCUMENT_BACK'
  | 'POA_DOCUMENT'
  | 'BUSINESS_REGISTRY'
  | 'LIVENESS_FRAME'
  | 'AGE_FRAME'
  | 'PORTRAIT_VIDEO_FRAME'
  | 'SCANNER_FRAME';

export type ChameleonErrorCode =
  | 'ERR_CAMERA_PERMISSION'
  | 'ERR_CAMERA_UNAVAILABLE'
  | 'ERR_PREFLIGHT_LUMA'
  | 'ERR_PREFLIGHT_GLARE'
  | 'ERR_PREFLIGHT_BLUR'
  | 'ERR_TRANSPORT'
  | 'ERR_SESSION_EXPIRED';

export interface CountryOption {
  code: string;
  name: string;
  docKinds?: DocumentKind[];
}

export interface AutoContextResult {
  locale: string;
  dir: 'ltr' | 'rtl';
  colorScheme: 'light' | 'dark';
  countryCode?: string;
}

export interface FramePayload {
  readonly bytes: Uint8Array;
  readonly mimeType: 'image/webp' | 'image/jpeg';
  readonly kind: FrameKind;
  readonly sequence: number;
  readonly timestampMs: number;
  readonly nonce: string;
  readonly qualityMetrics: {
    lumaMean: number;
    glareRatio: number;
    laplacianVariance: number;
  };
}

export interface SignedReceipt {
  sessionId: string;
  jwsToken: string;
  completedAt: string;
}

export interface CnameTransportConfig {
  endpoint: string;
  keyToken: string;
  headers?: Record<string, string>;
}

interface ChameleonBaseConfig {
  preset: VerificationPreset;
  locale?: string;
  detectedIpCountry?: string;
  countries?: CountryOption[];
  docKinds?: DocumentKind[];
  onComplete?: (receipt: SignedReceipt) => void;
  onError?: (error: { code: ChameleonErrorCode; message: string; recoverable: boolean }) => void;
  onClose?: () => void;
}

export interface ManagedTransportConfig extends ChameleonBaseConfig {
  transport: CnameTransportConfig;
  onFrameReady?: never;
}

export interface CustomTransportConfig extends ChameleonBaseConfig {
  onFrameReady: (payload: FramePayload) => Promise<void>;
  transport?: never;
}

export type ChameleonConfig = ManagedTransportConfig | CustomTransportConfig;

export interface ChameleonSessionHandle {
  destroy: () => void;
}
