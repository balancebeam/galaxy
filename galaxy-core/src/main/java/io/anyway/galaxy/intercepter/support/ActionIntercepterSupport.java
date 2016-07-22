package io.anyway.galaxy.intercepter.support;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ActionIntercepterSupport implements ActionIntercepter{
    @Override
    public long addAction(ActionExecutePayload payload, TransactionTypeEnum type, int timeout) throws SQLException {
        return 0;
    }

    @Override
    public void tryAction(Connection conn,long txId) {

    }

    @Override
    public void confirmAction(long txId) {

    }

    @Override
    public void cancelAction(long txId) {

    }
}
