package io.anyway.galaxy.scheduler;

import com.dangdang.ddframe.job.api.JobExecutionMultipleShardingContext;
import com.dangdang.ddframe.job.plugin.job.type.dataflow.AbstractBatchThroughputDataFlowElasticJob;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.recovery.TransactionRecoveryService;
import io.anyway.galaxy.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xiong.j on 2016/7/29.
 */
public class TransactionRecoveryJob extends AbstractBatchThroughputDataFlowElasticJob<TransactionInfo> {

    private static Map<Integer, Integer> statusMap = initStatus();

    @Override
    public List<TransactionInfo> fetchData(JobExecutionMultipleShardingContext shardingContext) {
        List<Integer> shardingItems = new ArrayList<Integer>(shardingContext.getShardingItems().size());
        // 每个分片对应一个状态
        for (int sharding : shardingContext.getShardingItems()) {
            if (sharding > 2) {
                break;
            }
            shardingItems.add(statusMap.get(sharding));
        }
        return getTransactionRecoveryService().fetchData(shardingItems);
    }

    @Override
    public boolean isStreamingProcess() {
        return true;
    }

    @Override
    public int processData(JobExecutionMultipleShardingContext shardingContext, List<TransactionInfo> data) {
        return getTransactionRecoveryService().execute(data);
    }

    public static Map<Integer, Integer> initStatus() {
        statusMap = new HashMap<Integer, Integer>();
        statusMap.put(0, TransactionStatusEnum.BEGIN.getCode());
        statusMap.put(1, TransactionStatusEnum.CANCELLING.getCode());
        statusMap.put(2, TransactionStatusEnum.CONFIRMING.getCode());
        return statusMap;
    }

    private TransactionRecoveryService getTransactionRecoveryService(){
        return SpringContextUtil.getBean("webapplication",TransactionRecoveryService.class);
    }
}
