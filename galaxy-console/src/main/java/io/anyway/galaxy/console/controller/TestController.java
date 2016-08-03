package io.anyway.galaxy.console.controller;

import io.anyway.galaxy.console.dal.dao.DataSourceInfoDao;
import io.anyway.galaxy.console.dal.db.DsTypeContextHolder;
import io.anyway.galaxy.console.dal.dto.TransactionInfoDto;
import io.anyway.galaxy.console.dal.rdao.TransactionInfoDao;
import io.anyway.galaxy.domain.TransactionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.crypto.Data;

/**
 * Created by xiong.j on 2016/8/2.
 */
@Controller
@RequestMapping("/jsp")
public class TestController {

    @Autowired
    private TransactionInfoDao transactionInfoDao;

    @Autowired
    private DataSourceInfoDao dataSourceInfoDao;

    @RequestMapping(value="/testAysnJms/{times}")
    @ResponseBody
    public String testAysnJms(Model model, @PathVariable int times) {
//        long start = System.currentTimeMillis();
//        long end = System.currentTimeMillis() - start;
        TransactionInfoDto transactionInfoDto = null;

        System.out.println("dataSourceInfoDao:" + dataSourceInfoDao.get(1));

        DsTypeContextHolder.setContextType(DsTypeContextHolder.DYNAMIC_SESSION_FACTORY);
        DsTypeContextHolder.setDsType(1);

        transactionInfoDto = transactionInfoDao.get(1);
        return "Test transactionInfoDao result|" + transactionInfoDto;
    }

}
