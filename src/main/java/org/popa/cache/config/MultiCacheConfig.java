package org.popa.cache.config;

import org.springframework.data.redis.core.RedisTemplate;

public abstract class MultiCacheConfig {

    /**
     * 缓存前缀
     */
    private static final String MODEL_CACHE = "{popa-cache}";

    public static final int EXPIRE_CACHE = 24 * 60 * 60;

    public abstract RedisTemplate<String, ?> getRedisTemplate();

    public String cachePrefix() {
	return MODEL_CACHE;
    }

}
