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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by xiong.j on 2016/7/28.
 */
@Component
@Slf4j
public class TransactionMessageServiceImpl implements TransactionMessageService {

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
        message.setParentId(ctx.getParentId());
        message.setBusinessId(ctx.getSerialNumber());
        message.setTxStatus(txStatus.getCode());
        messageProducer.sendMessage(message);

        //发消息成功后更改TX的状态
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(ctx.getTxId());
        transactionInfo.setTxStatus(TransactionStatusEnum.getNextStatus(txStatus).getCode());
        transactionRepository.update(transactionInfo);
        log.info("Update Action TX "+ TransactionStatusEnum.getMemo(transactionInfo.getTxStatus()) +", ctx: " + ctx);
    }

    @Transactional
    public boolean isValidMessage(TransactionMessage message) throws Throwable {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setParentId(message.getParentId());
        transactionInfo.setParentId(0L);

        if (message.getTxStatus() == TransactionStatusEnum.CANCELLING.getCode()) {
            transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLING.getCode());
            return validAndSaveMessage(transactionInfo, message);
        } else if (message.getTxStatus() == TransactionStatusEnum.CONFIRMING.getCode()) {
            transactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMING.getCode());
            return validAndSaveMessage(transactionInfo, message);
        } else {
            log.warn("Incorrect status, message:" + message);
            return false;
        }
    }

    private boolean validAndSaveMessage(TransactionInfo transactionInfo, TransactionMessage message) throws Throwable {
        if (transactionRepository.find(transactionInfo).size() > 0) {
            log.info("Has main transaction record, ignored message: " + message + ", status=" + TransactionStatusEnum.getMemo(message.getTxStatus()));
            return false;
        } else {
            try {
                transactionRepository.create(transactionInfo);
            } catch (SQLException e) {
                if (e.getSQLState().equals(Constants.KEY_23505)) {
                    log.info("Has main transaction record, ignored message: " + message + ", status=" + TransactionStatusEnum.getMemo(message.getTxStatus()));
                    return false;
                } else {
                    throw e;
                }
            }
            log.info("Valid message and saved to db: " + message + ", status=" + TransactionStatusEnum.getMemo(message.getTxStatus()));
            return true;
        }
    }

    public void asyncHandleMessage(final TransactionMessage message) {
        txMsgTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
            TransactionMessageService service = SpringContextUtil.getBean(Constants.DEFAULT_MODULE_ID, TransactionMessageService.class);
            try {
                service.handleMessage(message);
            } catch (Throwable e) {
                log.error("Execute Cancel or Confirm error",e);
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

            List<TransactionInfo> infos;

            if (message.getParentId() > -1L) {
                // 定时任务调用
                TransactionInfo lockInfo = new TransactionInfo();
                lockInfo.setParentId(message.getParentId());
                lockInfo.setTxId(message.getTxId());
                try {
                    infos = transactionRepository.lock(lockInfo);
                } catch (Exception e) {
                    throw new DistributedTransactionException("Lock failed, parentId = " + message.getParentId() + ", txId = " + lockInfo.getTxId(), e);
                }
            } else {
                // 消息调用
                List<String> modules = SpringContextUtil.getModules();
                try {
                    infos = transactionRepository.lockByModules(message.getParentId(), SpringContextUtil.getModules());
                } catch (Exception e) {
                    throw new DistributedTransactionException("Lock failed, parentId = " + message.getParentId() + ", modules = " + modules, e);
                }
            }

            if (infos == null) return;
            for (TransactionInfo info : infos) {
                try {
                    if (validation(message, info)) {
                        ServiceExecutePayload payload = parsePayload(info);
                        //根据模块的ApplicationContext获取Bean对象
                        Object aopBean= SpringContextUtil.getBean(info.getModuleId(), payload.getTargetClass());

                        String methodName = null;
                        if (TransactionStatusEnum.CANCELLING.getCode() == message.getTxStatus()) {
                            // 补偿
                            methodName = payload.getCancelMethod();
                            if (StringUtils.isEmpty(methodName)) {
                                log.error("Miss Cancel method, serviceExecutePayload: " + payload);
                                return;
                            }
                        } else if (TransactionStatusEnum.CONFIRMING.getCode() == message.getTxStatus()) {
                            // 确认
                            methodName = payload.getConfirmMethod();
                            if (StringUtils.isEmpty(methodName)) {
                                log.error("Miss Confirm method, serviceExecutePayload: " + payload);
                                return;
                            }
                        }
                        // 执行消息对应的操作
                        ProxyUtil.proxyMethod(aopBean,methodName, payload.getTypes(), payload.getArgs());
                    } else {
                        log.warn("Validation error, txMsg=" + message + " txInfo=" + info);
                    }
                } catch (Exception e){
                    TransactionInfo updInfo = new TransactionInfo();
                    updInfo.setTxId(info.getTxId());
                    updInfo.setRetried_count(info.getRetried_count() - 1);
                    updInfo.setNextRetryTime(getNextRetryTime(updInfo));
                    transactionRepository.update(updInfo);
                }
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
