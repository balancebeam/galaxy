package io.anyway.galaxy.message;

import java.io.Serializable;
import java.sql.Date;

/**
 * Created by xiong.j on 2016/7/25.
 */

public class TransactionMessage implements Serializable{

    private long txId;

    private long businessId;

    private String businessType;

    private int txStatus = -1;

    private int txType = -1;

    private Date date= new Date(new java.util.Date().getTime());

    public long getTxId() {
        return txId;
    }

    public void setTxId(long txId) {
        this.txId = txId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(long businessId) {
        this.businessId = businessId;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
