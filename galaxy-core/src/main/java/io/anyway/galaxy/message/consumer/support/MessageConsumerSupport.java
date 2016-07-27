package io.anyway.galaxy.message.consumer.support;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.message.consumer.MessageConsumer;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.util.concurrent.*;

/**
 * Created by yangzz on 16/7/27.
 */

@Component
public class MessageConsumerSupport implements MessageConsumer<TransactionMessage>, InitializingBean,DisposableBean,ApplicationContextAware{

    private final static Log logger= LogFactory.getLog(MessageConsumerSupport.class);

    @Value("${tx.executor.poolSize}")
    private int poolSize= Runtime.getRuntime().availableProcessors() * 4;

    @Value("${tx.executor.timeout}")
    private int timeout= 60;

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext= applicationContext;
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

    @Override
    public void destroy() throws Exception {
        executorService.shutdown();
        executorService= null;
    }

    @Override
    @Transactional
    public void handleConfirmMessage(TransactionMessage message)throws Throwable {
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        TransactionInfo transactionInfo= transactionRepository.directFindById(conn,message.getTxId());
        if(transactionInfo== null){
            logger.warn("no confirm record, message: "+ message);
            return;
        }
        if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMING.getCode()){
            if(logger.isInfoEnabled()){
                logger.info("in confirming operation, message: "+ message);
            }
            return;
        }
        if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMED.getCode()){
            if(logger.isInfoEnabled()){
                logger.info("completed confirm operation, message: "+ message);
            }
            return;
        }
        //更新tx状态为CONFIRMING
        TransactionInfo newTransactionInfo = new TransactionInfo();
        newTransactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMING.getCode());
        newTransactionInfo.setTxId(message.getTxId());
        newTransactionInfo.setGmtModified(message.getDate()); //TODO action modify time or local time?
        transactionRepository.update(conn,newTransactionInfo);
        if(logger.isInfoEnabled()){
            logger.info("update TxStatus CANCELLING , message: "+ message);
        }

        final ServiceExecutePayload serviceExecutePayload= parsePayload(transactionInfo);
        if(StringUtils.isEmpty(serviceExecutePayload.getConfirmMethod())){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Object bean= applicationContext.getBean(serviceExecutePayload.getTarget());
                    try {
                        ProxyUtil.proxyMethod(bean, serviceExecutePayload.getConfirmMethod(), serviceExecutePayload.getTypes(), serviceExecutePayload.getArgs());
                        if(logger.isInfoEnabled()){
                            logger.info("execute confirm success , serviceExecutePayload: "+ serviceExecutePayload);
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            });
        }
        else{
            logger.warn("miss confirm method, serviceExecutePayload: "+serviceExecutePayload);
        }
    }

    @Override
    @Transactional
    public void handleCancelMessage(TransactionMessage message)throws Throwable{
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        conn= dataSourceAdaptor.getDataSource().getConnection();
        TransactionInfo transactionInfo= transactionRepository.directFindById(conn,message.getTxId());
        if(transactionInfo== null){
            if(logger.isInfoEnabled()){
                logger.warn("no cancel record, message: "+ message);
            }
            return;
        }
        if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLING.getCode()){
            if(logger.isInfoEnabled()){
                logger.info("in cancelling operation, message: "+ message);
            }
            return;
        }
        if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLED.getCode()){
            if(logger.isInfoEnabled()){
                logger.info("completed cancel operation, message: "+ message);
            }
            return;
        }

        //更新tx状态为CANCELLING
        TransactionInfo newTransactionInfo = new TransactionInfo();
        newTransactionInfo.setTxStatus(TransactionStatusEnum.CANCELLING.getCode());
        newTransactionInfo.setTxId(message.getTxId());
        newTransactionInfo.setGmtModified(message.getDate()); //TODO action modify time or local time?
        transactionRepository.update(conn,newTransactionInfo);
        if(logger.isInfoEnabled()){
            logger.info("update TxStatus CANCELLING , message: "+ message);
        }

        final ServiceExecutePayload serviceExecutePayload= parsePayload(transactionInfo);
        if(StringUtils.isEmpty(serviceExecutePayload.getCancelMethod())){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Object bean= applicationContext.getBean(serviceExecutePayload.getTarget());
                    try {
                        ProxyUtil.proxyMethod(bean, serviceExecutePayload.getCancelMethod(), serviceExecutePayload.getTypes(), serviceExecutePayload.getArgs());
                        if(logger.isInfoEnabled()){
                            logger.info("execute cancel success , serviceExecutePayload: "+ serviceExecutePayload);
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            });
        }
        else{
            logger.warn("miss cancel method, serviceExecutePayload: "+serviceExecutePayload);
        }
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

}
