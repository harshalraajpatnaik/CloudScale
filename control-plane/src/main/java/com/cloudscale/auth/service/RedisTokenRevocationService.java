package com.cloudscale.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenRevocationService.class);
    private static final String REVOCATION_KEY_PREFIX = "cloudscale:revoked_tokens:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revokeToken(String tokenHash, long ttlSeconds) {
        try {
            String key = REVOCATION_KEY_PREFIX + tokenHash;
            redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
            log.debug("Token hash {} blacklisted in Redis for {}s", tokenHash, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    public boolean isTokenRevoked(String tokenHash) {
        try {
            String key = REVOCATION_KEY_PREFIX + tokenHash;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis check failed for token hash {}: {}. Falling back to DB.", tokenHash, e.getMessage());
            return false;
        }
    }
}
