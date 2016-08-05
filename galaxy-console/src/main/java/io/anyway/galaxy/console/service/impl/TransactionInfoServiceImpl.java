package io.anyway.galaxy.console.service.impl;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.console.dal.dao.BusinessTypeDao;
import io.anyway.galaxy.console.dal.db.DsTypeContextHolder;
import io.anyway.galaxy.console.dal.dto.BusinessTypeDto;
import io.anyway.galaxy.console.dal.dto.TransactionInfoDto;
import io.anyway.galaxy.console.dal.rdao.TransactionInfoDao;
import io.anyway.galaxy.console.domain.BusinessTypeInfo;
import io.anyway.galaxy.console.domain.TransactionInfo;
import io.anyway.galaxy.console.service.TransactionInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiong.j on 2016/8/4.
 */
@Slf4j
@Service
public class TransactionInfoServiceImpl implements TransactionInfoService {

    @Autowired
    private ThreadPoolTaskExecutor txMsgTaskExecutor;

    @Autowired
    private BusinessTypeDao businessTypeDao;

    @Autowired
    private TransactionInfoDao transactionInfoDao;

    private static long WAIT_TIME = 30;

    public List<TransactionInfo> list(BusinessTypeInfo businessTypeInfo) {
        final BusinessTypeDto businessTypeDto;

        // 获取业务状态详情
        businessTypeDto = businessTypeDao.get(businessTypeInfo.getId());

        if (businessTypeDto == null) return null;

        // 根据业务状态对应的数据源获取事务信息
        List<Future<TransactionInfoInner>> futureList = new ArrayList<Future<TransactionInfoInner>>();
        List<Long> dsIds = JSON.parseArray(businessTypeDto.getDsId(), Long.TYPE);
        for (final Long dsId : dsIds) {
            Future<TransactionInfoInner> future = txMsgTaskExecutor.submit(
                    new FindTransactionInfo(dsId, businessTypeDto));

            futureList.add(future);
        }

        return getResult(futureList);
    }

    private List<TransactionInfo> getResult(List<Future<TransactionInfoInner>> futureList){
        List<TransactionInfo> resultList = new ArrayList<TransactionInfo>();
        TransactionInfoInner transactionInfoInner = null;
        for (Future<TransactionInfoInner> future : futureList) {
            try {
                transactionInfoInner = future.get(WAIT_TIME, TimeUnit.SECONDS);
                if (transactionInfoInner != null) {
                    resultList.addAll(transactionInfoInner.getTransactionInfos());
                }
            } catch (Exception e) {
                if (transactionInfoInner != null) {
                    log.warn("Error, Get TransactionInfo from BusinessType=" + transactionInfoInner.getBusinessType()
                            + ", dateSource id= " + transactionInfoInner.getDsId(), e);
                } else {
                    log.warn("Error, Get TransactionInfo from BusinessType=" + transactionInfoInner.getBusinessType(), e);
                }
            }
        }
        return resultList;
    }

    private class FindTransactionInfo implements Callable<TransactionInfoInner>{

        private long dsId;

        private BusinessTypeDto businessTypeDto;

        public FindTransactionInfo(long dsId, BusinessTypeDto businessTypeDto){
            this.dsId = dsId;
            this.businessTypeDto = businessTypeDto;
        }

        @Override
        public TransactionInfoInner call() throws Exception {

            TransactionInfoInner transactionInfoInner = new TransactionInfoInner();
            transactionInfoInner.setBusinessType(businessTypeDto.getName());
            transactionInfoInner.setDsId(dsId);

            // 设置线程上下文,使用配置的数据源查询事务信息
            DsTypeContextHolder.setContextType(DsTypeContextHolder.DYNAMIC_SESSION_FACTORY);
            DsTypeContextHolder.setDsType(dsId);
            TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
            transactionInfoDto.setBusinessType(businessTypeDto.getName());
            List<TransactionInfoDto> dtos = transactionInfoDao.list(transactionInfoDto);

            List<TransactionInfo> infos = new ArrayList<TransactionInfo>();
            TransactionInfo transactionInfo;
            for (TransactionInfoDto dto : dtos) {
                transactionInfo =  new TransactionInfo();
                BeanUtils.copyProperties(dto, transactionInfo);
                infos.add(transactionInfo);
            }
            transactionInfoInner.setTransactionInfos(infos);
            return transactionInfoInner;
        }
    }

    private class TransactionInfoInner{

        private List<TransactionInfo> transactionInfos;

        private String businessType;

        private long dsId;

        public List<TransactionInfo> getTransactionInfos() {
           return transactionInfos;
        }

        public void setTransactionInfos(List<TransactionInfo> transactionInfos) {
           this.transactionInfos = transactionInfos;
        }

        public String getBusinessType() {
           return businessType;
        }

        public void setBusinessType(String businessType) {
           this.businessType = businessType;
        }

        public long getDsId() {
           return dsId;
        }

        public void setDsId(long dsId) {
           this.dsId = dsId;
        }
   }

    public static void main(String args[]){
        String list = JSON.toJSONString(new long[]{0, 1, 2});
        System.out.println(list);
        JSON.parseArray(list, Long.TYPE);
    }
}
