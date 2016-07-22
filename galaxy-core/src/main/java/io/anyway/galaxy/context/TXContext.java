package io.anyway.galaxy.context;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/21.
 */
public interface TXContext extends Serializable{

    long getTxId();
}

