package io.anyway.galaxy.context;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/21.
 */
public interface TXContext extends Serializable{
    /**
     * 获取全局事务标识
     * @return
     */
    long getTxId();

    /**
     * 是不是Action入口
     * @return
     */
    boolean isAction();

    /**
     * 用户需要设置关联的业务流水号
     * @param bizSerial
     */
    void setBizSerial(String bizSerial);

    /**
     * 获取业务流水号
     * @return
     */
    String getBizSerial();

}

