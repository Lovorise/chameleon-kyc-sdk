export interface PreFlightResult {
  passed: boolean;
  reason?: 'ERR_PREFLIGHT_LUMA' | 'ERR_PREFLIGHT_GLARE' | 'ERR_PREFLIGHT_BLUR';
  lumaMean: number;
  glareRatio: number;
  laplacianVariance: number;
}

export function evaluateFrameQuality(
  rgba: Uint8ClampedArray,
  width: number,
  height: number
): PreFlightResult {
  const pixelCount = width * height;
  if (pixelCount === 0) {
    return { passed: false, reason: 'ERR_PREFLIGHT_LUMA', lumaMean: 0, glareRatio: 0, laplacianVariance: 0 };
  }

  const luma = new Uint8Array(pixelCount);
  let lumaSum = 0;
  let clippedPixels = 0;

  for (let i = 0, p = 0; i < rgba.length; i += 4, p++) {
    const y = (77 * rgba[i]! + 150 * rgba[i + 1]! + 29 * rgba[i + 2]!) >> 8;
    luma[p] = y;
    lumaSum += y;
    if (y >= 250) clippedPixels++;
  }

  const lumaMean = lumaSum / pixelCount;
  const glareRatio = clippedPixels / pixelCount;

  let lapSum = 0;
  let lapSqSum = 0;
  let interiorCount = 0;

  for (let y = 1; y < height - 1; y++) {
    const rowOffset = y * width;
    for (let x = 1; x < width - 1; x++) {
      const idx = rowOffset + x;
      const lap =
        luma[idx - width]! +
        luma[idx - 1]! +
        luma[idx + 1]! +
        luma[idx + width]! -
        4 * luma[idx]!;
      lapSum += lap;
      lapSqSum += lap * lap;
      interiorCount++;
    }
  }

  luma.fill(0);

  const meanLap = interiorCount > 0 ? lapSum / interiorCount : 0;
  const laplacianVariance = interiorCount > 0 ? lapSqSum / interiorCount - meanLap * meanLap : 0;

  if (lumaMean < 45 || lumaMean > 225) {
    return { passed: false, reason: 'ERR_PREFLIGHT_LUMA', lumaMean, glareRatio, laplacianVariance };
  }
  if (glareRatio > 0.12) {
    return { passed: false, reason: 'ERR_PREFLIGHT_GLARE', lumaMean, glareRatio, laplacianVariance };
  }
  if (laplacianVariance < 85) {
    return { passed: false, reason: 'ERR_PREFLIGHT_BLUR', lumaMean, glareRatio, laplacianVariance };
  }

  return { passed: true, lumaMean, glareRatio, laplacianVariance };
}
