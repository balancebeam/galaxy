package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.SerialNumberGenerator;
import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.demo.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yangzz on 16/7/19.
 */

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private AtomicInteger oId = new AtomicInteger(1);

    @Value("${rest.repository.url}")
    private String repositoryURL;

    @Value("${rest.order.url}")
    private String orderURL;

    @Autowired
    private RestOperations restOperations;

    @Override
    @Transactional
    @TXAction(value = TransactionTypeEnum.TC,bizType = "purchase")
    public String purchase(SerialNumberGenerator generator, long userId, long productId, long amount)throws Exception{

        TXContext ctx= TXContextHolder.getTXContext();

        final Map<String,Object> params= new HashMap<String, Object>();
        params.put("txId",ctx.getTxId());
        params.put("serialNumber",ctx.getSerialNumber());
        params.put("productId",productId);
        params.put("amount",amount);
        params.put("userId",userId);

        if(restOperations.postForObject(repositoryURL,null,Boolean.class,params)){
            if(restOperations.postForObject(orderURL,null,Boolean.class,params)){
                return "购买产品成功";
            }
            throw new Exception("生成订单操作失败.");
        }
        else{
            throw new Exception("减库存操作失败.");
        }
    }


}
