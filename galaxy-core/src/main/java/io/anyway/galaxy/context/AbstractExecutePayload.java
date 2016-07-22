package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public abstract class AbstractExecutePayload implements Cloneable{

    protected String bizType;

    protected Class<?> target;

    protected Class<?>[] types;

    protected Object[] args;

    public AbstractExecutePayload(){}

    public AbstractExecutePayload(String bizType, Class<?> target, Class<?>[] types){
        this.bizType= bizType;
        this.target= target;
        this.types= types;
    }

    public String getBizType(){
        return bizType;
    }

    public Class<?> getTarget() {
        return target;
    }

    public Class<?>[] getTypes(){
        return types;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public void setTarget(Class<?> target) {
        this.target = target;
    }

    public void setTypes(Class<?>[] types) {
        this.types = types;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
