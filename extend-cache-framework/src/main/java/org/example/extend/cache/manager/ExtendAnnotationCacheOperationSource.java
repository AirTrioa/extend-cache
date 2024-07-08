package org.example.extend.cache.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.SpringCacheAnnotationParser;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * 针对 AnnotationCacheOperationSource，将 ExtendSpringCacheAnnotationParser 添加到属性
 *
 * @author liuzw
 * @date 2024/7/6
 */
@Configuration
public class ExtendAnnotationCacheOperationSource implements BeanDefinitionRegistryPostProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExtendAnnotationCacheOperationSource.class);

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    Object cacheOperationSource = beanFactory.getBean("cacheOperationSource");
    if (cacheOperationSource instanceof AnnotationCacheOperationSource) {
      // 针对 AnnotationCacheOperationSource 方法修改其属性，添加上 ExtendSpringCacheAnnotationParser
      // AnnotationCacheOperationSource source = (AnnotationCacheOperationSource) bean;
      LOGGER.info("拦截到 AnnotationCacheOperationSource 初始化，添加上 ExtendSpringCacheAnnotationParser 方法");
      try {
        // 反射修改 cacheOperationSource 的 annotationParsers 属性
        Field annotationParsers = AnnotationCacheOperationSource.class.getDeclaredField("annotationParsers");
        annotationParsers.setAccessible(true);
        Set<CacheAnnotationParser> cacheAnnotationParsers = new HashSet<>();
        cacheAnnotationParsers.add(new ExtendSpringCacheAnnotationParser());
        cacheAnnotationParsers.add(new SpringCacheAnnotationParser());
        annotationParsers.set(cacheOperationSource, cacheAnnotationParsers);
      } catch (Exception e) {
        LOGGER.error("反射获取AnnotationCacheOperationSource属性异常", e);
      }
    }
  }
}
