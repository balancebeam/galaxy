package io.anyway.galaxy.intercepter.support;

import java.sql.Connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.repository.impl.JdbcTransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ActionIntercepterSupport implements ActionIntercepter{

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;
        


    @Override
    public long addAction(ActionExecutePayload payload, TransactionTypeEnum type, int timeout){
        //TODO throw new TXException("no implement body");

        return -1;
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
