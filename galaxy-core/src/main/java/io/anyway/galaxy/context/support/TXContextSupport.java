package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.TXContext;


/**
 * Created by yangzz on 16/7/21.
 */
public class TXContextSupport implements TXContext{

    private long parentId;

    private long txId;

    private String serialNumber;

    private String businessType;

    public TXContextSupport(){}

    public TXContextSupport(long parentId, String serialNumber){
        this.parentId = parentId;
        this.serialNumber = serialNumber;
    }

    public TXContextSupport(long parentId, long txId,String serialNumber){
        this.parentId = parentId;
        this.txId= txId;
        this.serialNumber = serialNumber;
    }

    public TXContextSupport(long parentId, long txId,String serialNumber, String businessType){
        this.parentId = parentId;
        this.txId= txId;
        this.serialNumber = serialNumber;
        this.businessType = businessType;
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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    @Override
    public String toString() {
        return "TXContextSupport{" +
                "parentId=" + parentId +
                ", txId=" + txId +
                ", serialNumber='" + serialNumber + '\'' +
                ", businessType='" + businessType + '\'' +
                '}';
    }
}
