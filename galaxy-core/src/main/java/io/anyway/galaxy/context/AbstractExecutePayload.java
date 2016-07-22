package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public abstract class AbstractExecutePayload {

    final private Object target;

    final private Class<?>[] types;

    final private Object[] args;

    public AbstractExecutePayload(Object target, Class<?>[] types, Object[] args){
        this.target= target;
        this.types= types;
        this.args= args;
    }

    public Object getTarget() {
        return target;
    }

    public Class<?>[] getTypes(){
        return types;
    }

    public Object[] getArgs() {
        return args;
    }

}
