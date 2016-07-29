package io.anyway.galaxy.demo.rest;

import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.demo.domain.OrderDO;
import io.anyway.galaxy.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下单
 * @author xiong.jie
 * @version $Id: OrderRest.java, v 0.1 2016-7-20 下午1:33:31 Exp $
 */
@Controller
@RequestMapping(value = "/rest")
public class OrderRest {
    /** 下单处理Service */
    @Autowired
    private OrderService orderService;

    private AtomicLong orderNextId= new AtomicLong(1);

    @RequestMapping()
    public boolean purchase(@RequestBody Map<String,Object> params)throws Exception {
    	long txId= (Long)params.get("txId");
    	String serialNumber= (String)params.get("serialNumber");
    	TXContext tx= new TXContextSupport(txId,serialNumber);

        long orderId= orderNextId.getAndIncrement();
        long productId= (Long)params.get("productId");
        long userId= (Long)params.get("userId");
        String status= "success";
        long amount= (Long)params.get("amount");
    	OrderDO orderDO= new OrderDO(orderId,productId,userId,status,amount);

    	return orderService.addOrder(tx,orderDO);
    }


}