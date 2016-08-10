package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.TXContext;


/**
 * Created by yangzz on 16/7/21.
 */
public class TXContextSupport implements TXContext{

    private long parentId;

    private long txId;

    private String serialNumber;

    public TXContextSupport(){}

    public TXContextSupport(long txId,String serialNumber){
        this.txId= txId;
        this.serialNumber = serialNumber;
    }

    @Override
    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    @Override
    public long getTxId() {
        return txId;
    }

    public void setTxId(long txId){
        this.txId= txId;
    }

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber){
        this.serialNumber = serialNumber;
    }

    @Override
    public String toString() {
        return "TXContext{" +
                "parentId=" + parentId +
                ", txId=" + txId +
                ", serialNumber='" + serialNumber + '\'' +
                '}';
    }
}
