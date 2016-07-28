package io.anyway.galaxy.domain;

import java.sql.Date;
import java.text.SimpleDateFormat;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;

/**
 * Created by xiongjie on 2016/7/21.
 */
public class TransactionInfo {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private long txId;

    private long parentId;

    private String serialNumber;

    private String businessType;

    private int txStatus = -1;

    private int txType = -1;

    private String context;

    private String payload;

    private int retried_count = -1;

    private Date gmtCreated;

    private Date gmtModified;

    public long getTxId() {
        return txId;
    }

    public void setTxId(long txId) {
        this.txId = txId;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public int getTxStatus() {
        return txStatus;
    }

    public void setTxStatus(int txStatus) {
        this.txStatus = txStatus;
    }

    public int getTxType() {
        return txType;
    }

    public void setTxType(int txType) {
        this.txType = txType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getRetried_count() {
        return retried_count;
    }

    public void setRetried_count(int retried_count) {
        this.retried_count = retried_count;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getStrGmtCreated(){
    	return sdf.format(gmtCreated);
    }
    
    public String getStrGmtModified(){
    	return sdf.format(gmtModified);
    }
    
    public String getStrTXType(){
    	return TransactionTypeEnum.getMemo(txType);
    }
    
    public String getStrTXStatus(){
    	return TransactionStatusEnum.getMemo(txStatus);
    }
    
}
