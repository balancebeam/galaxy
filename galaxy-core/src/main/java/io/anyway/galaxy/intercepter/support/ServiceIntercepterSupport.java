package io.anyway.galaxy.intercepter.support;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.TXContext;
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
    public void tryService(TXContext ctx,ServiceExecutePayload bean) throws Throwable{
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTxId(ctx.getTxId());
        transactionInfo.setContext(JSON.toJSONString(bean));
        transactionInfo.setBusinessId(ctx.getSerialNumber());
        transactionInfo.setBusinessType(bean.getBizType());
        transactionInfo.setModuleId(bean.getModuleId());
        transactionInfo.setTxStatus(TransactionStatusEnum.TRIED.getCode());
        transactionRepository.create(transactionInfo);
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
