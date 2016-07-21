package io.anyway.galaxy.annotation;

import java.lang.annotation.*;

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
@Documented
@Inherited
public @interface TXManager {
    /**
     * 定义分布式事务执行的超时时间,默认60秒
     * @return
     */
    int timeout() default 60;
}
