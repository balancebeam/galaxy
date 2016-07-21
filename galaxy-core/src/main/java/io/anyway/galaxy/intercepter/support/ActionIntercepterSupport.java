package io.anyway.galaxy.intercepter.support;

import io.anyway.galaxy.annotation.TXAction;
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
    public String addAction(Connection conn, ActionExecutePayload payload, TXAction.TXType type, int timeout) throws SQLException {
        return null;
    }

    @Override
    public void confirmAction(Connection conn, String txid) {

    }

    @Override
    public void cancelAction(Connection conn, String txid) {

    }
}
