package io.anyway.galaxy.console.dal.db;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiong.j on 2016/8/1.
 */
public class DsTypeContextHolder {

    public final static String DEFAULT_SESSION_FACTORY = "default";
    public final static String DYNAMIC_SESSION_FACTORY = "dynamic";

    private final static String DS_TYPE = "dsType";

    private final static String CONTEXT_TYPE = "contextType";

    private static final ThreadLocal<Map<Object, Object>> contextHolder = new ThreadLocal<Map<Object, Object>>();

    public static void setDsType(long dsType) {
        contextHolder.get().put(DS_TYPE, dsType);
    }

    public static Long getDsType() {
        Map<Object, Object> map = contextHolder.get();
        if (map == null) {
            init();
        }
        if (contextHolder.get().containsKey(DS_TYPE)) {
            return (Long)contextHolder.get().get(DS_TYPE);
        }
        return null;
    }

    public static void setContextType(String contextType) {
        contextHolder.get().put(CONTEXT_TYPE, contextType);
    }

    public static String getContextType() {
        Map<Object, Object> map = contextHolder.get();
        if (map == null) {
            init();
        }
        if (contextHolder.get().containsKey(CONTEXT_TYPE)) {
            return (String) contextHolder.get().get(CONTEXT_TYPE);
        }
        return null;
    }

    public static void clear() {
        contextHolder.remove();
    }

    private static void init(){
        Map<Object, Object> map = new HashMap<Object, Object>(2);
        contextHolder.set(map);
    }
}
