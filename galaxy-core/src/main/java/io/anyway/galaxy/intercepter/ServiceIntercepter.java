package io.anyway.galaxy.intercepter;

import java.sql.Connection;

import io.anyway.galaxy.context.support.ServiceExecutePayload;

/**
 * Created by yangzz on 16/7/21.
 */
public interface ServiceIntercepter {

    /**
     * 尝试执行业务事务,事务执行方法的入口
     * @param conn 业务操作开启的数据库连接
     * @param payload 存储的Service执行体内容,包含了try/confirm/cancel等方法定义
     * @param txId 事务编号
     * @param serialNumber 业务流水号
     */
    void tryService(Connection conn, ServiceExecutePayload payload, long txId,String serialNumber);

    /**
     * 提交事务,努力送达型通过调度任务实现持久化成功
     * @param conn 业务操作开启的数据库连接
     * @param txId 事务编号
     */
    void confirmService(Connection conn,long txId);

    /**
     * 回滚事务,尝试若干次回顾使用调度任务完成,如果失败交由人工处理
     * @param conn 业务操作开启的数据库连接
     * @param txId 事务编号
     */
    void cancelService(Connection conn,long txId);
}
