package com.library.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Redis Connection Pool Configuration.
 */
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    private static volatile RedisConfig instance;
    private JedisPool jedisPool;
    private boolean enabled = true;
    private int catalogTtl = 600;
    private int availabilityTtl = 3600;

    private RedisConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("redis.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            logger.warn("Could not load redis.properties, using fallback defaults", e);
        }

        this.enabled = Boolean.parseBoolean(props.getProperty("redis.enabled", "true"));
        this.catalogTtl = Integer.parseInt(props.getProperty("redis.ttl.catalog.search", "600"));
        this.availabilityTtl = Integer.parseInt(props.getProperty("redis.ttl.book.availability", "3600"));

        if (!enabled) {
            logger.info("Redis caching is disabled in configuration.");
            return;
        }

        String host = props.getProperty("redis.host", "localhost");
        int port = Integer.parseInt(props.getProperty("redis.port", "6379"));
        String password = props.getProperty("redis.password", "");
        int timeout = Integer.parseInt(props.getProperty("redis.timeout", "2000"));

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Integer.parseInt(props.getProperty("redis.maxTotal", "30")));
        poolConfig.setMaxIdle(Integer.parseInt(props.getProperty("redis.maxIdle", "10")));
        poolConfig.setMinIdle(Integer.parseInt(props.getProperty("redis.minIdle", "2")));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofMillis(timeout));

        try {
            if (password != null && !password.trim().isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
            } else {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
            }
            logger.info("JedisPool initialized for {}:{}", host, port);
        } catch (Exception e) {
            logger.warn("Failed to connect to Redis on {}:{}. In-memory fallback will be active.", host, port, e);
            this.jedisPool = null;
        }
    }

    public static RedisConfig getInstance() {
        if (instance == null) {
            synchronized (RedisConfig.class) {
                if (instance == null) {
                    instance = new RedisConfig();
                }
            }
        }
        return instance;
    }

    public JedisPool getJedisPool() { return jedisPool; }
    public boolean isEnabled() { return enabled && jedisPool != null; }
    public int getCatalogTtl() { return catalogTtl; }
    public int getAvailabilityTtl() { return availabilityTtl; }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("JedisPool closed.");
        }
    }
}
