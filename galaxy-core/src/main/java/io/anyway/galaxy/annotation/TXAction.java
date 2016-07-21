package io.anyway.galaxy.annotation;

import java.lang.annotation.*;

/**
 * Created by yangzz on 16/7/20.
 * 分布式事务入口
 *
 * @Transactional
 * @TXAction
 * public void purchase(){
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
public @interface TXAction {
    /**
     * 定义分布式事务执行的超时时间,默认60秒
     * @return
     */
    int timeout() default 60;

    /**
     * 事务类型TC|TCC
     * @return
     */
    TXType value() default TXType.TC;

    public static enum TXType{

        TC("try-cancel"),
        TCC("try-confirm-cancel");

        String name;

        private TXType(String name){
            this.name= name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
