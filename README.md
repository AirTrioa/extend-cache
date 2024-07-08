---
title: Cacheable缓存注解增强，添加缓存过期时间
date: 2024-07-08 16:51:13
tags: [Java,SpringCache]
---
# @Cacheable缓存注解增强

1. 在使用 `SpringCache` 时，虽然可以使用 `@Cacheable`、`@CachePut`、`@CacheEvict` 三个注解，简单的实现缓存的配置、编辑、删除，但是缓存的过期时间无法直接使用注解进行配置而需要使用代码/配置文件配置，所以需要添加一个 `@ExtendCacheable` 增强 `@Cacheable` 功能。

## 使用方法
1. 直接引入依赖：
```xml
   <dependency>
      <groupId>org.example</groupId>
      <artifactId>extend-cache-framework</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```
2. 在需要添加缓存的方法上，添加上`@ExtendCacheable`注解：
```java
  /**
   * 模拟向缓存中添加数据
   *
   * @return
   */
  @ExtendCacheable(value = "Example5", key = "#id", unless = "#result == null", expiredSecondTime = 200)
  @PostMapping("/add/{id}")
  public RestResult<Example> add(@PathVariable("id") Long id) {
    final Example example = new Example();
    example.setId(id);
    example.setDescribe(UUID.randomUUID().toString());
    example.setExampleTypeCode("code");
    return RestResult.buildSuccess(example);
  }
```

## 原理解析
1. 首先，创建 `@ExtendCacheable` 注解以及配套的`CacheManager`：
- 注解：
```java
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
   * 这边指定 CacheManager 为定制化的 cacheManager
   */
  String cacheManager() default "extendRedisCacheManager";
}
```
- CacheManager：
```java
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
```

2. 通过实现 `BeanPostProcessor` 实现注解元数据的拦截
```java
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
```
3. 参照 `Spring` 对注解的解析`SpringCacheAnnotationParser`，实现 `ExtendSpringCacheAnnotationParser`，实现对 自定义注解的处理
```java
public class ExtendSpringCacheAnnotationParser extends SpringCacheAnnotationParser {
  private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);

  static {
    // 关键就是这段代码
    CACHE_OPERATION_ANNOTATIONS.add(ExtendCacheable.class);
  }
  public ExtendSpringCacheAnnotationParser() {
    super();
  }

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
  }

  @Override
  public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(type);
    return parseCacheAnnotations(defaultConfig, type);
  }

  @Override
  public Collection<CacheOperation> parseCacheAnnotations(Method method) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
    return parseCacheAnnotations(defaultConfig, method);
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
    Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
    if (ops != null && ops.size() > 1) {
      // More than one operation found -> local declarations override interface-declared ones...
      Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
      if (localOps != null) {
        return localOps;
      }
    }
    return ops;
  }

  @Nullable
  private Collection<CacheOperation> parseCacheAnnotations(
      DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {

    Collection<? extends Annotation> anns = (localOnly ?
        AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) :
        AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS));
    if (anns.isEmpty()) {
      return null;
    }

    final Collection<CacheOperation> ops = new ArrayList<>(1);
    anns.stream().filter(ann -> ann instanceof ExtendCacheable).forEach(
        ann -> ops.add(parseCacheableAnnotation(ae, cachingConfig, (ExtendCacheable) ann)));
    return ops;
  }

  private CacheableOperation parseCacheableAnnotation(
      AnnotatedElement ae, DefaultCacheConfig defaultConfig, ExtendCacheable cacheable) {

    CacheableOperation.Builder builder = new CacheableOperation.Builder();

    builder.setName(ae.toString());
    builder.setCacheNames(cacheable.cacheNames());
    builder.setCondition(cacheable.condition());
    builder.setUnless(cacheable.unless());
    builder.setKey(cacheable.key());
    builder.setKeyGenerator(cacheable.keyGenerator());
    builder.setCacheManager(cacheable.cacheManager());
    builder.setCacheResolver(cacheable.cacheResolver());
    builder.setSync(cacheable.sync());

    defaultConfig.applyDefault(builder);
    CacheableOperation op = builder.build();
    validateCacheOperation(ae, op);

    return op;
  }

  /**
   * Validates the specified {@link CacheOperation}.
   * <p>Throws an {@link IllegalStateException} if the state of the operation is
   * invalid. As there might be multiple sources for default values, this ensure
   * that the operation is in a proper state before being returned.
   * @param ae the annotated element of the cache operation
   * @param operation the {@link CacheOperation} to validate
   */
  private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
    if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
      throw new IllegalStateException("Invalid cache annotation configuration on '" +
          ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
          "These attributes are mutually exclusive: either set the SpEL expression used to" +
          "compute the key at runtime or set the name of the KeyGenerator bean to use.");
    }
    if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
      throw new IllegalStateException("Invalid cache annotation configuration on '" +
          ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
          "These attributes are mutually exclusive: the cache manager is used to configure a" +
          "default cache resolver if none is set. If a cache resolver is set, the cache manager" +
          "won't be used.");
    }
  }

  private static class DefaultCacheConfig {

    private final Class<?> target;

    @Nullable
    private String[] cacheNames;

    @Nullable
    private String keyGenerator;

    @Nullable
    private String cacheManager;

    @Nullable
    private String cacheResolver;

    private boolean initialized = false;

    public DefaultCacheConfig(Class<?> target) {
      this.target = target;
    }

    /**
     * Apply the defaults to the specified {@link CacheOperation.Builder}.
     * @param builder the operation builder to update
     */
    public void applyDefault(CacheOperation.Builder builder) {
      if (!this.initialized) {
        CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(this.target, CacheConfig.class);
        if (annotation != null) {
          this.cacheNames = annotation.cacheNames();
          this.keyGenerator = annotation.keyGenerator();
          this.cacheManager = annotation.cacheManager();
          this.cacheResolver = annotation.cacheResolver();
        }
        this.initialized = true;
      }

      if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
        builder.setCacheNames(this.cacheNames);
      }
      if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
          StringUtils.hasText(this.keyGenerator)) {
        builder.setKeyGenerator(this.keyGenerator);
      }

      if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
        // One of these is set so we should not inherit anything
      }
      else if (StringUtils.hasText(this.cacheResolver)) {
        builder.setCacheResolver(this.cacheResolver);
      }
      else if (StringUtils.hasText(this.cacheManager)) {
        builder.setCacheManager(this.cacheManager);
      }
    }
  }
}
```

4. 将上面实现的解析器，注入到 `AnnotationCacheOperationSource` 的解析链中
- 该类内部使用了 `SpringCacheAnnotationParser` 类，作为注解的默认解析器，所以需要使用反射，将我们实现的解析器 注入进入
```java
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
```

5. 自动化装配配置
```java
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
```

## TODO
1. 虽然以上完成了 缓存的过期时间配置，但是仅限于使用 `Redis`缓存，还有其他类型待扩展（`ex:Zookeeper...`）；
2. `ExtendCacheable` 还可以有其他的扩展属性，比如缓存自动刷新等等。

## 参见源码地址
https://github.com/AirTrioa/extend-cache/tree/master/extend-cache-framework
