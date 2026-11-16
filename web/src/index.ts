import { VolatileBufferPool } from './capture/memory-pool.js';
import { CnameIngestionClient } from './transport/cname-client.js';
import type { ChameleonConfig, ChameleonSessionHandle } from './types.js';

export * from './types.js';
export { resolveAutoContext } from './context/auto-context.js';
export { evaluateFrameQuality } from './capture/preflight.js';
export { VolatileBufferPool } from './capture/memory-pool.js';
export { CnameIngestionClient } from './transport/cname-client.js';

export function mountChameleonKYC(
  container: HTMLElement,
  config: ChameleonConfig
): ChameleonSessionHandle {
  if (!container) {
    throw new Error('mountChameleonKYC requires a valid target HTMLElement.');
  }

  const pool = new VolatileBufferPool();
  const activeStreams = new Set<MediaStream>();
  let destroyed = false;

  const hasManaged = 'transport' in config && config.transport !== undefined;
  const hasCustom = 'onFrameReady' in config && typeof config.onFrameReady === 'function';
  if (hasManaged === hasCustom) {
    throw new Error('Provide strictly one of `transport` or `onFrameReady`.');
  }

  const client = hasManaged ? new CnameIngestionClient(config.transport) : null;

  const wrapper = document.createElement('div');
  wrapper.className = 'chameleon-kyc-container';
  wrapper.setAttribute('data-preset', config.preset);
  if (config.locale) wrapper.lang = config.locale;
  container.appendChild(wrapper);

  const destroy = (): void => {
    if (destroyed) return;
    destroyed = true;
    for (const stream of activeStreams) {
      for (const track of stream.getTracks()) track.stop();
    }
    activeStreams.clear();
    pool.destroy();
    wrapper.remove();
  };

  void client;
  return { destroy };
}
