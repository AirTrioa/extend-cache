package org.example.extend.cache.manager;

import org.example.extend.cache.anno.ExtendCacheable;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.*;

/**
 * Redis Cache 缓存支持，添加延时时间配置
 *
 * @author liuzw
 * @date 2024/7/5
 */
public class ExtendRedisCacheManager extends RedisCacheManager {
  /**
   * ExtendCacheable 注解配置
   */
  private final ExtendCacheableHandler extendCacheableHandler;
  private final Map<String, RedisCacheConfiguration> initialCacheConfigurations;
  private final RedisCacheConfiguration defaultCacheConfiguration;

  public ExtendRedisCacheManager(ExtendCacheableHandler extendCacheableHandler,
                                 RedisCacheWriter cacheWriter,
                                 RedisCacheConfiguration defaultCacheConfiguration,
                                 Map<String, RedisCacheConfiguration> initialCacheConfigurations) {
    super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations);
    this.initialCacheConfigurations = initialCacheConfigurations;
    this.defaultCacheConfiguration = defaultCacheConfiguration;
    this.extendCacheableHandler = extendCacheableHandler;
  }

  @Override
  protected Collection<RedisCache> loadCaches() {
    List<RedisCache> caches = new LinkedList<>();
    for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfigurations.entrySet()) {
      caches.add(this.createRedisCache(entry.getKey(), entry.getValue()));
    }
    return caches;
  }

  @Override
  public RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
    // 默认使用注入的 RedisCacheConfiguration
    cacheConfig = cacheConfig != null ? cacheConfig : defaultCacheConfiguration;
    // 获取Cache对应的过期时间
    ExtendCacheable extendCacheable = extendCacheableHandler.getExtendCacheableConfig(name);
    long ttlSecond = Objects.isNull(extendCacheable) ? -1 : extendCacheable.expiredSecondTime();
    if (-1 != ttlSecond) {
      // 设置过期时间【秒】
      cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(ttlSecond));
    }
    return super.createRedisCache(name, cacheConfig);
  }
}
