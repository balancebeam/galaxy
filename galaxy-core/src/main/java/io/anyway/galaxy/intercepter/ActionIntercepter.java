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
     * @param conn 新的数据源连接
     * @param payload Action执行体定义
     * @param type 操作类型TC|TCC
     * @param timeout Action执行的超时时间
     * @return 事务编号
     * @throws SQLException
     */
    String addAction(Connection conn, ActionExecutePayload payload, TXAction.TXType type, int timeout)throws SQLException;

    /**
     * 更新事务状态为confirmed
     * @param conn 业务操作的数据源连接
     * @param txid 事务编号
     */
    void confirmAction(Connection conn,String txid);

    /**
     * 更新事务状态为cancelled
     * @param conn 业务操作的数据源连接
     * @param txid 事务编号
     */
    void cancelAction(Connection conn,String txid);

}
