package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public abstract class AbstractExecutePayload implements Cloneable{

    protected String bizType;

    protected Class<?> target;

    protected Class<?>[] types;

    protected Object[] args;

    protected String moduleId;

    public AbstractExecutePayload(){}

    public AbstractExecutePayload(String bizType,String moduleId, Class<?> target, Class<?>[] types){
        this.bizType= bizType;
        this.moduleId= moduleId;
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

    public void setModuleId(String moduleId){
        this.moduleId= moduleId;
    }

    public String getModuleId(){
        return moduleId;
    }
}
