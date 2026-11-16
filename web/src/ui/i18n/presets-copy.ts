import type { VerificationPreset } from '../../types.js';

export interface ChameleonLabels {
  introTitle: string;
  introSubtitle: string;
  countryTitle: string;
  docSelectTitle: string;
  docCaptureInstruction: string;
  livenessTitle: string;
  livenessInstruction: string;
  submitButton: string;
  retakeButton: string;
  continueButton: string;
}

const NEUTRAL_KYC_LABELS: ChameleonLabels = {
  introTitle: 'Identity verification',
  introSubtitle: 'This check confirms your identity securely without storing data on your device.',
  countryTitle: 'Select issuing country',
  docSelectTitle: 'Choose document type',
  docCaptureInstruction: 'Place your document fully inside the frame on a plain, well-lit surface.',
  livenessTitle: 'Biometric liveness check',
  livenessInstruction: 'Centre your face inside the frame and hold steady.',
  submitButton: 'Submit',
  retakeButton: 'Retake',
  continueButton: 'Continue'
};

export const PRESET_DEFAULT_LABELS: Record<VerificationPreset, ChameleonLabels> = {
  GOV_EID: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Official identity verification',
    introSubtitle: 'Required for electronic identification and document signing.'
  },
  FINTECH_KYC: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Verify your identity',
    introSubtitle: 'Required to comply with financial regulations and protect your account.'
  },
  KYB_ENTITY: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Business & representative verification',
    introSubtitle: 'Verify your personal ID and company registration documents.'
  },
  COMMERCE_TRUST: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Quick identity check',
    introSubtitle: 'Required to complete your booking or high-value transaction.'
  },
  WORKFORCE_SCREENING: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Credential & right-to-work check',
    introSubtitle: 'Verify your identity document or residence permit.'
  },
  HEALTH_PATIENT: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Patient identity verification',
    introSubtitle: 'Confirm your identity and coverage details before your consultation.'
  },
  SOCIAL_BADGE: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Verify your profile',
    introSubtitle: 'We will not publish this video on your profile. It is used solely to verify your identity.',
    livenessTitle: 'Video selfie with handwritten note',
    livenessInstruction: 'Hold a paper showing your full name, date of birth, location, and today’s date.'
  },
  AGE_GATE: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Age verification',
    introSubtitle: 'A quick camera check to confirm you meet the minimum age requirement.',
    livenessTitle: 'Age check',
    livenessInstruction: 'Look directly at the camera in good lighting.'
  },
  STEP_UP_AUTH: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Security confirmation',
    introSubtitle: 'Complete a quick liveness check to authorise this action.'
  },
  SCANNER_ONLY: {
    ...NEUTRAL_KYC_LABELS,
    introTitle: 'Scan pass or document',
    docCaptureInstruction: 'Align the QR code or document MRZ inside the frame.'
  }
};

export function resolvePresetLabels(
  preset: VerificationPreset,
  overrides?: Partial<ChameleonLabels>
): ChameleonLabels {
  return {
    ...PRESET_DEFAULT_LABELS[preset],
    ...overrides
  };
}
