package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.AbstractExecutePayload;
import org.springframework.util.StringUtils;

/**
 * Created by yangzz on 16/7/21.
 */
public class ServiceExcecutePayload extends AbstractExecutePayload {

    final private String tryMethod;

    private String confirmMethod;

    private String cancelMethod;

    public ServiceExcecutePayload(Class<?> target, String tryMethod, Class[] types, Object[] args) {
        super(target, types, args);
        this.tryMethod= tryMethod;
    }

    public String getTryMethod(){
        return tryMethod;
    }

    public String getConfirmMethod() {
        return confirmMethod;
    }

    public void setConfirmMethod(String confirmMethod) {
        this.confirmMethod = confirmMethod;
    }

    public String getCancelMethod() {
        return cancelMethod;
    }

    public void setCancelMethod(String cancelMethod) {
        this.cancelMethod = cancelMethod;
    }

    @Override
    public String toString(){
        StringBuilder builder= new StringBuilder();
        builder.append("{class=")
                .append(getTarget().getName())
                .append(",tryMethod=")
                .append(tryMethod);
        if(!StringUtils.isEmpty(confirmMethod)){
            builder.append(",confirmMethod=")
                    .append(confirmMethod);
        }
        if(!StringUtils.isEmpty(cancelMethod)){
            builder.append(",cancelMethod=")
                    .append(cancelMethod);
        }
        builder.append(",inTypes=")
                .append(getTypes())
                .append(",inArgs=")
                .append(getArgs())
                .append("}");
        return builder.toString();
    }

}
