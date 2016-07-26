package io.anyway.galaxy.message;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.extension.ExtensionFactory;
import io.anyway.galaxy.message.producer.MessageProducer;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.spring.SpringContextUtil;
import io.anyway.galaxy.util.ProxyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ServiceLoader;


/**
 * Created by xiong.j on 2016/7/25.
 */
@Slf4j
@Component
public class TransactionMessageServiceImpl implements MessageService<TransactionMessage> {

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void handleMessage(TransactionMessage txMsg) throws Throwable{
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        TransactionInfo transactionInfo;

        try {
            transactionInfo = transactionRepository.lockById(conn, txMsg.getTxId());
        } catch (Exception e) {
            log.info("Lock failed, txId = " + txMsg.getTxId());
            throw e;
        }

        if (validation(txMsg, transactionInfo)) {
            ServiceExecutePayload bean = JSON.parseObject(transactionInfo.getContext(), ServiceExecutePayload.class);
            Object objectClass = SpringContextUtil.getBean(bean.getTarget().getName());

            String methodName = null;
            if (TransactionStatusEnum.CANCELLING.getCode() == txMsg.getTxStatus()) {
                // 补偿
                methodName = bean.getCancelMethod();
            } else if(TransactionStatusEnum.CONFIRMING.getCode() == txMsg.getTxStatus()) {
                // 确认
                methodName = bean.getConfirmMethod();
            }

            // 执行消息对应的操作
            ProxyUtil.proxyMethod(objectClass, methodName, bean.getTypes(), bean.getArgs());

            // 处理成功后更新事务状态，在拦截器中以处理?
            /*transactionInfo = new TransactionInfo();
            transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLED.getCode());
            transactionInfo.setTxId(txMsg.getTxId());
            transactionRepository.update(dataSourceAdaptor.getDataSource().getConnection(), transactionInfo);*/
        } else {
            log.error("validation error, txMsg=", txMsg, " txInfo=", transactionInfo);
        }
    }

    public void sendMessage(TransactionMessage txMsg) throws Throwable{

        //TODO 先写死， 需优化
        ServiceLoader<MessageProducer> serviceLoader = ExtensionFactory.getExtension(MessageProducer.class);
        for (MessageProducer producer : serviceLoader) {
            producer.sendMessage(txMsg);
        }

        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLED.getCode());
        transactionInfo.setTxId(txMsg.getTxId());
        transactionRepository.update(dataSourceAdaptor.getDataSource().getConnection(), transactionInfo);
    }

    public boolean isProcessed(TransactionMessage txMsg) throws Throwable {
        try {
            Connection conn = dataSourceAdaptor.getDataSource().getConnection();
            if (transactionRepository.directFindById(conn, txMsg.getTxId()) != null) {
                TransactionInfo transactionInfo = new TransactionInfo();
                transactionInfo.setPayload(JSON.toJSONString(txMsg));
                transactionInfo.setTxId(txMsg.getTxId());
            }
            return false;
        } catch (SQLException e) {
            log.warn("Check record failed, txId=" + txMsg.getTxId());
        }

        return true;
    }

    public TransactionInfo msg2TransInfo(TransactionMessage txMsg) {
        return null;
    }

    public TransactionMessage transInfo2Msg(TransactionInfo txInfo) {
        return null;
    }

    private boolean validation(TransactionMessage txMsg, TransactionInfo txInfo){
        if (txInfo.getTxType() != txMsg.getTxType()) {
            return false;
        }

        if (txInfo.getTxType() == TransactionTypeEnum.TC.getCode()
                && txInfo.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()) {
            return false;
        }
        if (txMsg.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()
                && txInfo.getTxStatus() == TransactionStatusEnum.CANCELLED.getCode()) {
            return false;
        }

        if (txMsg.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()
                && txInfo.getTxStatus() == TransactionStatusEnum.CONFIRMED.getCode()) {
            return false;
        }

        return true;
    }

}
