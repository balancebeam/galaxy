package io.anyway.galaxy.demo.controller;

import io.anyway.galaxy.demo.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 下单
 * @author xiong.jie
 * @version $Id: PurchaseController.java, v 0.1 2016-7-20 下午1:33:31 Exp $
 */
@Controller
public class PurchaseController {
    /** 下单处理Service */
    @Autowired
    private PurchaseService purchaseService;

    @RequestMapping(value="/purchase")
    @ResponseBody
    public String purchase(Model model)throws Exception {
        long id = 1;
        long item_id = 1;
        long number = 2;
        return purchaseService.purchase(id, item_id, number);
    }


}