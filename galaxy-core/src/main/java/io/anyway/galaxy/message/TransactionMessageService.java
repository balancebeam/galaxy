package io.anyway.galaxy.message;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.message.producer.MessageProducer;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.spring.SpringContextUtil;
import io.anyway.galaxy.util.ProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.Date;
import java.util.concurrent.*;

/**
 * Created by yangzz on 16/7/27.
 */
@Component
public class TransactionMessageService implements InitializingBean,DisposableBean,ApplicationContextAware {

    private final static Log logger= LogFactory.getLog(TransactionMessageService.class);

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MessageProducer<TransactionMessage> messageProducer;

    @Value("${tx.executor.poolSize}")
    private int poolSize= Runtime.getRuntime().availableProcessors() * 4;

    @Value("${tx.executor.timeout}")
    private int timeout= 60;

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendMessage(long txId,int msgTxStatus,int actionTxStatus) throws Throwable {
        //先发送消息,如果发送失败会抛出Runtime异常
        TransactionMessage message= new TransactionMessage();
        message.setTxId(txId);
        message.setTxStatus(msgTxStatus);
        messageProducer.sendMessage(message);
        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(txId);
        transactionInfo.setTxStatus(actionTxStatus);
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        transactionRepository.update(conn, transactionInfo);
    }

    @Transactional
    public boolean validMessage(TransactionMessage message){
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        TransactionInfo transactionInfo= transactionRepository.lockById(conn,message.getTxId());
        if(transactionInfo== null){
            logger.warn("no tx record, message: "+ message);
            return false;
        }

        if(message.getTxStatus()==TransactionStatusEnum.CONFIRMING.getCode()){
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMING.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("in confirming operation, message: "+ message);
                }
                return false;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMED.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("completed confirm operation, message: "+ message);
                }
                return false;
            }
        }
        else{
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLING.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("in cancelling operation, message: "+ message);
                }
                return false;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLED.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("completed cancel operation, message: "+ message);
                }
                return false;
            }
        }

        TransactionInfo newTransactionInfo = new TransactionInfo();
        newTransactionInfo.setTxStatus(message.getTxStatus());
        newTransactionInfo.setTxId(message.getTxId());
        newTransactionInfo.setGmtModified(new Date(new java.util.Date().getTime()));
        transactionRepository.update(conn,newTransactionInfo);
        if(logger.isInfoEnabled()){
            logger.info("update TxStatus CANCELLING , message: "+ message);
        }
        return true;
    }

    public void handleMessage(final TransactionMessage message){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                TransactionMessageService service= applicationContext.getBean(TransactionMessageService.class);
                try {
                    service.handleMessageInteral(message);
                } catch (Throwable e) {
                    logger.error(e);
                }
            }
        });
    }

    @Transactional
    public void handleMessageInteral(TransactionMessage message)throws Throwable{
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
                if(StringUtils.isEmpty(methodName)){
                    logger.error("miss cancel method, serviceExecutePayload: "+payload);
                    return;
                }
            } else if(TransactionStatusEnum.CONFIRMING.getCode() == message.getTxStatus()) {
                // 确认
                methodName = payload.getConfirmMethod();
                if(StringUtils.isEmpty(methodName)){
                    logger.error("miss confirm method, serviceExecutePayload: "+payload);
                    return;
                }
            }
            // 执行消息对应的操作
            ProxyUtil.proxyMethod(bean, methodName, payload.getTypes(), payload.getArgs());
        } else {
            logger.warn("validation error, txMsg="+message+ " txInfo="+ transactionInfo);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext= applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        executorService.shutdown();
        executorService= null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService= createTransactionMessageExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if(executorService!=null && !executorService.isShutdown()) {
                        executorService.shutdown();
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        });
    }

    private ServiceExecutePayload parsePayload(TransactionInfo transactionInfo){
        String payload= transactionInfo.getPayload();
        return JSON.parseObject(payload,ServiceExecutePayload.class);
    }

    private ExecutorService createTransactionMessageExecutor() {
        final String method= "createExecutorForTxMessage";
        int coreSize = Runtime.getRuntime().availableProcessors();
        if (poolSize < coreSize) {
            coreSize = poolSize;
        }
        ThreadFactory tf = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "thread created at tx transaction method [" + method + "]");
                t.setDaemon(true);
                return t;
            }
        };
        BlockingQueue<Runnable> queueToUse = new LinkedBlockingQueue<Runnable>(coreSize);
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, poolSize, timeout, TimeUnit.SECONDS, queueToUse,
                tf, new ThreadPoolExecutor.CallerRunsPolicy());
        if(logger.isInfoEnabled()){
            logger.info("create executorService(poolSize="+poolSize+",timeout="+timeout+") for tx transaction");
        }
        return executor;
    }

    private boolean validation(TransactionMessage message, TransactionInfo txInfo){
        if (txInfo== null) {
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
