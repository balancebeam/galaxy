package io.anyway.galaxy.annotation;

import java.lang.annotation.*;

/**
 * Created by yangzz on 16/7/20.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
@Inherited
public @interface TXTry {
    /**
     * 提交方法
     * @return
     */
    String confirm() default "";

    /**
     * 回滚方法
     * @return
     */
    String cancel() default "";
}
