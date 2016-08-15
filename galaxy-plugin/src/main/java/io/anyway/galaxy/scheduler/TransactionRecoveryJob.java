package io.anyway.galaxy.scheduler;

import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.recovery.TransactionRecoveryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.anyway.galaxy.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.JobConfiguration;
import com.dangdang.ddframe.job.api.JobExecutionMultipleShardingContext;
import com.dangdang.ddframe.job.api.JobScheduler;
import com.dangdang.ddframe.job.plugin.job.type.dataflow.AbstractBatchThroughputDataFlowElasticJob;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;

/**
 * Created by xiong.j on 2016/7/29.
 */
public class TransactionRecoveryJob extends AbstractBatchThroughputDataFlowElasticJob<TransactionInfo> {

    private static Map<Integer, Integer> statusMap = initStatus();

    @Autowired
    private CoordinatorRegistryCenter regCenter;

    @Autowired
    @Qualifier("transactionRecoveryJobConfig")
    private JobConfiguration jobConfiguration;

    @Autowired
    private static SpringContextUtil springContextUtil;

    @Autowired
    private static TransactionRecoveryService transactionRecoveryService;

    public void init(){
        new JobScheduler(regCenter, jobConfiguration).init();
    }

    @Override
    public List<TransactionInfo> fetchData(JobExecutionMultipleShardingContext shardingContext) {

        if (this.transactionRecoveryService == null) {
            this.transactionRecoveryService = SpringContextUtil.getBean(springContextUtil.getModuleId(), TransactionRecoveryService.class);
        }

        List<Integer> shardingItems = new ArrayList<Integer>(shardingContext.getShardingItems().size());
        // 每个分片对应一个状态
        for (int sharding : shardingContext.getShardingItems()) {
            if (sharding >= statusMap.size()) {
                break;
            }
            shardingItems.add(statusMap.get(sharding));
        }
        return transactionRecoveryService.fetchData(shardingItems);
    }

    @Override
    public boolean isStreamingProcess() {
        return true;
    }

    @Override
    public int processData(JobExecutionMultipleShardingContext shardingContext, List<TransactionInfo> data) {
        return transactionRecoveryService.execute(data);
    }

    private static Map<Integer, Integer> initStatus() {
        statusMap = new HashMap<Integer, Integer>();
        statusMap.put(0, TransactionStatusEnum.BEGIN.getCode());
        statusMap.put(1, TransactionStatusEnum.TRIED.getCode());
        statusMap.put(2, TransactionStatusEnum.CANCELLING.getCode());
        statusMap.put(3, TransactionStatusEnum.CONFIRMING.getCode());
        return statusMap;
    }


    public void setRegCenter(CoordinatorRegistryCenter regCenter) {
        this.regCenter = regCenter;
    }

    public void setJobConfiguration(JobConfiguration jobConfiguration) {
        this.jobConfiguration = jobConfiguration;
    }

    public void setSpringContextUtil(SpringContextUtil springContextUtil) {
        this.springContextUtil = springContextUtil;
    }

    public void setTransactionRecoveryService(TransactionRecoveryService transactionRecoveryService) {
        this.transactionRecoveryService = transactionRecoveryService;
    }
}
