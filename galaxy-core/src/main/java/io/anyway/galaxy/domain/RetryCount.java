package io.anyway.galaxy.domain;

import com.alibaba.fastjson.JSON;
import io.anyway.galaxy.common.TransactionStatusEnum;
import lombok.*;

/**
 * Created by xiong.j on 2016/8/11.
 */
@Setter
@Getter
@ToString
@RequiredArgsConstructor
public class RetryCount {

    // default tried retry count
    private final int dMsg;

    // current tried retry count
    private int msg;

    // default cancel retry count
    private final int dCancel;

    // current cancel retry count
    private int cancel;

    // default confirm retry count
    private final int dConfirm;

    // current cancel retry count
    private int confirm;

    /**
     * 创建一个实例
     * @param dMsg
     * @param dCancel
     * @param dConfirm
     * @return
     */
    public static RetryCount CreateRetryCount(int dMsg, int dCancel, int dConfirm) {
        return new RetryCount(dMsg, dCancel, dConfirm);
    }

    /**
     * 根据事务状态获取初始设定的重试次数
     * @param retryCount
     * @param txStatus
     * @return
     */
    public int getDefaultRetryTimes(RetryCount retryCount, int txStatus) {

        if (txStatus == TransactionStatusEnum.BEGIN.getCode()) {
            return retryCount.getDMsg();
        } else if (txStatus == TransactionStatusEnum.CANCELLING.getCode()) {
            return retryCount.getDCancel();
        } else if (txStatus == TransactionStatusEnum.CONFIRMING.getCode()) {
            return retryCount.getDConfirm();
        }
        return 0;
    }

    /**
     * 根据事务状态获取当前的重试次数
     * @param retryCount
     * @param txStatus
     * @return
     */
    public int getCurrentRetryTimes(RetryCount retryCount, int txStatus) {

        if (txStatus == TransactionStatusEnum.BEGIN.getCode()) {
            return retryCount.getMsg();
        } else if (txStatus == TransactionStatusEnum.CANCELLING.getCode()) {
            return retryCount.getCancel();
        } else if (txStatus == TransactionStatusEnum.CONFIRMING.getCode()) {
            return retryCount.getConfirm();
        }
        return 0;
    }

    /**
     * 根据事务状态获取下一次的重试次数
     * @param retryCount
     * @param txStatus
     * @return
     */
    public String getNextRetryTimes(RetryCount retryCount, int txStatus) {

        if (txStatus == TransactionStatusEnum.BEGIN.getCode()) {
            retryCount.setMsg(retryCount.getMsg() - 1);
        } else if (txStatus == TransactionStatusEnum.CANCELLING.getCode()) {
            retryCount.setMsg(retryCount.getCancel() - 1);
        } else if (txStatus == TransactionStatusEnum.CONFIRMING.getCode()) {
            retryCount.setMsg(retryCount.getConfirm() - 1);
        }
        return JSON.toJSONString(retryCount);
    }

    public static void main(String args[]){
        RetryCount r = new RetryCount(999, 999, 999);
        r.setCancel(999);
        r.setConfirm(999);
        r.setMsg(999);
        System.out.println(JSON.toJSONString(r));
    }
}
