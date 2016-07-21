package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.TXContext;


/**
 * Created by yangzz on 16/7/21.
 */
public class TXContextSupport implements TXContext{

    final private long txId;

    public TXContextSupport(long txId){
        this.txId= txId;
    }

    @Override
    public long getTxId() {
        return txId;
    }

}
