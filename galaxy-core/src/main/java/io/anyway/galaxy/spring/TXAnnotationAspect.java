package io.anyway.galaxy.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.annotation.TXTry;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.AbstractExecutePayload;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.context.support.ServiceExcecutePayload;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.intercepter.ServiceIntercepter;
import io.anyway.galaxy.jetty.TransactionServer;

/**
 * Created by yangzz on 16/7/20.
 */
@Component
@Aspect
//TODO 合併 "@Transactional"
public class TXAnnotationAspect implements Ordered,ResourceLoaderAware{

    private Log logger= LogFactory.getLog(TXAnnotationAspect.class);

    private ResourceLoader resourceLoader;

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private ActionIntercepter actionIntercepter;

    @Autowired
    private ServiceIntercepter serviceIntercepter;

    private Cache<Method, AbstractExecutePayload> cache= CacheBuilder.newBuilder().expireAfterAccess(300, TimeUnit.SECONDS).maximumSize(1000).build();

    private TransactionServer transactionServer;
    
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
    
    @PostConstruct
    public void init(){
    	this.transactionServer = TransactionServer.instance();
    	this.transactionServer.setDataSource(dataSourceAdaptor);
    	this.transactionServer.start();
    }
    
    @PreDestroy
    public void destroy(){
    	this.transactionServer.shutdown();
    }

    //切面注解TXAction
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXAction)")
    public void pointcutTXAction(){}

    @Around("pointcutTXAction()")
    public Object processTXAction(ProceedingJoinPoint pjp) throws Throwable {
        //需要判断最外层是否开启了写事务
        assertTransactional();

        //获取方法上的注解内容
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method= signature.getMethod();

        ActionExecutePayload cachedPayload = (ActionExecutePayload)cache.getIfPresent(method);
        if(cachedPayload==null){
            synchronized (method) {
                if(cachedPayload==null){
                    TXAction action = method.getAnnotation(TXAction.class);
                    Class<?> target = pjp.getTarget().getClass();
                    String bizType = action.bizType();
                    if (StringUtils.isEmpty(bizType)) {
                        logger.warn("miss business type, class=" + target + ",method=" + method);
                    }
                    cachedPayload = new ActionExecutePayload(bizType, target, method.getName(), method.getParameterTypes());
                    cachedPayload.setTimeout(action.timeout());
                    cachedPayload.setTxType(action.value());
                    cache.put(method, cachedPayload);
                }
            }
            cachedPayload= (ActionExecutePayload)cache.getIfPresent(method);
        }
        final ActionExecutePayload payload= cachedPayload.clone();
        //设置运行时的入参
        payload.setArgs(pjp.getArgs());

        try {
            //获取外出业务开启事务的对应的数据库连接
            final Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
            //获取新的连接开启新事务新增一条TransactionAction记录
            final long txId = actionIntercepter.addAction(payload);

            TXContextSupport ctx= new TXContextSupport(txId);
            ctx.setAction(true);
            TXContextHolder.setTXContext(ctx);

            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSourceAdaptor.getDataSource());
            method= ReflectionUtils.findMethod(ConnectionHolder.class,"setConnection",Connection.class);
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method,conHolder,
                Proxy.newProxyInstance(resourceLoader.getClassLoader(),
                        //重载Connection复写commit和rollback方法
                        new Class<?>[]{Connection.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            return method.invoke(conn,args);
                        }finally {
                            //确保在commit执行之后执行通知方法
                            if ("commit".equals(method.getName())) {
                                //如果是TCC类型事务才发送confirm消息
                                if(payload.getTxType()== TransactionTypeEnum.TCC){
                                    if (logger.isInfoEnabled()) {
                                        logger.info("will send \"confirm\" message, txId=" + txId+", payload="+payload);
                                    }
                                    actionIntercepter.confirmAction(txId);
                                }
                                //确保在cancel之后执行通知方法
                            } else if ("rollback".equals(method.getName())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("will send \"cancel\" message, txId=" + txId+", payload="+payload);
                                }
                                actionIntercepter.cancelAction(txId);
                            }
                        }
                    }
                }));

            //先执行业务操作
            Object result= pjp.proceed();
            //更新TX表状态为成功态,在根据TX类型确定是否要发送Confirm消息
            if(StringUtils.isEmpty(TXContextHolder.getTXContext().getBizSerial())){
                logger.warn("miss business serial, txId="+txId+",payload="+payload);
            }
            actionIntercepter.tryAction(conn, txId);
            return result;

        }finally{
            TXContextHolder.setTXContext(null);
        }
    }

    //切面注解TXTry
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXTry)")
    public void pointcutTXTry(){ }

    @Around("pointcutTXTry()")
    public Object processTXTry(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.getTXContext().isAction()){
            return pjp.proceed();
        }
        long txId= TXContextHolder.getTXContext().getTxId();

        //获取方法上的注解内容
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method= signature.getMethod();

        ServiceExcecutePayload cachedPayload = (ServiceExcecutePayload)cache.getIfPresent(method);
        if(cachedPayload==null){
            synchronized (method) {
                if(cachedPayload==null){
                    TXTry txTry= method.getAnnotation(TXTry.class);
                    Class<?> target = pjp.getTarget().getClass();
                    String bizType = txTry.bizType();
                    if (StringUtils.isEmpty(bizType)) {
                        logger.warn("miss business type, class=" + target + ",method=" + method);
                    }
                    cachedPayload = new ServiceExcecutePayload(bizType, target, method.getName(), method.getParameterTypes());
                    cachedPayload.setConfirmMethod(txTry.confirm());
                    cachedPayload.setCancelMethod(txTry.cancel());
                    cache.put(method, cachedPayload);
                }
            }
            cachedPayload= (ServiceExcecutePayload)cache.getIfPresent(method);
        }
        ServiceExcecutePayload payload= cachedPayload.clone();
        //设置运行时的入参
        payload.setArgs(pjp.getArgs());

        //先调用业务方法
        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        serviceIntercepter.tryService(conn,payload,txId);
        return result;
    }

    //切面注解TXConfirm
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXConfirm)")
    public void pointcutTXConfirm(){}

    @Around("pointcutTXConfirm()")
    public Object processTXConfirm(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.getTXContext().isAction()){
            return pjp.proceed();
        }
        long txId= TXContextHolder.getTXContext().getTxId();

        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.confirmService(conn,txId);
        return result;
    }

    //切面注解TXCancel
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXCancel)")
    public void pointcutTXCancel(){}

    @Around("pointcutTXCancel()")
    public Object processTXCancel(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.getTXContext().isAction()){
            return pjp.proceed();
        }
        long txId= TXContextHolder.getTXContext().getTxId();

        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.cancelService(conn,txId);
        return result;
    }

    private void assertTransactional()throws Throwable{
        DataSource dataSource= dataSourceAdaptor.getDataSource();
        Assert.notNull(dataSource,"datasource can not empty");
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder == null || conHolder.getConnectionHandle()==null || !conHolder.isSynchronizedWithTransaction()) {
            throw new DistributedTransactionException("transaction connection is null");
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader= resourceLoader;
    }
}

