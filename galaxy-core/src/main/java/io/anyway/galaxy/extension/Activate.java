package io.anyway.galaxy.extension;

import java.lang.annotation.*;

/**
 *
 * 对于可以被框架中自动激活加载扩展，此Annotation用于配置扩展被自动激活加载条件。
 * 比如，过滤扩展，有多个实现，使用Activate Annotation的扩展可以根据条件被自动加载。
 *
 * Created by xiong.j on 2016/7/26.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {

    /**
     * Key过滤条件。
     */
    String value() default "";

    /**
     * 排序信息，可以不提供。
     */
    int order() default 0;
}