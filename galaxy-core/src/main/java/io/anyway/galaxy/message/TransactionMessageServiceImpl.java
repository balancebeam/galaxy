package io.anyway.galaxy.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.message.producer.MessageProducer;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.SpringContextUtil;
import io.anyway.galaxy.util.ProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;

/**
 * Created by xiong.j on 2016/7/28.
 */
@Component
public class TransactionMessageServiceImpl implements TransactionMessageService {

    private final static Log logger = LogFactory.getLog(io.anyway.galaxy.message.TransactionMessageService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MessageProducer<TransactionMessage> messageProducer;

    @Autowired
    private ThreadPoolTaskExecutor txMsgTaskExecutor;

    @Value("recovery.retry.times")
    private int retryTimes = 3;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendMessage(final TXContext ctx, TransactionStatusEnum txStatus) throws Throwable {
        //先发送消息,如果发送失败会抛出Runtime异常
        TransactionMessage message = new TransactionMessage();
        message.setTxId(ctx.getTxId());
        message.setBusinessId(ctx.getSerialNumber());
        message.setTxStatus(txStatus.getCode());
        messageProducer.sendMessage(message);

        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(ctx.getTxId());
        transactionInfo.setTxStatus(getNextStatus(txStatus).getCode());
        transactionRepository.update(transactionInfo);
        if (logger.isInfoEnabled()) {
            logger.info("Update Action TX "+getNextStatus(txStatus)+", ctx: " + ctx);
        }
    }

    @Transactional
    public boolean isValidMessage(TransactionMessage message) {
        TransactionInfo transactionInfo = transactionRepository.directFindById(message.getTxId());
        if (transactionInfo == null) {
            logger.warn("Haven't transaction record, message: " + message);
            return false;
        }

        if (message.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()) {
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("In confirming operation, ignored message: " + message);
                }
                return false;
            }
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CONFIRMED.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Completed confirm operation, ignored message: " + message);
                }
                return false;
            }
        } else {
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("In cancelling operation, ignored message: " + message);
                }
                return false;
            }
            if (transactionInfo.getTxStatus() == TransactionStatusEnum.CANCELLED.getCode()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Completed cancel operation, ignored message: " + message);
                }
                return false;
            }
        }

        TransactionInfo newTransactionInfo = new TransactionInfo();
        newTransactionInfo.setTxStatus(message.getTxStatus());
        newTransactionInfo.setTxId(message.getTxId());
        //newTransactionInfo.setGmtModified(new Date(new java.util.Date().getTime()));
        transactionRepository.update(newTransactionInfo);
        if (logger.isInfoEnabled()) {
            logger.info("Valid message and saved to db: " + message + ", status=" + TransactionStatusEnum.getMemo(message.getTxStatus()));
        }
        return true;
    }

    public void asyncHandleMessage(final TransactionMessage message) {
        txMsgTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
            TransactionMessageService service = SpringContextUtil.getBean(Constants.DEFAULT_MODULE_ID, TransactionMessageService.class);
            try {
                service.handleMessage(message);
            } catch (Throwable e) {
                logger.error("Execute Cancel or Confirm error",e);
            }
            }
        });
    }

    @Transactional
    public void handleMessage(TransactionMessage message) throws Throwable {
        try {
            //从消息中获取事务的标识和业务序列号
            TXContext ctx= new TXContextSupport(message.getTxId(), message.getBusinessId());
            //设置到上下文中
            TXContextHolder.setTXContext(ctx);

            TransactionInfo transactionInfo;
            try {
                transactionInfo = transactionRepository.lockById(message.getTxId());
            } catch (Exception e) {
                logger.warn("Lock failed, txId = " + message.getTxId());
                throw new DistributedTransactionException(e);
            }
            try {
                if (validation(message, transactionInfo)) {
                    ServiceExecutePayload payload = parsePayload(transactionInfo);
                    //根据模块的ApplicationContext获取Bean对象
                    Object aopBean= SpringContextUtil.getBean(transactionInfo.getModuleId(), payload.getTargetClass());

                    String methodName = null;
                    if (TransactionStatusEnum.CANCELLING.getCode() == message.getTxStatus()) {
                        // 补偿
                        methodName = payload.getCancelMethod();
                        if (StringUtils.isEmpty(methodName)) {
                            logger.error("Miss Cancel method, serviceExecutePayload: " + payload);
                            return;
                        }
                    } else if (TransactionStatusEnum.CONFIRMING.getCode() == message.getTxStatus()) {
                        // 确认
                        methodName = payload.getConfirmMethod();
                        if (StringUtils.isEmpty(methodName)) {
                            logger.error("Miss Confirm method, serviceExecutePayload: " + payload);
                            return;
                        }
                    }
                    // 执行消息对应的操作
                    ProxyUtil.proxyMethod(aopBean,methodName, payload.getTypes(), payload.getArgs());
                } else {
                    logger.warn("Validation error, txMsg=" + message + " txInfo=" + transactionInfo);
                }
            } catch (Exception e){
                TransactionInfo updInfo = new TransactionInfo();
                updInfo.setTxId(transactionInfo.getTxId());
                updInfo.setRetried_count(transactionInfo.getRetried_count() - 1);
                updInfo.setNextRetryTime(getNextRetryTime(updInfo));
                transactionRepository.update(updInfo);
            }
        } finally {
            TXContextHolder.setTXContext(null);
        }
    }

    private ServiceExecutePayload parsePayload(TransactionInfo transactionInfo) {
        String json = transactionInfo.getContext();
        //获取模块的名称
        String moduleId= transactionInfo.getModuleId();
        ClassLoader classLoader= SpringContextUtil.getClassLoader(moduleId);
        ParserConfig config= new ParserConfig();
        //指定类加载器
        config.setDefaultClassLoader(classLoader);
        ServiceExecutePayload payload= JSON.parseObject(json, ServiceExecutePayload.class, config, null, JSON.DEFAULT_PARSER_FEATURE, new Feature[0]);
        final Object[] values= payload.getArgs();
        int index=0 ;
        for(Class<?> each: payload.getActualTypes()){
            Object val= values[index];
            if(val!= null) {
                values[index] = JSON.parseObject(val.toString(), each, config, null, JSON.DEFAULT_PARSER_FEATURE, new Feature[0]);
            }
            index++;
        }
        return payload;
    }

    private TransactionStatusEnum getNextStatus(TransactionStatusEnum txStatus){
        switch(txStatus){
            case CANCELLING:
                return TransactionStatusEnum.CANCELLED;
            case CONFIRMING:
                return TransactionStatusEnum.CONFIRMED;
            default:
                return null;
        }
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

    private Date getNextRetryTime(TransactionInfo txInfo){
        // TODO 重试次数间隔
        return new Date(System.currentTimeMillis()
                + Math.round(Math.pow(9, retryTimes - txInfo.getRetried_count()))
                * 1000);
    }

}
