package io.anyway.galaxy.message;

import java.io.Serializable;
import java.sql.Date;

/**
 * Created by xiong.j on 2016/7/25.
 */

public class TransactionMessage implements Serializable{

    private long txId;

    private String serialNumber;

    private int txStatus = -1;

    private Date date= new Date(new java.util.Date().getTime());

    public long getTxId() {
        return txId;
    }

    public void setTxId(long txId) {
        this.txId = txId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getTxStatus() {
        return txStatus;
    }

    public void setTxStatus(int txStatus) {
        this.txStatus = txStatus;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
