package io.anyway.galaxy.console.dal.db;

/**
 * Created by xiong.j on 2016/8/1.
 */
public class DsTypeContextHolder {

    public final static String SESSION_FACTORY_DEFAULT = "default";
    public final static String SESSION_FACTORY_DYNAMIC = "dynamic";

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();

    public static void setDsType(String dsType) {
        contextHolder.set(dsType);
    }

    public static String getDsType() {
        return contextHolder.get();
    }

    public static void setContextType(String contextType) {
        contextHolder.set(contextType);
    }

    public static String getContextType() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }

}
