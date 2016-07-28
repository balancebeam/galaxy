package io.anyway.galaxy.util;

import io.anyway.galaxy.proxy.ProxyFactory;
import io.anyway.galaxy.proxy.TXOperationProxy;

import java.lang.reflect.Method;

/**
 * Created by xiong.j on 2016/7/25.
 */
public class ProxyUtil {

    /**
     * 动态代理方法调用
     *
     * @param target 目标对象
     * @param methodName 方法名
     * @param types 参数类型
     * @param args 参数值
     * @throws Throwable
     */
    public static void proxyMethod(Object target, String methodName, Class<?>[] types, Object[] args) throws Throwable {
        Method method = target.getClass().getMethod(methodName, types);
        method.invoke(target, args);
    }

    /**
     * 静态代理方法调用TXOperationProxy.invokeCancel
     *
     * @param target 目标对象
     * @param types 参数类型
     * @param args 参数值
     * @throws Throwable
     */
    public static void invokeCancel(Object target, Class<?>[] types, Object[] args) throws Throwable {
        TXOperationProxy txOperationProxy = ProxyFactory.getProxy(target, TXOperationProxy.class, types);
        txOperationProxy.invokeCancel(target, args);
    }

    /**
     * 静态代理方法调用TXOperationProxy.invokeConfirm
     *
     * @param target 目标对象
     * @param types 参数类型
     * @param args 参数值
     * @throws Throwable
     */
    public static void invokeConfirm(Object target, Class<?>[] types, Object[] args) throws Throwable {
        TXOperationProxy txOperationProxy = ProxyFactory.getProxy(target, TXOperationProxy.class, types);
        txOperationProxy.invokeConfirm(target, args);
    }
}
