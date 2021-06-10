package org.popa.cache.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.popa.cache.config.MultiCacheConfig;
import org.popa.cache.util.CacheBusinessException;
import org.popa.cache.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Aspect
public class MultiCacheableAspectj {

    @Autowired
    private MultiCacheConfig multiCacheConfig;

    @Pointcut("@annotation(org.popa.cache.annotation.MultiCacheable)") // 表示所有带有MultiCacheable的注解
    public void point() {

    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Around(value = "point()")
    public Object assertAround(ProceedingJoinPoint pjp) throws Throwable {
	String methodName = pjp.getSignature().getName(); // 先获取目标方法的签名，再获取目标方法的名
	try {
	    Class<?> aClass = pjp.getTarget().getClass(); // 先获取被织入增强处理的目标对象，再获取目标类的clazz
	    Class[] parameterTypes = ((MethodSignature) pjp.getSignature()).getParameterTypes(); // 获取目标方法参数类型
	    Object[] args = pjp.getArgs(); // 获取目标方法的入参
	    if (args == null || args.length == 0) {
		throw new CacheBusinessException("参数不能为空");
	    }
	    Method method = aClass.getMethod(methodName, parameterTypes); // 获取目标方法
	    MultiCacheable annotation = method.getAnnotation(MultiCacheable.class); // 获取方法上的注解
	    String cacheKey = annotation.value()[0];
	    String paramKey = annotation.key();
	    List<Object> ids = null;
	    if (!StringUtils.isEmpty(paramKey)) {// 根据SPEL表达式获取参数
		EvaluationContext context = getContext(args, method);
		ids = getValue(context, paramKey, List.class);
	    } else { // 默认取第一个参数
		ids = (List<Object>) args[0];
	    }

	    Map<Object, Object> cacheData = getMulti(cacheKey, ids);
	    if (ids.size() > cacheData.size()) {
		args[0] = ids.stream().filter(id -> !cacheData.keySet().contains(id)).collect(Collectors.toList());
		Map<Object, Object> missObj = (Map<Object, Object>) pjp.proceed(args); // 执行目标方法
		if (missObj.size() > 0) {
		    // 放入缓存
		    putMulti(cacheKey, missObj);
		    cacheData.putAll(missObj);
		}
	    }
	    return cacheData;
	} catch (Throwable e) {
	    throw e;
	}
    }

    /**
     * 获取参数容器
     *
     * @param arguments
     *            方法的参数列表
     * @param signatureMethod
     *            被执行的方法体
     * @return 装载参数的容器
     * @throws CacheBusinessException
     */
    private EvaluationContext getContext(Object[] arguments, Method signatureMethod) throws CacheBusinessException {

	String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(signatureMethod);
	if (parameterNames == null) {
	    throw new CacheBusinessException("参数不能为空");
	}

	EvaluationContext context = new StandardEvaluationContext();
	for (int i = 0; i < arguments.length; i++) {
	    context.setVariable(parameterNames[i], arguments[i]);
	}
	return context;
    }

    /**
     * 获取spel 定义的参数值
     *
     * @param context
     *            参数容器
     * @param key
     *            key
     * @param clazz
     *            需要返回的类型
     * @param <T>
     *            返回泛型
     * @return 参数值
     */
    private <T> T getValue(EvaluationContext context, String key, Class<T> clazz) {
	SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
	Expression expression = spelExpressionParser.parseExpression(key);
	return expression.getValue(context, clazz);
    }

    /**
     * 批量获取缓存
     * 
     * @param cacheKey
     * @param ids
     * @return
     */
    public Map<Object, Object> getMulti(String cacheKey, List<Object> ids) {
	Map<Object, Object> ret = new HashMap<Object, Object>();
	if (ids == null || ids.size() == 0) {
	    return ret;
	}

	byte[][] dataBytes = buildCacheMultiKey(cacheKey, ids);
	List<byte[]> dataList = multiCacheConfig.getRedisTemplate().execute(new RedisCallback<List<byte[]>>() {
	    @Override
	    public List<byte[]> doInRedis(RedisConnection connection) {
		return connection.mGet(dataBytes);
	    }
	});

	if (dataList != null && dataList.size() > 0) {
	    for (int i = 0; i < dataList.size(); i++) {
		byte[] data = dataList.get(i);
		if (data != null && data.length > 0) {
		    Object obj = RedisKeyUtil.byteToObj2(data, false);
		    ret.put(ids.get(i), obj);
		}

	    }
	}
	return ret;
    }

    /**
     * 批量写入缓存
     * 
     * @param cacheKey
     * @param missObj
     */
    public void putMulti(String cacheKey, Map<Object, Object> missObj) {
	if (missObj == null || missObj.size() == 0) {
	    return;
	}

	Map<byte[], byte[]> dataMap = missObj.keySet().stream()
	        .collect(Collectors.toMap(v -> buildCacheKey(cacheKey, v), v -> {
	            Object dataVal = missObj.get(v);
	            return RedisKeyUtil.objToByte2(dataVal, false);
	        }));

	multiCacheConfig.getRedisTemplate().execute(new RedisCallback<Boolean>() {
	    @Override
	    public Boolean doInRedis(RedisConnection connection) {
		Boolean ret = connection.mSet(dataMap);
		dataMap.keySet().forEach(k -> connection.expire(k, MultiCacheConfig.EXPIRE_CACHE));
		return ret;
	    }
	});
    }

    /**
     * 生成批量缓存Key
     * 
     * @param cacheKey
     * @param ids
     * @return
     */
    private byte[][] buildCacheMultiKey(String cacheKey, List<Object> ids) {
	return ids.stream().map(key -> buildCacheKey(cacheKey, key)).toArray(byte[][]::new);
    }

    /**
     * 生成单个缓存key
     * 
     * @param cacheKey
     * @param id
     * @return
     */
    private byte[] buildCacheKey(String cacheKey, Object id) {
	if (multiCacheConfig.cachePrefix() != null) {
	    cacheKey = multiCacheConfig.cachePrefix() + ":" + cacheKey;
	}
	return RedisKeyUtil.objToByte2(cacheKey + "::" + id, true);
    }
}
