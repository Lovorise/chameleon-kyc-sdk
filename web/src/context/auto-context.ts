import type { AutoContextResult, CnameTransportConfig } from '../types.js';

const RTL_LOCALES = new Set(['ar', 'he', 'fa', 'ur']);

export async function resolveAutoContext(
  transport?: Pick<CnameTransportConfig, 'endpoint' | 'keyToken'>
): Promise<AutoContextResult> {
  const rawLocale = (typeof navigator !== 'undefined' ? navigator.language : 'en') || 'en';
  const baseLang = rawLocale.split('-')?.toLowerCase() ?? 'en';
  const dir: 'ltr' | 'rtl' = RTL_LOCALES.has(baseLang) ? 'rtl' : 'ltr';

  const colorScheme: 'light' | 'dark' =
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';

  let countryCode: string | undefined;

  if (transport) {
    try {
      const endpoint = transport.endpoint.replace(/\/+$/, '');
      const res = await fetch(`${endpoint}/v1/context`, {
        method: 'GET',
        credentials: 'omit',
        signal: AbortSignal.timeout(5_000),
        headers: { 'Authorization': `Bearer ${transport.keyToken}` }
      });
      if (res.ok) {
        const data = (await res.json()) as { countryCode?: string };
        if (typeof data.countryCode === 'string' && /^[A-Z]{2}$/.test(data.countryCode)) {
          countryCode = data.countryCode;
        }
      }
    } catch {
      // Fail open
    }
  }

  return countryCode
    ? { locale: rawLocale, dir, colorScheme, countryCode }
    : { locale: rawLocale, dir, colorScheme };
}
