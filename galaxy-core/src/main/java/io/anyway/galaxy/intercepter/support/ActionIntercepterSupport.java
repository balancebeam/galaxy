package io.anyway.galaxy.intercepter.support;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.repository.TransactionIdGenerator;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ActionIntercepterSupport implements ActionIntercepter{

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    public long addAction(ActionExecutePayload bean, TransactionTypeEnum type, int timeout) throws Throwable {

        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(TransactionIdGenerator.next());
        transactionInfo.setContext(JSON.toJSONString(bean));
        transactionInfo.setTxType(type.getCode());
        transactionInfo.setTxStatus(TransactionStatusEnum.BEGIN.getCode());

        transactionRepository.create(dataSourceAdaptor.getDataSource().getConnection(), transactionInfo);

        return 1;
    }

    @Override
    public void tryAction(Connection conn, long txId) throws Throwable {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.TRIED.getCode());
        transactionRepository.update(conn, transactionInfo);
    }

    @Override
    public void confirmAction(long txid) throws Throwable {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txid);
        transactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMING.getCode());
        transactionRepository.update(dataSourceAdaptor.getDataSource().getConnection(), transactionInfo);
    }

    @Override
    public void cancelAction(long txId) throws Throwable {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLING.getCode());
        transactionRepository.update(dataSourceAdaptor.getDataSource().getConnection(), transactionInfo);
    }
}
