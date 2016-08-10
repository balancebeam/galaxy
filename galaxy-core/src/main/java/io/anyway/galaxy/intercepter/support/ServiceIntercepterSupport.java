package io.anyway.galaxy.intercepter.support;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.intercepter.ServiceIntercepter;
import io.anyway.galaxy.repository.TransactionIdGenerator;
import io.anyway.galaxy.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ServiceIntercepterSupport implements ServiceIntercepter {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public void tryService(TXContext ctx,ServiceExecutePayload bean) throws Throwable{
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setParentId(ctx.getTxId());
        transactionInfo.setTxId(Constants.MAIN_ID);

        if (transactionRepository.find(transactionInfo).size() > 0) {
//            log.warn("Received cancel command from main transaction unit, interrupt current transaction!");
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return;
            throw new DistributedTransactionException("Received cancel command from main transaction unit, interrupt current transaction!");
        }

        // 插入事务数据
        transactionInfo = new TransactionInfo();
        transactionInfo.setParentId(ctx.getTxId());
        transactionInfo.setTxId(TransactionIdGenerator.next());
        transactionInfo.setContext(JSON.toJSONString(bean));
        transactionInfo.setBusinessId(ctx.getSerialNumber());
        transactionInfo.setBusinessType(bean.getBizType());
        transactionInfo.setModuleId(bean.getModuleId());
        transactionInfo.setTxStatus(TransactionStatusEnum.TRIED.getCode());

        int i = 2;
        while(i > 0) {
            try {
                transactionRepository.create(transactionInfo);
            } catch (SQLException e) {
                if (e.getSQLState().equals(Constants.KEY_23505)) {
                    transactionInfo.setTxId(TransactionIdGenerator.next());
                    transactionRepository.create(transactionInfo);
                } else {
                    throw e;
                }
            }
            i--;
        }
        ((TXContextSupport)ctx).setParentId(transactionInfo.getParentId());
        ((TXContextSupport)ctx).setTxId(transactionInfo.getTxId());
    }

    @Override
    public void confirmService(TXContext ctx) throws Throwable{
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(ctx.getTxId());
        transactionInfo.setTxStatus(TransactionStatusEnum.CONFIRMED.getCode());
        transactionRepository.update(transactionInfo);
    }

    @Override
    public void cancelService(TXContext ctx) throws Throwable{
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(ctx.getTxId());
        transactionInfo.setTxStatus(TransactionStatusEnum.CANCELLED.getCode());
        transactionRepository.update(transactionInfo);
    }
}
