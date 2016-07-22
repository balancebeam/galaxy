package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public abstract class AbstractExecutePayload {

    final private Class<?> target;

    final private Class<?>[] types;

    final private Object[] args;

    public AbstractExecutePayload(Class target, Class<?>[] types, Object[] args){
        this.target= target;
        this.types= types;
        this.args= args;
    }

    public Class getTarget() {
        return target;
    }

    public Class<?>[] getTypes(){
        return types;
    }

    public Object[] getArgs() {
        return args;
    }

}
