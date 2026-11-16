export interface VerificationReviewRecord {
  sessionId: string;
  preset: string;
  verifiedAt: string;
  signals: {
    livenessPassed: boolean;
    livenessScore: number;
    faceMatchScore?: number;
    nfcChipVerified?: boolean;
    amlStatus?: 'CLEAR' | 'POTENTIAL_MATCH' | 'NOT_RUN';
  };
  extractedFields?: Record<string, string>;
  mediaUrls?: Array<{ label: string; url: string }>;
}

export class ChameleonReviewCard extends HTMLElement {
  private readonly root: ShadowRoot;
  private record: VerificationReviewRecord | null = null;

  constructor() {
    super();
    this.root = this.attachShadow({ mode: 'open' });
  }

  public set data(value: VerificationReviewRecord | null) {
    this.record = value;
    this.render();
  }

  public get data(): VerificationReviewRecord | null {
    return this.record;
  }

  private render(): void {
    if (!this.record) {
      this.root.replaceChildren();
      return;
    }

    const { sessionId, preset, verifiedAt, signals, extractedFields, mediaUrls } = this.record;
    const safeMedia = (mediaUrls ?? []).filter((m) => this.isSafeUrl(m.url));

    this.root.innerHTML = `
      <style>
        :host { display: block; font-family: inherit; color: inherit; border: 1px solid currentColor; border-radius: 8px; padding: 16px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-top: 12px; }
        .kv { display: flex; flex-direction: column; gap: 2px; }
        .label { font-size: 0.75rem; opacity: 0.7; text-transform: uppercase; }
        .val { font-weight: 600; }
        .media { display: flex; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
        img { max-height: 160px; border-radius: 4px; object-fit: contain; }
      </style>
      <div>
        <strong>Session:</strong> <code>${this.escape(sessionId)}</code> · <span>${this.escape(preset)}</span> · <small>${this.escape(verifiedAt)}</small>
        <div class="grid">
          <div class="kv"><span class="label">Liveness</span><span class="val">${signals.livenessPassed ? 'PASS' : 'FAIL'} (${signals.livenessScore.toFixed(2)})</span></div>
          ${signals.faceMatchScore !== undefined ? `<div class="kv"><span class="label">Face Match</span><span class="val">${signals.faceMatchScore.toFixed(2)}</span></div>` : ''}
          ${signals.nfcChipVerified !== undefined ? `<div class="kv"><span class="label">ICAO NFC Chip</span><span class="val">${signals.nfcChipVerified ? 'VERIFIED' : 'UNVERIFIED'}</span></div>` : ''}
          ${signals.amlStatus ? `<div class="kv"><span class="label">AML / Sanctions</span><span class="val">${this.escape(signals.amlStatus)}</span></div>` : ''}
        </div>
        ${extractedFields ? `<div class="grid">${Object.entries(extractedFields).map(([k, v]) => `<div class="kv"><span class="label">${this.escape(k)}</span><span class="val">${this.escape(v)}</span></div>`).join('')}</div>` : ''}
        ${safeMedia.length > 0 ? `<div class="media">${safeMedia.map((m) => `<figure><img src="${this.escape(m.url)}" alt="${this.escape(m.label)}" /><figcaption>${this.escape(m.label)}</figcaption></figure>`).join('')}</div>` : ''}
      </div>
    `;
  }

  private isSafeUrl(raw: string): boolean {
    try {
      const parsed = new URL(raw);
      return parsed.protocol === 'https:' || parsed.protocol === 'blob:';
    } catch {
      return false;
    }
  }

  private escape(str: string): string {
    return str.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!));
  }
}

if (typeof customElements !== 'undefined' && !customElements.get('chameleon-review-card')) {
  customElements.define('chameleon-review-card', ChameleonReviewCard);
}
