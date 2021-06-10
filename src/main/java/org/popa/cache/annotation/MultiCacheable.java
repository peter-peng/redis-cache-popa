
package org.popa.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MultiCacheable {

    /**
     * Alias for {@link #cacheNames}.
     */
    @AliasFor("cacheNames")
    String[] value() default {};

    /**
     * Names of the caches in which method invocation results are stored.
     * <p>
     * Names may be used to determine the target cache (or caches), matching the
     * qualifier value or bean name of a specific bean definition.
     * 
     * @since 4.2
     * @see #value
     * @see CacheConfig#cacheNames
     */
    @AliasFor("value")
    String[] cacheNames() default {};

    /**
     * Spring Expression Language (SpEL) expression for computing the key
     * dynamically.
     * <p>
     * Default is {@code ""}, meaning all method parameters are considered as a
     * key, unless a custom {@link #keyGenerator} has been configured.
     * <p>
     * The SpEL expression evaluates against a dedicated context that provides
     * the following meta-data:
     * <ul>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches}
     * for references to the {@link java.lang.reflect.Method method}, target
     * object, and affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target
     * class ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second
     * argument can be accessed via {@code #root.args[1]}, {@code #p1} or
     * {@code #a1}. Arguments can also be accessed by name if that information
     * is available.</li>
     * </ul>
     */
    String key() default "";

}
