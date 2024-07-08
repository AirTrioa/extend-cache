package org.example.extend.cache.manager;

import org.apache.commons.lang3.ArrayUtils;
import org.example.extend.cache.anno.ExtendCacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtendCacheableHandler
 *
 * @author liuzw
 * @date 2024/7/6
 */
@Configuration
public class ExtendCacheableHandler implements BeanPostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtendCacheableHandler.class);
  /**
   * 每个缓存的注解配置
   */
  private static final Map<String, ExtendCacheable> CACHE_TTL_MAP = new ConcurrentHashMap<>();

  /**
   * 获取ExtendCacheable的注解配置
   *
   * @param cacheName cacheName
   * @return ExtendCacheable
   */
  public ExtendCacheable getExtendCacheableConfig(String cacheName) {
    return CACHE_TTL_MAP.get(cacheName);
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // 过滤每个类中方法的 @ExtendCacheable 注解
    final Method[] methods = AopUtils.getTargetClass(bean).getDeclaredMethods();
    if (ArrayUtils.isEmpty(methods)) {
      // 若没有类方法，则直接返回Bean
      return bean;
    }
    for (Method method : methods) {
      // 获取 @ExtendCacheable 注解
      final ExtendCacheable extendCacheable = method.getAnnotation(ExtendCacheable.class);
      if (Objects.nonNull(extendCacheable)) {
        LOGGER.info("拦截到ExtendCache缓存配置,cacheNames:{},value:{}", extendCacheable.cacheNames(), extendCacheable.value());
        // 设置每个 Cache 的缓存过期时间
        Arrays.stream(extendCacheable.cacheNames()).forEach(k -> CACHE_TTL_MAP.put(k, extendCacheable));
        Arrays.stream(extendCacheable.value()).forEach(k -> CACHE_TTL_MAP.put(k, extendCacheable));
      }
    }
    return bean;
  }
}
