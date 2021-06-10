# REDIS CACHE 

补充`@Cacheable` 标签进行批量获取对象.使用了redis的原子命令 `mget` 和 `mset` 来实现


## POM依赖
```
 <dependency>
      <groupId>com.popa.cache</groupId>
      <artifactId>redis-cache-popa</artifactId>
      <version>1.0.0</version>
</dependency>
```

## Examples

### 注入RedisTemplate

注意和`@Cacheable`依赖的redisTemplate保持一致:

### 修改cachePrefix

注意和RedisCacheConfiguration的computePrefixWith保持一致:


```java
    @Bean
    public MultiCacheConfig initMultiCacheConfig() {
  return new MultiCacheConfig() {

      @Override
      public RedisTemplate<String, ?> getRedisTemplate() {
    return redisTemplate;
      }

      @Override
      public String cachePrefix() {
    return "xxxx";
      }

  };
    }
```


### 包扫描

扫描依赖包

```java
@ComponentScan(basePackages = {"org.popa.cache.annotation" })
```


### 注解使用
`@MultiCacheable` 补充批量获取缓存

```java
   
    @Cacheable(value = "goods:view:spu", key = "#id", unless = "#result == null")
    public GoodsSpu findById(Long id) {
  return spuMapper.selectById(id);
    }

    /**
     * 批量获取spu缓存
     * 
     * @param ids
     * @return
     */
    @MultiCacheable(value = "goods:view:spu")
    public Map<Long, GoodsSpu> findByIds(List<Long> ids) {
  List<GoodsSpu> contents = spuMapper.selectBatchIds(ids);
  return contents.stream().collect(Collectors.toMap(GoodsSpu::getId, Function.identity()));
    }
```

### 声明
不足之处欢迎补充讨论
