package io.anyway.galaxy.intercepter;

import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.support.ActionExecutePayload;

import java.sql.Connection;

/**
 * Created by yangzz on 16/7/20.
 */
public interface ActionIntercepter {

    /**
     * 在开启业务事务时先记录一条TX记录,状态为trying
     * 这个过程开启新的事务,成功返回唯一的事务编号,失败抛异常
     * @param payload Action执行体定义
     * @return 事务编号
     */
    long addAction(ActionExecutePayload payload);

    /**
     * 尝试成功更新,更新事务状态为tried
     * @param conn 业务操作的数据库连接
     * @param txId 事务编号
     */
    void tryAction(Connection conn, long txId) throws Throwable;
    /**
     * 更新事务状态为confirmed
     * @param txId 事务编号
     */
    void confirmAction(long txId) throws Throwable;

    /**
     * 更新事务状态为cancelled
     * @param txId 事务编号
     */
    void cancelAction(long txId) throws Throwable;

}
