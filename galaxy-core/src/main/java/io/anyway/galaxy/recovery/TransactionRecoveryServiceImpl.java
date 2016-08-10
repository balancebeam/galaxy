package io.anyway.galaxy.recovery;

import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.message.TransactionMessageService;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.util.DateUtil;

import java.sql.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by xiong.j on 2016/7/25.
 */
@Slf4j
@Service
public class TransactionRecoveryServiceImpl implements TransactionRecoveryService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMessageService transactionMessageService;

    @Value("${recovery.retry.waitTime}")
    private static long waitTime = 10 * 1000;

    @Value("${recovery.start.day}")
    private static int day = 7;

    @Override
    public List<TransactionInfo> fetchData(List<Integer> shardingItem) {
        // 30天前
        Date searchDate = DateUtil.getPrevDate(30);

        // parentId 升序，txId降序
        return transactionRepository.findSince(searchDate, shardingItem.toArray(new Integer[shardingItem.size()]));
    }

    public int execute(List<TransactionInfo> transactionInfos) {
        int successCount = 0;

        long parentId = -1L;
        for(TransactionInfo info : transactionInfos) {

            // 未到重试时间不重试
            if (info.getGmtModified().getTime() + info.getRetried_count() * waitTime  > System.currentTimeMillis()) {
                continue;
            }

            if(TransactionStatusEnum.BEGIN.getCode() == info.getTxStatus()){
                // TODO BEGIN状态需要回查是否Try成功，后续优化
                try {
                    transactionMessageService.sendMessage(new TXContextSupport(info.getTxId(), info.getBusinessId())
                            , TransactionStatusEnum.CANCELLING);
                    successCount++;
                } catch (Throwable throwable) {
                    log.warn("Send cancel message error, TransactionInfo=", info);
                }
            } else {
                if (parentId != info.getParentId() && info.getTxId() == Constants.MAIN_ID) {
                    // 更新每个事务单元的主事务状态
                    TransactionInfo updInfo = new TransactionInfo();
                    updInfo.setParentId(info.getParentId());
                    updInfo.setTxId(Constants.MAIN_ID);
                    updInfo.setTxStatus(TransactionStatusEnum.getNextStatus(
                                TransactionStatusEnum.getEnum(info.getTxStatus())
                            ).getCode());
                    transactionRepository.update(updInfo);
                    parentId = info.getParentId();
                    continue;
                }

                if (TransactionStatusEnum.CANCELLING.getCode() == info.getTxStatus()) {
                    try {
                        transactionMessageService.handleMessage(transInfo2Msg(info));
                        successCount++;
                    } catch (Throwable e) {
                        log.warn("Process cancel error, TransactionInfo=", info.toString());
                    }
                }

                if (TransactionStatusEnum.CONFIRMING.getCode() == info.getTxStatus()) {
                    try {
                        transactionMessageService.handleMessage(transInfo2Msg(info));
                        successCount++;
                    } catch (Throwable e) {
                        log.warn("Process confirm error, TransactionInfo=", info);
                    }
                }
            }
        }
        return successCount;
    }

    private TransactionMessage transInfo2Msg(TransactionInfo txInfo){
        TransactionMessage txMsg = new TransactionMessage();
        txMsg.setParentId(txInfo.getParentId());
        txMsg.setTxId(txInfo.getTxId());
        txMsg.setBusinessId(txInfo.getBusinessId());
        txMsg.setTxStatus(txInfo.getTxStatus());
        return txMsg;
    }


}
