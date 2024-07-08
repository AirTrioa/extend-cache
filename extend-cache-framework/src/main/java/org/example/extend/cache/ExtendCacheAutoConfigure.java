package org.example.extend.cache;

import org.apache.commons.lang3.StringUtils;
import org.example.extend.cache.manager.ExtendCacheableHandler;
import org.example.extend.cache.manager.ExtendRedisCacheManager;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;

/**
 * 自动化配置
 *
 * @author liuzw
 * @date 2024/7/5
 */
@EnableCaching
@EnableConfigurationProperties(value = CacheProperties.class)
@ComponentScan("org.example.extend.cache")
public class ExtendCacheAutoConfigure {
  private final CacheProperties cacheProperties;

  public ExtendCacheAutoConfigure(CacheProperties cacheProperties) {
    this.cacheProperties = cacheProperties;
  }

  @Bean(name = "extendRedisCacheManager")
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                   ExtendCacheableHandler extendCacheableHandler) {
    RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
    Duration timeToLive = cacheProperties.getRedis().getTimeToLive();
    if (Objects.isNull(timeToLive)) {
      timeToLive = Duration.ofDays(1);
    }
    RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
        // 设置过期时间
        .entryTtl(timeToLive)
        // 设置序列化
        .computePrefixWith(this.genSimpleCacheKey())
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    return new ExtendRedisCacheManager(extendCacheableHandler, redisCacheWriter, defaultCacheConfig, new HashMap<>());
  }

  /**
   * 构建简单CacheKey规则
   *
   * @return 简单CacheKey规则
   */
  public CacheKeyPrefix genSimpleCacheKey() {
    String keyPrefix = cacheProperties.getRedis().getKeyPrefix();
    if (StringUtils.isBlank(keyPrefix)) {
      keyPrefix = "extend";
    }
    return CacheKeyPrefix.prefixed(keyPrefix.concat(CacheKeyPrefix.SEPARATOR));
  }
}
