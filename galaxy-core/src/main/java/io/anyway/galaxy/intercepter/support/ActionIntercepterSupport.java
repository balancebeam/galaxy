package io.anyway.galaxy.intercepter.support;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.exception.TXException;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ActionIntercepterSupport implements ActionIntercepter{

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Override
    public long addAction(ActionExecutePayload payload, TXAction.TXType type, int timeout) {
        throw new TXException("no implement body");
    }

    @Override
    public void tryAction(Connection conn,long txId) {

    }

    @Override
    public void confirmAction(long gittxId) {

    }

    @Override
    public void cancelAction(long txId) {

    }
}
