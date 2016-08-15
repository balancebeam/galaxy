package io.anyway.galaxy.common;

import io.anyway.galaxy.domain.RetryCount;

/**
 * Created by xiong.j on 2016/8/11.
 */
public enum RetryEnum {

    TCC(0, "TCC型事务"),

    TC(1, "TC型事务"),

    TWOPC(2, "2PC型事务");

    private int    code;

    private String memo;

    /**
     * @param code
     * @param memo
     */
    private RetryEnum(int code, String memo) {
        this.code = code;
        this.memo = memo;
    }

    public int getCode() {
        return code;
    }

    public String getMemo() {
        return memo;
    }

    public static String getMemo(int code){

        for(RetryEnum type: RetryEnum.values()){
            if(type.code == code){
                return type.memo;
            }
        }
        return "unknow";
    }
}
