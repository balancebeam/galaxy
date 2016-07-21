package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.TXContext;


/**
 * Created by yangzz on 16/7/21.
 */
public class TXContextSupport implements TXContext{

    final private String txid;

    public TXContextSupport(String txid){
        this.txid= txid;
    }

    @Override
    public String getTXid() {
        return txid;
    }

}
