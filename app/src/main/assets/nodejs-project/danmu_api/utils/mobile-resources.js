// Approximate retained data size without allocating a second serialized payload.
function weight(value, seen = new WeakSet()) {
  if (typeof value === 'string') return 32 + value.length * 2;
  if (!value || typeof value !== 'object') return 16;
  if (seen.has(value)) return 0;
  seen.add(value);
  if (ArrayBuffer.isView(value)) return value.byteLength + 64;
  let bytes = 64;
  if (value instanceof Map) {
    for (const [key, item] of value) bytes += weight(key, seen) + weight(item, seen);
  } else {
    for (const key of Object.keys(value)) bytes += key.length * 2 + weight(value[key], seen);
  }
  return bytes;
}

export class ResourceCache extends Map {
  constructor(maxEntries, maxBytes, ttlMs) {
    super();
    this.maxEntries = maxEntries;
    this.maxBytes = maxBytes;
    this.ttlMs = ttlMs;
    this.sizes = new Map();
    this.bytes = 0;
  }
  set(key, value) {
    this.delete(key);
    const bytes = weight(value);
    if (bytes > this.maxBytes) return this; // Serve large results, but don't retain them.
    while (this.size >= this.maxEntries || this.bytes + bytes > this.maxBytes) {
      this.delete(this.keys().next().value);
    }
    super.set(key, value);
    this.sizes.set(key, bytes);
    this.bytes += bytes;
    this.schedule();
    return this;
  }
  delete(key) {
    this.bytes -= this.sizes.get(key) || 0;
    this.sizes.delete(key);
    const deleted = super.delete(key);
    this.schedule();
    return deleted;
  }
  clear() {
    super.clear();
    this.sizes.clear();
    this.bytes = 0;
    clearTimeout(this.timer);
  }
  schedule() {
    clearTimeout(this.timer);
    if (!this.size) return;
    const ttl = this.ttlMs();
    const next = Math.min(...Array.from(this.values(), v => v.timestamp + ttl));
    this.timer = setTimeout(() => {
      for (const [key, value] of this) {
        if (value.timestamp + this.ttlMs() <= Date.now()) this.delete(key);
      }
      this.schedule();
    }, Math.max(1, next - Date.now()));
    this.timer.unref?.();
  }
}

export async function mapLimited(items, limit, action) {
  const results = new Array(items.length);
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const index = next++;
      results[index] = await action(items[index], index);
    }
  }));
  return results;
}
