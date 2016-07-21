package io.anyway.galaxy.context.support;

import io.anyway.galaxy.context.AbstractExecutePayload;

/**
 * Created by yangzz on 16/7/21.
 */
public class ActionExecutePayload extends AbstractExecutePayload {

    final private String actionMethod;

    public ActionExecutePayload(Object target, String actionMethod, Class[] types, Object[] args) {
        super(target, types, args);
        this.actionMethod= actionMethod;
    }

    public String getActionMethod(){
        return actionMethod;
    }

    @Override
    public String toString(){
        StringBuilder builder= new StringBuilder();
        builder.append("{class=")
                .append(getTarget().getClass().getName())
                .append(",actionMethod=")
                .append(actionMethod)
                .append(",inTypes=")
                .append(getTypes())
                .append(",inArgs=")
                .append(getArgs())
                .append("}");
        return builder.toString();
    }
}
