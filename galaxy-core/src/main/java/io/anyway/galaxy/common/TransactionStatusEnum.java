package io.anyway.galaxy.common;

/**
 * Created by xiongjie on 2016/7/21.
 */

public enum TransactionStatusEnum {
    SUCCESS(0, "正常完成"),

    START(1, "事务开始"),

    NEED_ROLLBACK(2, "需要回滚"),

    ROLLBACK_SEND(3, "己发送回滚消息"),

    ROLLBACK(4, "已回滚");

    private int    code;

    private String memo;

    /**
     * @param code
     * @param memo
     */
    private TransactionStatusEnum(int code, String memo) {
        this.code = code;
        this.memo = memo;
    }


}
