package io.anyway.galaxy.demo.rest;

import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 下单
 * @author xiong.jie
 * @version $Id: RepositoryRest.java, v 0.1 2016-7-20 下午1:33:31 Exp $
 */
@Controller
@RequestMapping(value = "/rest")
public class RepositoryRest {
    /** 库存Service */
    @Autowired
    private RepositoryService repositoryService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public boolean purchase(@RequestBody  Map<String,Object> params)throws Exception {
        long txId= Long.parseLong(params.get("txId").toString());
        int txType = (Integer)(params.get("txType"));
        String businessType = (String)params.get("businessType");
    	String serialNumber= (String)params.get("serialNumber");
    	TXContext tx= new TXContextSupport(txId, serialNumber, businessType);

        long productId= Long.parseLong(params.get("productId").toString());
        long amount= Long.parseLong(params.get("amount").toString());
    	return repositoryService.decreaseRepository(tx,productId,amount);
    }

}