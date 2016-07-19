package io.anyway.galaxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yangzz on 16/7/19.
 * 声明式补偿方法注解定义
 *
 * //减库存操作
 * @TXCompensable(cancel="cancelDecreaseRepository")
 * public void decreaseRepository(RepositoryDO repository){
 *     ...
 * }
 *
 * //回滚减库存操作,入参定义与减库存的方法入参一致
 * private void cancelDecreaseRepository(RepositoryDO repository){
 *     ...
 * }
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TXCompensable {

    /**
     * 定义confirm的方法,暂时不支持
     * @return
     */
    String confirm() default "";

    /**
     * 定义cancel方法,用于回滚事务
     * @return
     */
    String cancel() default "";

}
