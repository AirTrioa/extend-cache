package org.example.extend.cache.anno;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

/**
 * 基于 Cacheable 注解实现的增强注解
 *
 * @author liuzw
 * @date 2024/7/5
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtendCacheable {

  /**
   * 过期时间（秒）
   * 为 -1 时，代表永不过期；默认 -1
   *
   * @return 缓存过期时间
   */
  long expiredSecondTime() default -1;

  // ---------- 以下是@Cacheable 原始属性-------------//

  /**
   * Alias for {@link #cacheNames}.
   */
  @AliasFor("cacheNames")
  String[] value() default {};

  /**
   * Names of the caches in which method invocation results are stored.
   * <p>Names may be used to determine the target cache (or caches), matching
   * the qualifier value or bean name of a specific bean definition.
   *
   * @see #value
   * @see CacheConfig#cacheNames
   * @since 4.2
   */
  @AliasFor("value")
  String[] cacheNames() default {};

  /**
   * Spring Expression Language (SpEL) expression for computing the key dynamically.
   * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
   * unless a custom {@link #keyGenerator} has been configured.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String key() default "";

  /**
   * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
   * to use.
   * <p>Mutually exclusive with the {@link #key} attribute.
   *
   * @see CacheConfig#keyGenerator
   */
  String keyGenerator() default "";

  /**
   * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
   * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
   * is set already.
   * <p>Mutually exclusive with the {@link #cacheResolver}  attribute.
   *
   * @see org.springframework.cache.interceptor.SimpleCacheResolver
   * @see CacheConfig#cacheManager
   */
  String cacheManager() default "extendRedisCacheManager";

  /**
   * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
   * to use.
   *
   * @see CacheConfig#cacheResolver
   */
  String cacheResolver() default "";

  /**
   * Spring Expression Language (SpEL) expression used for making the method
   * caching conditional.
   * <p>Default is {@code ""}, meaning the method result is always cached.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String condition() default "";

  /**
   * Spring Expression Language (SpEL) expression used to veto method caching.
   * <p>Unlike {@link #condition}, this expression is evaluated after the method
   * has been called and can therefore refer to the {@code result}.
   * <p>Default is {@code ""}, meaning that caching is never vetoed.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #result} for a reference to the result of the method invocation. For
   * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
   * object, not the wrapper</li>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   *
   * @since 3.2
   */
  String unless() default "";

  /**
   * Synchronize the invocation of the underlying method if several threads are
   * attempting to load a value for the same key. The synchronization leads to
   * a couple of limitations:
   * <ol>
   * <li>{@link #unless()} is not supported</li>
   * <li>Only one cache may be specified</li>
   * <li>No other cache-related operation can be combined</li>
   * </ol>
   * This is effectively a hint and the actual cache provider that you are
   * using may not support it in a synchronized fashion. Check your provider
   * documentation for more details on the actual semantics.
   *
   * @see org.springframework.cache.Cache#get(Object, Callable)
   * @since 4.3
   */
  boolean sync() default false;
}
