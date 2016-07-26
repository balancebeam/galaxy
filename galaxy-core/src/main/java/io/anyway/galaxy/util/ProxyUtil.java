package io.anyway.galaxy.util;

import java.lang.reflect.Method;

/**
 * Created by xiong.j on 2016/7/25.
 */
public class ProxyUtil {

    // TODO 改成静态代理
    public static void proxyMethod(Object objectClass, String methodName, Class<?>[] types, Object[] args) throws Exception {
        Method method = objectClass.getClass().getMethod(methodName, types);
        method.invoke(objectClass, args);
    }
}
