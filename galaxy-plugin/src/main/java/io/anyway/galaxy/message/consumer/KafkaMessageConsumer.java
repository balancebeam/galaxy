package io.anyway.galaxy.message.consumer;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.extension.Activate;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.util.ProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by xiong.j on 2016/7/21.
 */
@Activate(value = Constants.KAFKA)
public class KafkaMessageConsumer implements MessageConsumer<TransactionMessage>,InitializingBean,DisposableBean,ApplicationContextAware{

    private final static Log logger= LogFactory.getLog(KafkaMessageConsumer.class);

    @Value("${kafka.servers}")
    private String servers;

    @Value("${kafka.consumer.group}")
    private String group;

    @Value("${kafka.consumer.timeout}")
    private int timeout= 30;

    @Value("${tx.executor.poolSize}")
    private int poolSize= Runtime.getRuntime().availableProcessors() * 2;

    @Value("${tx.executor.timeout}")
    private int executeTimeout= 60;

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    private volatile boolean running;

    private KafkaConsumer<String, TransactionMessage> consumer;

    @Override
    public void handleMessage(TransactionMessage message) {
    }

    private void doConfirm(TransactionMessage message)throws SQLException{
        Connection conn= null;
        try{
            conn= dataSourceAdaptor.getDataSource().getConnection();
            TransactionInfo transactionInfo= transactionRepository.directFindById(conn,message.getTxId());
            if(transactionInfo== null){
                logger.warn("application group: "+group+" has no confirm record, message: "+ message);
                return;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMING.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("application group: "+group+" is in confirming operation, message: "+ message);
                }
                return;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CONFIRMED.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("application group: "+group+" has completed confirm operation, message: "+ message);
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
                logger.info("application group: "+group+" update TxStatus CANCELLING , message: "+ message);
            }

            final ServiceExecutePayload serviceExecutePayload= toPayload(transactionInfo);
            if(StringUtils.isEmpty(serviceExecutePayload.getConfirmMethod())){
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        Object beanInstance= applicationContext.getBean(serviceExecutePayload.getTarget());
                        try {
                            ProxyUtil.proxyMethod(beanInstance, serviceExecutePayload.getConfirmMethod(), serviceExecutePayload.getTypes(), serviceExecutePayload.getArgs());
                            if(logger.isInfoEnabled()){
                                logger.info("application group: "+group+" execute confirm success , serviceExecutePayload: "+ serviceExecutePayload);
                            }
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                });
            }
            else{
                logger.warn("application group: "+group+" miss confirm method, serviceExecutePayload: "+serviceExecutePayload);
            }

        }
        finally {
            if(conn!= null){
                try {
                    conn.close();
                }catch (SQLException e){
                    logger.error(e);
                }
            }
        }
    }

    private void doCancel(TransactionMessage message)throws SQLException{
        Connection conn= null;
        try{
            conn= dataSourceAdaptor.getDataSource().getConnection();
            TransactionInfo transactionInfo= transactionRepository.directFindById(conn,message.getTxId());
            if(transactionInfo== null){
                if(logger.isInfoEnabled()){
                    logger.warn("application group: "+group+" has no cancel record, message: "+ message);
                }
                return;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLING.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("application group: "+group+" is in cancelling operation, message: "+ message);
                }
                return;
            }
            if(transactionInfo.getTxStatus()== TransactionStatusEnum.CANCELLED.getCode()){
                if(logger.isInfoEnabled()){
                    logger.info("application group: "+group+" has completed cancel operation, message: "+ message);
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
                logger.info("application group: "+group+" update TxStatus CANCELLING , message: "+ message);
            }

            final ServiceExecutePayload serviceExecutePayload= toPayload(transactionInfo);
            if(StringUtils.isEmpty(serviceExecutePayload.getCancelMethod())){
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        Object beanInstance= applicationContext.getBean(serviceExecutePayload.getTarget());
                        try {
                            ProxyUtil.proxyMethod(beanInstance, serviceExecutePayload.getCancelMethod(), serviceExecutePayload.getTypes(), serviceExecutePayload.getArgs());
                            if(logger.isInfoEnabled()){
                                logger.info("application group: "+group+" execute cancel success , serviceExecutePayload: "+ serviceExecutePayload);
                            }
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                });
            }
            else{
                logger.warn("application group: "+group+" miss cancel method, serviceExecutePayload: "+serviceExecutePayload);
            }
        }
        finally {
            if(conn!= null){
                try {
                    conn.close();
                }catch (SQLException e){
                    logger.error(e);
                }
            }
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {

        executorService= createExecutor();
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

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", group);
        props.put("enable.auto.commit", "false");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000"); //TODO
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "io.anyway.galaxy.message.serialization.TransactionMessageDeserializer");

        running= true;
        consumer = new KafkaConsumer<String, TransactionMessage>(props);
        consumer.subscribe(Arrays.asList("galaxy-tx-message"));

        if(logger.isInfoEnabled()){
            logger.info("crete kafka consumer: "+consumer+" ,subscribe topic: galaxy-tx-confirming");
        }

        for(;running;){
            try{
                ConsumerRecords<String, TransactionMessage> records = consumer.poll(timeout);
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, TransactionMessage>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, TransactionMessage> each : partitionRecords) {
                        if (each.value().getTxStatus()==TransactionStatusEnum.CANCELLING.getCode()){
                            doCancel(each.value());
                        }
                        else{
                            doConfirm(each.value());
                        }
                        long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                        Map<TopicPartition, OffsetAndMetadata> offsets= Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1));
                        //同步设置offset
                        consumer.commitSync(offsets);
                        if(logger.isInfoEnabled()){
                            logger.info("application group: "+group+" has committed offset: "+ offsets);
                        }
                    }
                }
            }catch(Throwable e){
                logger.error(e);
            }
        }
    }

    private ExecutorService createExecutor() {
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
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, poolSize, executeTimeout, TimeUnit.SECONDS, queueToUse,
                tf, new ThreadPoolExecutor.CallerRunsPolicy());
        if(logger.isInfoEnabled()){
            logger.info("create executorService(poolSize="+poolSize+",timeout="+executeTimeout+") for tx transaction");
        }
        return executor;
    }

    private ServiceExecutePayload toPayload(TransactionInfo transactionInfo){
        String payload= transactionInfo.getPayload();
        return JSON.parseObject(payload,ServiceExecutePayload.class);
    }

    @Override
    public void destroy() throws Exception {
        running= false;
        consumer.close();
        if(logger.isInfoEnabled()){
            logger.info("destroy kafka consumer: "+consumer);
        }
        if(!executorService.isShutdown()) {
            executorService.shutdown();
            executorService = null;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
