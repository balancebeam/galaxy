package io.anyway.galaxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yangzz on 16/7/19.
 * 分布式事务开启器
 *
 * @TXManager
 * public void orderFundProduct(){
 *      RepositoryDO repository= ...;
 *      repositoryService.decreaseRepository(repository);
 *      OrderDO order= ...;
 *      orderService.addNewOrder(order);
 * }
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TXManager {
    /**
     * 定义分布式事务执行的超时时间,默认60秒
     * @return
     */
    int timeout() default 60;
}
