package io.anyway.galaxy.recovery;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.message.TransactionMessageService;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Date;
import java.util.List;

/**
 * Created by xiong.j on 2016/7/25.
 */
@Slf4j
@Component
public class TransactionRecoveryServiceImpl implements TransactionRecoveryService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMessageService transactionMessageService;

    @Override
    public List<TransactionInfo> fetchData(List<Integer> shardingItem) {
        // 30天前
        Date searchDate = DateUtil.getPrevDate(30);

        return transactionRepository.findSince(searchDate, shardingItem.toArray(new Integer[shardingItem.size()]));
    }

    public int execute(List<TransactionInfo> transactionInfos) {
        int successCount = 0;
        for(TransactionInfo info : transactionInfos) {
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
                if (TransactionStatusEnum.CANCELLING.getCode() == info.getTxStatus()) {
                    try {
                        transactionMessageService.handleMessage(transInfo2Msg(info));
                        successCount++;
                    } catch (Throwable e) {
                        log.warn("Process cancel error, TransactionInfo=", info);
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
        txMsg.setTxId(txInfo.getTxId());
        txMsg.setBusinessId(txInfo.getBusinessId());
        txMsg.setTxStatus(txInfo.getTxStatus());
        return txMsg;
    }


}
