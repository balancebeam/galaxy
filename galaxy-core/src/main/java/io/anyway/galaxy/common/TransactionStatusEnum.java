package io.anyway.galaxy.common;

/**
 * Created by xiongjie on 2016/7/21.
 */

public enum TransactionStatusEnum {
    //SUCCESS(0, "正常完成"),

    BEGIN(1, "事务开始"),

    TRIED(2, "尝试完成"),

    CANCELLING(3, "回滚中"),

    CANCELLED(4, "回滚完成"),

    CONFIRMING(5, "确认中"),

    CONFIRMED(6, "确认完成");

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
