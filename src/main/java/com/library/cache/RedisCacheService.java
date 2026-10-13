package com.library.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Cache Service:
 * Implements Write-Through Invalidation Strategy for Catalogue Searches & Book Availability.
 */
public class RedisCacheService {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    private static final String KEY_AVAILABILITY_PREFIX = "book:availability:";
    private static final String KEY_SEARCH_PREFIX = "catalog:search:";

    private final RedisConfig config;
    // Local in-memory fallback cache when Redis is not running locally
    private final Map<String, String> localMemoryCache = new ConcurrentHashMap<>();

    public RedisCacheService() {
        this.config = RedisConfig.getInstance();
    }

    /**
     * Retrieves cached availability count for a book.
     */
    public Integer getCachedAvailability(Long bookId) {
        String key = KEY_AVAILABILITY_PREFIX + bookId;
        if (config.isEnabled()) {
            JedisPool pool = config.getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                String val = jedis.get(key);
                if (val != null) {
                    logger.debug("[CACHE HIT] Redis key: {}", key);
                    return Integer.parseInt(val);
                }
            } catch (Exception e) {
                logger.warn("Redis error fetching key: {}", key, e);
            }
        }
        String local = localMemoryCache.get(key);
        return local != null ? Integer.parseInt(local) : null;
    }

    /**
     * Sets cached availability count with TTL.
     */
    public void setCachedAvailability(Long bookId, int count) {
        String key = KEY_AVAILABILITY_PREFIX + bookId;
        if (config.isEnabled()) {
            JedisPool pool = config.getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                jedis.setex(key, config.getAvailabilityTtl(), String.valueOf(count));
                logger.debug("[CACHE SET] Redis key: {} = {}", key, count);
                return;
            } catch (Exception e) {
                logger.warn("Redis error setting key: {}", key, e);
            }
        }
        localMemoryCache.put(key, String.valueOf(count));
    }

    /**
     * Retrieves cached catalogue search results JSON.
     */
    public String getCachedSearchResults(String queryKey) {
        String key = KEY_SEARCH_PREFIX + queryKey;
        if (config.isEnabled()) {
            JedisPool pool = config.getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                String val = jedis.get(key);
                if (val != null) {
                    logger.info("[CACHE HIT] Catalogue Search key: {}", key);
                    return val;
                }
            } catch (Exception e) {
                logger.warn("Redis error reading search key: {}", key, e);
            }
        }
        return localMemoryCache.get(key);
    }

    /**
     * Stores catalogue search results JSON with TTL.
     */
    public void setCachedSearchResults(String queryKey, String jsonPayload) {
        String key = KEY_SEARCH_PREFIX + queryKey;
        if (config.isEnabled()) {
            JedisPool pool = config.getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                jedis.setex(key, config.getCatalogTtl(), jsonPayload);
                logger.info("[CACHE SET] Catalogue Search key: {} (TTL: {}s)", key, config.getCatalogTtl());
                return;
            } catch (Exception e) {
                logger.warn("Redis error caching search results for key: {}", key, e);
            }
        }
        localMemoryCache.put(key, jsonPayload);
    }

    /**
     * WRITE-THROUGH INVALIDATION STRATEGY:
     * Called whenever a book is returned (or checked out).
     * 1. Write-Through: Instantly updates 'book:availability:{bookId}' in Redis with latest DB state.
     * 2. Invalidation: Evicts all 'catalog:search:*' keys to ensure fresh search queries.
     */
    public void writeThroughInvalidationOnReturn(Long bookId, int updatedAvailableCopies) {
        String availabilityKey = KEY_AVAILABILITY_PREFIX + bookId;
        logger.info("[WRITE-THROUGH] Updating cache for Book ID: {} -> Available: {}", bookId, updatedAvailableCopies);

        if (config.isEnabled()) {
            JedisPool pool = config.getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                // 1. Write-through update of availability
                jedis.setex(availabilityKey, config.getAvailabilityTtl(), String.valueOf(updatedAvailableCopies));

                // 2. Invalidate catalogue search caches
                Set<String> searchKeys = jedis.keys(KEY_SEARCH_PREFIX + "*");
                if (searchKeys != null && !searchKeys.isEmpty()) {
                    long deleted = jedis.del(searchKeys.toArray(new String[0]));
                    logger.info("[CACHE INVALIDATION] Evicted {} search cache keys matching '{}*'", deleted, KEY_SEARCH_PREFIX);
                }
                return;
            } catch (Exception e) {
                logger.warn("Redis error during write-through invalidation", e);
            }
        }

        // Fallback local memory eviction
        localMemoryCache.put(availabilityKey, String.valueOf(updatedAvailableCopies));
        localMemoryCache.keySet().removeIf(k -> k.startsWith(KEY_SEARCH_PREFIX));
        logger.info("[LOCAL CACHE] Invalidation complete.");
    }
}
