import { mountChameleonKYC } from './index.js';
import type { ChameleonSessionHandle, DocumentKind, SignedReceipt, VerificationPreset } from './types.js';

export class ChameleonKycElement extends HTMLElement {
  private handle: ChameleonSessionHandle | null = null;
  private readonly shadow: ShadowRoot;

  static get observedAttributes(): string[] {
    return ['preset', 'endpoint', 'key-token', 'doc-kinds', 'locale'];
  }

  constructor() {
    super();
    this.shadow = this.attachShadow({ mode: 'open' });
  }

  connectedCallback(): void {
    this.mount();
  }

  disconnectedCallback(): void {
    this.handle?.destroy();
    this.handle = null;
  }

  private mount(): void {
    this.handle?.destroy();

    const preset = (this.getAttribute('preset') as VerificationPreset) || 'FINTECH_KYC';
    const endpoint = this.getAttribute('endpoint');
    const keyToken = this.getAttribute('key-token');
    const locale = this.getAttribute('locale') ?? undefined;
    const rawDocs = this.getAttribute('doc-kinds');
    const docKinds = rawDocs ? (rawDocs.split(',').map((s) => s.trim()) as DocumentKind[]) : undefined;

    if (!endpoint || !keyToken) return;

    const mountPoint = document.createElement('div');
    this.shadow.replaceChildren(mountPoint);

    this.handle = mountChameleonKYC(mountPoint, {
      preset,
      ...(locale ? { locale } : {}),
      ...(docKinds ? { docKinds } : {}),
      transport: { endpoint, keyToken },
      onComplete: (receipt: SignedReceipt) => {
        this.dispatchEvent(new CustomEvent('chameleon:complete', { detail: { receipt }, bubbles: true, composed: true }));
      },
      onError: (error) => {
        this.dispatchEvent(new CustomEvent('chameleon:error', { detail: error, bubbles: true, composed: true }));
      },
      onClose: () => {
        this.dispatchEvent(new CustomEvent('chameleon:close', { bubbles: true, composed: true }));
      }
    });
  }
}

if (typeof customElements !== 'undefined' && !customElements.get('chameleon-kyc')) {
  customElements.define('chameleon-kyc', ChameleonKycElement);
}
