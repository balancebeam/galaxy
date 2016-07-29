package io.anyway.galaxy.recovery;

import java.sql.Connection;
import java.sql.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.message.TransactionMessageService;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import io.anyway.galaxy.util.DateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xiong.j on 2016/7/25.
 */
@Slf4j
public class RecoveryServiceImpl implements RecoveryService{

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMessageService transactionMessageService;

    @Override
    public void execute(List<Integer> shardingItem) {
        Connection conn = null;
        try {
            conn = dataSourceAdaptor.getDataSource().getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 30天前
        Date searchDate = DateUtil.getPrevDate(30);

        for(Integer status : shardingItem) {
            List<TransactionInfo> transactionInfos = transactionRepository.findSince(conn, searchDate, status);
            internalExecute(transactionInfos);
        }
    }

    private void internalExecute(List<TransactionInfo> transactionInfos) {
        for(TransactionInfo info : transactionInfos) {
            if(TransactionStatusEnum.BEGIN.getCode() == info.getTxStatus()){
                // TODO BEGIN状态需要回查是否Try成功，后续优化
                try {
                	TXContext ctx= new TXContextSupport(info.getTxId(),info.getSerialNumber());
                    transactionMessageService.sendMessage(ctx, TransactionStatusEnum.CANCELLING);
                } catch (Throwable throwable) {
                    log.warn("Send cancel message error, TransactionInfo=", info);
                }
            } else {
                if (TransactionStatusEnum.CANCELLING.getCode() == info.getTxStatus()) {
                    try {
                        transactionMessageService.handleMessage(transInfo2Msg(info));
                    } catch (Throwable e) {
                        log.warn("Process cancel error, TransactionInfo=", info);
                    }
                }

                if (TransactionStatusEnum.CONFIRMING.getCode() == info.getTxStatus()) {
                    try {
                        transactionMessageService.handleMessage(transInfo2Msg(info));
                    } catch (Throwable e) {
                        log.warn("Process confirm error, TransactionInfo=", info);
                    }
                }
            }
        }
    }

    private TransactionMessage transInfo2Msg(TransactionInfo txInfo){
        TransactionMessage txMsg = new TransactionMessage();
        txMsg.setTxId(txInfo.getTxId());
        txMsg.setTxStatus(txInfo.getTxStatus());
        return txMsg;
    }
}
