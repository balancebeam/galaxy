package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.TXContext;


/**
 * Created by yangzz on 16/7/21.
 */
public class TXContextSupport implements TXContext{

    final private long txId;

    private boolean action= false;

    private String bizSerial;

    public TXContextSupport(long txId){
        this.txId= txId;
    }

    @Override
    public long getTxId() {
        return txId;
    }

    @Override
    public boolean isAction() {
        return action;
    }

    @Override
    public void setBizSerial(String bizSerial) {
        this.bizSerial= bizSerial;
    }

    @Override
    public String getBizSerial() {
        return bizSerial;
    }

    public void setAction(boolean action){
        this.action= action;
    }

}
