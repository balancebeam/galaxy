package io.anyway.galaxy.demo.rest;

import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @RequestMapping
    public boolean purchase(@RequestBody  Map<String,Object> params)throws Exception {
    	long txId= (Long)params.get("txId");
    	String serialNumber= (String)params.get("serialNumber");
    	TXContext tx= new TXContextSupport(txId,serialNumber);
        long productId= (Long)params.get("productId");
        long amount= (Long)params.get("amount");
    	return repositoryService.decreaseRepository(tx,productId,amount);
    }

}