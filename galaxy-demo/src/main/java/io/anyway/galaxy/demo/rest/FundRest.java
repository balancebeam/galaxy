package io.anyway.galaxy.demo.rest;

import io.anyway.galaxy.demo.service.FundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by yangzz on 16/7/19.
 */

@RestController
@RequestMapping
public class FundRest {

    @Autowired
    private FundService service;

    @RequestMapping(value = "/{id}/{number}", method = RequestMethod.GET)
    public String buy(@PathVariable Integer id,@PathVariable Long number){
        return service.puyFund(id,number);
    }
}
