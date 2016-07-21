package io.anyway.galaxy.intercepter;

import io.anyway.galaxy.context.support.ServiceExcecutePayload;

import java.sql.Connection;

/**
 * Created by yangzz on 16/7/21.
 */
public interface ServiceIntercepter {

    /**
     * 尝试执行业务事务,事务执行方法的入口
     * @param conn 业务操作开启的数据库连接
     * @param payload 存储的Service执行体内容,包含了try/confirm/cancel等方法定义
     * @param txid 事务编号
     */
    void tryService(Connection conn, ServiceExcecutePayload payload,String txid);

    /**
     * 提交事务,努力送达型通过调度任务实现持久化成功
     * @param conn 业务操作开启的数据库连接
     * @param txid 事务编号
     */
    void confirmService(Connection conn,String txid);

    /**
     * 回滚事务,尝试若干次回顾使用调度任务完成,如果失败交由人工处理
     * @param conn 业务操作开启的数据库连接
     * @param txid 事务编号
     */
    void cancelService(Connection conn,String txid);
}
