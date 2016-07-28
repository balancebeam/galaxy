package io.anyway.galaxy.message;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.message.producer.MessageProducer;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.util.ProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.Date;

/**
 * Created by xiong.j on 2016/7/28.
 */
@Component
public class TransactionMessageServiceImpl implements TransactionMessageService, ApplicationContextAware {

    private final static Log logger = LogFactory.getLog(io.anyway.galaxy.message.TransactionMessageService.class);

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MessageProducer<TransactionMessage> messageProducer;

    private ApplicationContext applicationContext;

    @Autowired
    private ThreadPoolTaskExecutor txMsgTaskExecutor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendMessage(long txId, TransactionStatusEnum txStatus) throws Throwable {
        //先发送消息,如果发送失败会抛出Runtime异常
        TransactionMessage message = new TransactionMessage();
        message.setTxId(txId);
        message.setTxStatus(txStatus.getCode());
        messageProducer.sendMessage(message);

        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(getNextStatus(txStatus).getCode());
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        transactionRepository.update(conn, transactionInfo);
    }

    @Transactional
    public boolean isValidMessage(TransactionMessage message) {
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        TransactionInfo transactionInfo = transactionRepository.directFindById(conn, message.getTxId());
        if (transactionInfo == null) {
            logger.warn("no tx record, message: " + message);
            return false;
        }

        if (message.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()) {
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("in confirming operation, message: " + message);
                }
                return false;
            }
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CONFIRMED.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("completed confirm operation, message: " + message);
                }
                return false;
            }
        } else {
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("in cancelling operation, message: " + message);
                }
                return false;
            }
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CANCELLED.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("completed cancel operation, message: " + message);
                }
                return false;
            }
        }

        TransactionInfo newTransactionInfo = new TransactionInfo();
        newTransactionInfo.setTxStatus(message.getTxStatus());
        newTransactionInfo.setTxId(message.getTxId());
        newTransactionInfo.setGmtModified(new Date(new java.util.Date().getTime()));
        transactionRepository.update(conn, newTransactionInfo);
        if (logger.isInfoEnabled()) {
            logger.info("update TxStatus CANCELLING , message: " + message);
        }
        return true;
    }

    public void asyncHandleMessage(final TransactionMessage message) {
        txMsgTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                io.anyway.galaxy.message.TransactionMessageService service = applicationContext.getBean(io.anyway.galaxy.message.TransactionMessageService.class);
                try {
                    service.handleMessage(message);
                } catch (Throwable e) {
                    logger.error(e);
                }
            }
        });
    }

    @Transactional
    public void handleMessage(TransactionMessage message) throws Throwable {
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        TransactionInfo transactionInfo;
        try {
            transactionInfo = transactionRepository.lockById(conn, message.getTxId());
        } catch (Exception e) {
            logger.warn("Lock failed, txId = " + message.getTxId());
            throw new DistributedTransactionException(e);
        }
        if (validation(message, transactionInfo)) {
            ServiceExecutePayload payload = JSON.parseObject(transactionInfo.getContext(), ServiceExecutePayload.class);
            Object bean = applicationContext.getBean(payload.getTarget());

            String methodName = null;
            if (TransactionStatusEnum.CANCELLING.getCode() == message.getTxStatus()) {
                // 补偿
                methodName = payload.getCancelMethod();
                if (StringUtils.isEmpty(methodName)) {
                    logger.error("miss cancel method, serviceExecutePayload: " + payload);
                    return;
                }
            } else if (TransactionStatusEnum.CONFIRMING.getCode() == message.getTxStatus()) {
                // 确认
                methodName = payload.getConfirmMethod();
                if (StringUtils.isEmpty(methodName)) {
                    logger.error("miss confirm method, serviceExecutePayload: " + payload);
                    return;
                }
            }
            // 执行消息对应的操作
            ProxyUtil.proxyMethod(bean, methodName, payload.getTypes(), payload.getArgs());
        } else {
            logger.warn("validation error, txMsg=" + message + " txInfo=" + transactionInfo);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private ServiceExecutePayload parsePayload(TransactionInfo transactionInfo) {
        String payload = transactionInfo.getPayload();
        return JSON.parseObject(payload, ServiceExecutePayload.class);
    }

    private TransactionStatusEnum getNextStatus(TransactionStatusEnum txStatus){

        if (txStatus.equals(TransactionStatusEnum.CANCELLING)) return TransactionStatusEnum.CANCELLED;

        if (txStatus.equals(TransactionStatusEnum.CONFIRMING)) return TransactionStatusEnum.CONFIRMED;

        return null;

    }

    private boolean validation(TransactionMessage message, TransactionInfo txInfo) {
        if (txInfo == null) {
            return false;
        }

        if (message.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()
                && txInfo.getTxStatus() == TransactionStatusEnum.CANCELLED.getCode()) {
            return false;
        }

        if (message.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()
                && txInfo.getTxStatus() == TransactionStatusEnum.CONFIRMED.getCode()) {
            return false;
        }

        return true;
    }


}
