package io.anyway.galaxy.demo.controller;

import io.anyway.galaxy.context.SerialNumberGenerator;
import io.anyway.galaxy.demo.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

/**
 * 下单
 * @author xiong.jie
 * @version $Id: PurchaseController.java, v 0.1 2016-7-20 下午1:33:31 Exp $
 */
@Controller
public class OrderRest {
    /** 下单处理Service */
    @Autowired
    private OrderService orderService;

    @RequestMapping(value="/order")
    @ResponseBody
    public Boolean purchase(Map<String,Object> inArgs)throws Exception {
    	long txId= (Long)inArgs.get("txId");
    	String serialNumber= (String)inArgs.get("serialNumber");
    	TXContext tx= new TXContextSupport(txId,serialNumber);
    	
    	OrderDO orderDO= new OrderDO();
    	return orderService.addOrder(tx,orderDO);
        
    }


}