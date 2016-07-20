package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXManager;
import io.anyway.galaxy.demo.domain.OrderDO;
import io.anyway.galaxy.demo.service.PurchaseService;
import io.anyway.galaxy.demo.service.OrderService;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    @TXManager
    public String purchase(long userId, long repositoryId, long number){

        if (repositoryService.decreaseRepository(repositoryId,number)) {
            OrderDO orderDO = new OrderDO(oId.getAndIncrement(), repositoryId, userId,"待支付", number * 100);
            orderService.addOrder(orderDO);
            return "下单成功，请在30分钟内付款!";
        } else {
            return "扣减库存失败!";
        }
    }
}
