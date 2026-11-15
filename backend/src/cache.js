/** In-memory TTL cache that keeps expired entries for a while so they can be served "stale" when the provider fails. */
export class TtlCache {
  constructor({ maxEntries = 3000, maxStaleMs = 24 * 3600 * 1000, now = () => Date.now() } = {}) {
    this.map = new Map();
    this.maxEntries = maxEntries;
    this.maxStaleMs = maxStaleMs;
    this.now = now;
  }

  /** @returns {{value:any, fresh:boolean, storedAt:number}|null} */
  get(key) {
    const e = this.map.get(key);
    if (!e) return null;
    const t = this.now();
    if (t > e.expiresAt + this.maxStaleMs) {
      this.map.delete(key);
      return null;
    }
    return { value: e.value, fresh: t <= e.expiresAt, storedAt: e.storedAt };
  }

  set(key, value, ttlMs) {
    const t = this.now();
    this.map.delete(key);
    this.map.set(key, { value, storedAt: t, expiresAt: t + ttlMs });
    while (this.map.size > this.maxEntries) {
      const oldest = this.map.keys().next().value;
      this.map.delete(oldest);
    }
  }

  get size() {
    return this.map.size;
  }
}
