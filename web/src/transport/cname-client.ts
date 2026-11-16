import type { CnameTransportConfig, FramePayload, SignedReceipt } from '../types.js';

export class CnameIngestionClient {
  private readonly baseUrl: string;
  private readonly keyToken: string;
  private readonly extraHeaders: Record<string, string>;
  private readonly timeoutMs: number;

  constructor(config: CnameTransportConfig, timeoutMs = 15_000) {
    const parsed = new URL(config.endpoint);
    if (parsed.protocol !== 'https:' && parsed.hostname !== 'localhost' && parsed.hostname !== '127.0.0.1') {
      throw new Error('CNAME transport endpoint must enforce HTTPS.');
    }
    this.baseUrl = config.endpoint.replace(/\/+$/, '');
    this.keyToken = config.keyToken;
    this.extraHeaders = config.headers ?? {};
    this.timeoutMs = timeoutMs;
  }

  public async createSession(preset: string, countryCode?: string): Promise<{ sessionId: string }> {
    const res = await fetch(`${this.baseUrl}/v1/sessions`, {
      method: 'POST',
      credentials: 'omit',
      signal: AbortSignal.timeout(this.timeoutMs),
      headers: {
        ...this.extraHeaders,
        'Authorization': `Bearer ${this.keyToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ preset, countryCode })
    });
    if (!res.ok) throw new Error(`Session initialisation failed (HTTP ${res.status})`);
    return (await res.json()) as { sessionId: string };
  }

  public async sendFrame(sessionId: string, payload: FramePayload): Promise<void> {
    const res = await fetch(`${this.baseUrl}/v1/sessions/${encodeURIComponent(sessionId)}/frames`, {
      method: 'POST',
      credentials: 'omit',
      signal: AbortSignal.timeout(this.timeoutMs),
      headers: {
        ...this.extraHeaders,
        'Authorization': `Bearer ${this.keyToken}`,
        'Content-Type': payload.mimeType,
        'X-Frame-Kind': payload.kind,
        'X-Frame-Seq': String(payload.sequence),
        'X-Frame-Timestamp': String(payload.timestampMs),
        'X-Session-Nonce': payload.nonce
      },
      body: payload.bytes
    });
    if (!res.ok) throw new Error(`CNAME ingestion rejected frame (HTTP ${res.status})`);
  }

  public async finaliseSession(sessionId: string): Promise<SignedReceipt> {
    const res = await fetch(`${this.baseUrl}/v1/sessions/${encodeURIComponent(sessionId)}/finalise`, {
      method: 'POST',
      credentials: 'omit',
      signal: AbortSignal.timeout(this.timeoutMs),
      headers: {
        ...this.extraHeaders,
        'Authorization': `Bearer ${this.keyToken}`,
        'Content-Type': 'application/json'
      }
    });
    if (!res.ok) throw new Error(`Session finalisation failed (HTTP ${res.status})`);
    return (await res.json()) as SignedReceipt;
  }
}
