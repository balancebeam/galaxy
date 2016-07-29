package io.anyway.galaxy.intercepter.support;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.intercepter.ServiceIntercepter;
import io.anyway.galaxy.repository.TransactionIdGenerator;
import io.anyway.galaxy.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ServiceIntercepterSupport implements ServiceIntercepter {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public void tryService(Connection conn, ServiceExecutePayload bean, long txId,String serialNumber) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(TransactionIdGenerator.next());
        transactionInfo.setContext(JSON.toJSONString(bean));
        transactionInfo.setBusinessId(serialNumber);
        transactionInfo.setBusinessType(bean.getBizType());
        transactionInfo.setTxStatus(TransactionStatusEnum.BEGIN.getCode());
        transactionRepository.create(conn, transactionInfo);
    }

    @Override
    public void confirmService(Connection conn, long txId) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMED.getCode());
        transactionRepository.update(conn, transactionInfo);
    }

    @Override
    public void cancelService(Connection conn, long txId) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLED.getCode());
        transactionRepository.update(conn, transactionInfo);
    }
}
