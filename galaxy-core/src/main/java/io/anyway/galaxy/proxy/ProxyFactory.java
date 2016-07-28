package io.anyway.galaxy.proxy;

import javassist.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xiong.j on 2016/7/27.
 */
public class ProxyFactory {

    private static final ConcurrentMap<String, Class<?>> CLASSES = new ConcurrentHashMap<String, Class<?>>();

    private static final ConcurrentMap<Class<?>, Object> INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * 根据代理接口生成代理
     *
     * @param object 被代理对象
     * @param clz 代理接口
     * @param types 参数类型
     * @param <T>
     * @return 代理对象
     */
    public static <T> T getProxy(Object object, Class<?> clz, Class<?>[] types) throws Throwable {
        return getInstance(getProxyClass(object, clz, types));
    }

    private static <T> T getInstance(Class<?> clazz) throws Throwable {
        if (clazz == null) {
            return null;
        }

        T instance = (T) INSTANCES.get(clazz);
        if (instance == null) {
            INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
            instance = (T) INSTANCES.get(clazz);
        }
        return instance;
    }

    private static Class<?> getProxyClass(Object object, Class<?> clz, Class<?>[] types) throws Throwable{
        if (object == null) {
            return null;
        }

        Class<?> cls = CLASSES.get(object.getClass().getSimpleName());
        if (cls != null) {
            return cls;
        }

        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(getClassloader(object)));
        CtClass cc = pool.makeClass(object.getClass().getSimpleName() + "ProxyStub");
        cc.addInterface(pool.get(clz.getName()));
        // Method append
        CtMethod mthd;
        StringBuilder sb = new StringBuilder();
        String methodName;
        for (int i = 0; i < clz.getMethods().length; i++) {
            methodName = clz.getMethods()[i].getName();
            sb.append("public void ").append(methodName).append("(Object target, Object[] args){ ");
            sb.append("((").append(object.getClass().getName()).append(")target).").append(methodName).append("(");

            for (int j = 0; j < types.length; j++) {
                if (j == 0) {
                    sb.append("(").append(types[j].getName()).append(")").append("args[").append(j).append("]");
                } else {
                    sb.append(", (").append(types[j].getName()).append(")").append("args[").append(j).append("]");
                }
            }

            sb.append(");}");

            System.out.println(sb.toString());
            mthd = CtNewMethod.make(sb.toString(),cc);
            cc.addMethod(mthd);
            sb.setLength(0);
        }

        //生成Class
        cls = cc.toClass();
        CLASSES.putIfAbsent(object.getClass().getSimpleName(), cls);

        return cls;
    }

    private static ClassLoader getClassloader(Object object) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = object.getClass().getClassLoader();
        return classLoader;
    }

}
