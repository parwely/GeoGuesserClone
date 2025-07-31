const NodeCache = require("node-cache");

class CacheService {
  constructor() {
    // In-memory cache with configurable TTL
    this.cache = new NodeCache({
      stdTTL: 300, // 5 minutes default
      checkperiod: 60, // Check for expired keys every minute
      useClones: false, // Better performance, but be careful with object mutations
    });

    // Cache statistics
    this.stats = {
      hits: 0,
      misses: 0,
      sets: 0,
    };

    console.log("🚀 Cache service initialized");
  }

  get(key) {
    const value = this.cache.get(key);
    if (value !== undefined) {
      this.stats.hits++;
      console.log(`✅ Cache HIT for key: ${key}`);
      return value;
    } else {
      this.stats.misses++;
      console.log(`❌ Cache MISS for key: ${key}`);
      return undefined;
    }
  }

  set(key, value, ttl = 300) {
    const success = this.cache.set(key, value, ttl);
    if (success) {
      this.stats.sets++;
      console.log(`💾 Cache SET for key: ${key} (TTL: ${ttl}s)`);
    }
    return success;
  }

  del(key) {
    return this.cache.del(key);
  }

  clear() {
    this.cache.flushAll();
    console.log("🧹 Cache cleared");
  }

  // Cache key generators
  generateLocationKey(options) {
    const normalized = this.normalizeOptions(options);
    return `locations:${JSON.stringify(normalized)}`;
  }

  generateStatsKey() {
    return "stats:overview";
  }

  generateLocationByIdKey(id) {
    return `location:${id}`;
  }

  generateDifficultyKey(level, limit, offset) {
    return `difficulty:${level}:${limit}:${offset}`;
  }

  generateCategoryKey(category, limit, offset) {
    return `category:${category}:${limit}:${offset}`;
  }

  // Normalize options for consistent cache keys
  normalizeOptions(options) {
    const normalized = {};

    // Sort keys for consistent hashing
    const keys = Object.keys(options).sort();
    keys.forEach((key) => {
      if (options[key] !== null && options[key] !== undefined) {
        normalized[key] = options[key];
      }
    });

    return normalized;
  }

  // Get cache statistics
  getStats() {
    return {
      ...this.stats,
      hitRate:
        (this.stats.hits / (this.stats.hits + this.stats.misses)) * 100 || 0,
      keys: this.cache.keys().length,
      size: this.cache.getStats(),
    };
  }
}

module.exports = new CacheService();
