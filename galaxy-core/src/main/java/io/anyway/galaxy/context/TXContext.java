package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public interface TXContext extends SerialNumberScenario {
    /**
     * 获取全局事务标识
     * @return
     */
    long getTxId();

}

