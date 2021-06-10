package org.popa.cache.util;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * redis key utils
 * 
 * @author pengc
 *
 */
public class RedisKeyUtil {

    private static RedisSerializer<String> serializerStrValue = new StringRedisSerializer();

    @SuppressWarnings({"rawtypes", "unchecked" })
    private static Jackson2JsonRedisSerializer objSer = new Jackson2JsonRedisSerializer(Object.class);

    static {

	ObjectMapper om = new ObjectMapper();
	om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
	om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
	// 当反序列化json时，未知属性会引起的反序列化被打断，这里我们禁用未知属性打断反序列化功能
	om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	objSer.setObjectMapper(om);
    }

    public static final void setExpire(String key, RedisConnection connection, Long time) {
	if (time == null || time == 0) {
	    return;
	}
	connection.expire(key.getBytes(), time);
    }

    /**
     * 存储redis数据为obj格式
     * 
     * @param o
     * @param isString
     * @return
     */
    public static final byte[] objToByte2(Object o, Boolean isString) {
	if (isString) {
	    return serializerStrValue.serialize(o.toString());
	} else {
	    return objSer.serialize(o);
	}
    }

    /**
     * 取出的redis数据为obj格式
     * 
     * @param b
     * @param isString
     * @return
     */
    public static final Object byteToObj2(byte[] b, Boolean isString) {
	if (isString) {
	    return serializerStrValue.deserialize(b);
	} else {
	    return objSer.deserialize(b);
	}
    }

}
