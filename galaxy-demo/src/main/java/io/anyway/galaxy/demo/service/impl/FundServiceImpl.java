package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXManager;
import io.anyway.galaxy.demo.domain.OrderDO;
import io.anyway.galaxy.demo.domain.RepositoryDO;
import io.anyway.galaxy.demo.service.FundService;
import io.anyway.galaxy.demo.service.OrderService;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yangzz on 16/7/19.
 */

@Service
public class FundServiceImpl implements FundService{

    private AtomicInteger oId= new AtomicInteger(1);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private OrderService orderService;

    @Override
    @Transactional
    @TXManager
    public String puyFund(int repositoryId,long number){

        repositoryService.decreaseRepository(repositoryId,number);

        OrderDO orderDO = new OrderDO(oId.getAndIncrement(),number+"份基金订单","支付成功");
        orderService.addOrder(orderDO);

        return "OK";
    }
}
