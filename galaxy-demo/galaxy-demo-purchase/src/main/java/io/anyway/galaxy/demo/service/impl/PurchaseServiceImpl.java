package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.demo.domain.OrderDO;
import io.anyway.galaxy.demo.service.PurchaseService;
import io.anyway.galaxy.demo.service.OrderService;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yangzz on 16/7/19.
 */

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private AtomicInteger oId = new AtomicInteger(1);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private OrderService orderService;

    private ExecutorService executorService= Executors.newCachedThreadPool();

    @Override
    @Transactional
    @TXAction(TransactionTypeEnum.TC)
    public String purchase(long userId, long repositoryId, long number)throws Exception{

        long txId= TXContextHolder.getTXContext().getTxId();

        Future<Boolean> task= doRepository(txId,repositoryId,number);
        if(task.get()){
            task= doOrder(txId,repositoryId,userId,number);
            if(task.get()){
                return "下单成功，请在30分钟内付款!";
            }
        }
        throw new Exception("下单失败.");
    }

    private Future<Boolean> doRepository(final long txId,final long repositoryId, final long number){
       return executorService.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                TXContextHolder.setTXContext(new TXContextSupport(txId));
                return repositoryService.decreaseRepository(repositoryId,number);
            }
        });
    }

    private Future<Boolean> doOrder(final long txId,final long repositoryId,final long userId,final long number){
        return executorService.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                TXContextHolder.setTXContext(new TXContextSupport(txId));
                OrderDO orderDO = new OrderDO(oId.getAndIncrement(), repositoryId, userId,"待支付", number * 100);
                return orderService.addOrder(orderDO);
            }
        });
    }
}
