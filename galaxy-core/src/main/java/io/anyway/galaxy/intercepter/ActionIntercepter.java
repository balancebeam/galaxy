package io.anyway.galaxy.intercepter;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.ActionExecutePayload;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by yangzz on 16/7/20.
 */
public interface ActionIntercepter {

    /**
     * 在开启业务事务时先记录一条TX记录,状态为trying
     * 这个过程开启新的事务,成功返回唯一的事务编号,失败抛异常
     * @param payload Action执行体定义
     * @param type 操作类型TC|TCC
     * @param timeout Action执行的超时时间
     * @return 事务编号
     */
    long addAction(ActionExecutePayload payload, TXAction.TXType type, int timeout);

    /**
     * 尝试成功更新,更新事务状态为tried
     * @param conn 业务操作的数据库连接
     * @param txId 事务编号
     */
    void tryAction(Connection conn,long txId);
    /**
     * 更新事务状态为confirmed
     * @param txId 事务编号
     */
    void confirmAction(long txId);

    /**
     * 更新事务状态为cancelled
     * @param txId 事务编号
     */
    void cancelAction(long txId);

}
