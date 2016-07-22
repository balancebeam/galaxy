package io.anyway.galaxy.common;

/**
 * Created by xiongjie on 2016/7/21.
 */

public enum TransactionTypeEnum {
    TCC(0, "TCC型事务"),

    TC(1, "TC型事务"),

    TWOPC(2, "2PC型事务");

    private int    code;

    private String memo;

    /**
     * @param code
     * @param memo
     */
    private TransactionTypeEnum(int code, String memo) {
        this.code = code;
        this.memo = memo;
    }


}
