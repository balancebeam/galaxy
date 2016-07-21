package io.anyway.galaxy.proxy;
public interface Proxy {

    /**
     * 执行链式代理
     *
     * @param proxyChain 代理链
     * @return 目标方法返回值
     * @throws Throwable 异常
     */
    Object doProxy(ProxyChain proxyChain) throws Throwable;
}