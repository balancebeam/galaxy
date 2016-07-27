package io.anyway.galaxy.intercepter.support;

import java.sql.Connection;

import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.message.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.repository.TransactionIdGenerator;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ActionIntercepterSupport implements ActionIntercepter{

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MessageProducer<TransactionMessage> messageProducer;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long addAction(ActionExecutePayload bean){
        TransactionInfo transactionInfo = new TransactionInfo();

        transactionInfo.setTxId(TransactionIdGenerator.next());
        transactionInfo.setContext(JSON.toJSONString(bean));
        transactionInfo.setBizSerial(TXContextHolder.getTXContext().getBizSerial()); //业务流水号
        transactionInfo.setBusinessType(bean.getBizType()); //业务类型
        transactionInfo.setTxType(bean.getTxType().getCode()); //TC | TCC
        transactionInfo.setTxStatus(TransactionStatusEnum.BEGIN.getCode()); //begin状态

        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        transactionRepository.create(conn, transactionInfo);
        return transactionInfo.getTxId();
    }

    @Override
    public void tryAction(final Connection conn, long txId) throws Throwable {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.TRIED.getCode());
        transactionRepository.update(conn, transactionInfo);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmAction(long txId) throws Throwable {
        //先发送消息,如果发送失败会抛出Runtime异常
        TransactionMessage message= new TransactionMessage();
        message.setTxId(txId);
        message.setBizSerial(TXContextHolder.getTXContext().getBizSerial());
        message.setTxStatus(TransactionStatusEnum.CONFIRMING.getCode());
        messageProducer.sendMessage(message);
        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMED.getCode());
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        transactionRepository.update(conn, transactionInfo);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAction(long txId) throws Throwable {
        //先发送消息,如果发送失败会抛出Runtime异常
        TransactionMessage message= new TransactionMessage();
        message.setTxId(txId);
        message.setBizSerial(TXContextHolder.getTXContext().getBizSerial());
        message.setTxStatus(TransactionStatusEnum.CANCELLING.getCode());
        messageProducer.sendMessage(message);
        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLED.getCode());
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        transactionRepository.update(conn, transactionInfo);
    }
}
