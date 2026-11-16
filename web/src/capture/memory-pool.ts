export class VolatileBufferPool {
  private buffer: ArrayBuffer;
  private inUse = false;

  constructor(initialByteLength = 4 * 1024 * 1024) {
    if (!Number.isSafeInteger(initialByteLength) || initialByteLength <= 0) {
      throw new RangeError('Invalid initialByteLength for VolatileBufferPool.');
    }
    this.buffer = new ArrayBuffer(initialByteLength);
  }

  public acquire(requiredBytes: number): Uint8Array {
    if (this.inUse) {
      throw new Error('Concurrent buffer acquisition is forbidden under serial backpressure.');
    }
    if (!Number.isSafeInteger(requiredBytes) || requiredBytes <= 0) {
      throw new RangeError(`Invalid frame byte length: ${requiredBytes}`);
    }
    if (requiredBytes > this.buffer.byteLength) {
      this.scrubAndRelease();
      this.buffer = new ArrayBuffer(requiredBytes);
    }
    this.inUse = true;
    return new Uint8Array(this.buffer, 0, requiredBytes);
  }

  public scrubAndRelease(): void {
    if (this.buffer.byteLength > 0) {
      new Uint8Array(this.buffer).fill(0);
    }
    this.inUse = false;
  }

  public destroy(): void {
    this.scrubAndRelease();
    this.buffer = new ArrayBuffer(0);
  }
}
